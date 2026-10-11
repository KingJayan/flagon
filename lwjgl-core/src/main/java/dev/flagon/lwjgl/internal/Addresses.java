package dev.flagon.lwjgl.internal;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Addresses {

    public static final long NULL = 0L;

    private static final int SLOT_BITS = 24;
    private static final long SLOT_MASK = (1L << SLOT_BITS) - 1;
    private static final int GENERATIONS = 1 << 8;

    private static final int MEMO = 8;

    private static final int SWEEP_EVERY = 64;

    private static final ReferenceQueue<Buffer> COLLECTED = new ReferenceQueue<>();
    private static final List<Held> REGISTERED = new ArrayList<>();
    private static final Held[] MEMOED = new Held[MEMO];

    private static Held[] slots = new Held[64];
    private static int[] ages = new int[64];
    private static int slotCount;

    private static int[] free = new int[64];
    private static int freeCount;

    private static Held[] table = new Held[64];
    private static int tableCount;

    private static int missed;
    private static int gone;
    private static int sequence;

    private static final class Held extends WeakReference<Buffer> {

        private final int identity;
        private final int sequence;
        private final int slot;

        private Held sameIdentity;
        private boolean dropped;
        private long handle;

        Held(Buffer buffer, int identity, int sequence, int slot) {
            super(buffer, COLLECTED);
            this.identity = identity;
            this.sequence = sequence;
            this.slot = slot;
        }
    }

    private Addresses() {
    }

    public static long of(Buffer buffer) {
        return buffer == null ? NULL : base(buffer)
                + (long) buffer.position() * elementSize(buffer);
    }

    public static long of(Buffer buffer, int index) {
        return buffer == null ? NULL : base(buffer) + (long) index * elementSize(buffer);
    }

    public static <B extends Buffer> B derive(B view, ByteBuffer root, int byteOffset) {
        long handle = base(root) + byteOffset;
        int identity = System.identityHashCode(view);
        Held known = find(identity, view);
        if (known == null) {
            Held held = new Held(view, identity, sequence++, -1);
            held.handle = handle;
            add(held);
        } else {
            known.handle = handle;
        }
        return view;
    }

    private static long base(Buffer buffer) {
        int identity = System.identityHashCode(buffer);
        int seat = memoSeat(identity);
        Held remembered = MEMOED[seat];
        if (remembered != null && remembered.get() == buffer) {
            return remembered.handle;
        }
        if (++missed >= SWEEP_EVERY) {
            missed = 0;
            sweep();
        }
        Held known = find(identity, buffer);
        if (known == null) {
            int slot = takeSlot();
            known = new Held(buffer, identity, sequence++, slot);
            known.handle = ((long) ages[slot] << 56) | ((long) (slot + 1) << 32);
            slots[slot] = known;
            add(known);
        }
        MEMOED[seat] = known;
        return known.handle;
    }

    private static int memoSeat(int identity) {
        return spread(identity) & (MEMO - 1);
    }

    private static int takeSlot() {
        if (freeCount > 0) {
            return free[--freeCount];
        }
        if (slotCount >= SLOT_MASK) {
            sweep();
        }
        if (freeCount > 0) {
            return free[--freeCount];
        }
        if (slotCount >= SLOT_MASK) {
            throw new IllegalStateException("this build hands out at most " + SLOT_MASK
                    + " addresses at a time, and every one of them is still in use");
        }
        if (slotCount == slots.length) {
            slots = Arrays.copyOf(slots, slots.length * 2);
            ages = Arrays.copyOf(ages, ages.length * 2);
        }
        return slotCount++;
    }

    private static int spread(int identity) {
        int mixed = identity * 0x9E3779B9;
        return mixed ^ (mixed >>> 16);
    }

    private static int start(int identity) {
        return spread(identity) & (table.length - 1);
    }

    private static int after(int at) {
        return (at + 1) & (table.length - 1);
    }

    private static int seat(int identity) {
        int at = start(identity);
        while (table[at] != null && table[at].identity != identity) {
            at = after(at);
        }
        return at;
    }

    private static Held find(int identity, Buffer buffer) {
        for (Held held = table[seat(identity)]; held != null; held = held.sameIdentity) {
            if (held.get() == buffer) {
                return held;
            }
        }
        return null;
    }

    private static void add(Held held) {
        int at = seat(held.identity);
        if (table[at] == null) {
            table[at] = held;
            tableCount++;
            if (tableCount * 4 >= table.length * 3) {
                widen();
            }
        } else {
            held.sameIdentity = table[at].sameIdentity;
            table[at].sameIdentity = held;
        }
        REGISTERED.add(held);
    }

    private static void widen() {
        Held[] was = table;
        table = new Held[was.length * 2];
        for (Held head : was) {
            if (head != null) {
                table[seat(head.identity)] = head;
            }
        }
    }

    private static void unmap(Held held) {
        int at = seat(held.identity);
        Held head = table[at];
        if (head == null) {
            return;
        }
        if (head != held) {
            for (Held before = head; before.sameIdentity != null; before = before.sameIdentity) {
                if (before.sameIdentity == held) {
                    before.sameIdentity = held.sameIdentity;
                    break;
                }
            }
        } else if (head.sameIdentity != null) {
            table[at] = head.sameIdentity;
        } else {
            table[at] = null;
            tableCount--;
            close(at);
        }
        held.sameIdentity = null;
    }

    private static void close(int hole) {
        for (int at = after(hole); table[at] != null; at = after(at)) {
            int wanted = start(table[at].identity);
            if (((at - wanted) & (table.length - 1)) >= ((at - hole) & (table.length - 1))) {
                table[hole] = table[at];
                table[at] = null;
                hole = at;
            }
        }
    }

    private static void sweep() {
        for (Reference<? extends Buffer> dead = COLLECTED.poll(); dead != null;
                dead = COLLECTED.poll()) {
            if (dead instanceof Held held && !held.dropped) {
                drop(held);
                gone++;
            }
        }
        if (gone * 2 > REGISTERED.size()) {
            compact();
        }
    }

    private static void compact() {
        int out = 0;
        for (int at = 0; at < REGISTERED.size(); at++) {
            Held held = REGISTERED.get(at);
            if (!held.dropped) {
                REGISTERED.set(out++, held);
            }
        }
        REGISTERED.subList(out, REGISTERED.size()).clear();
        gone = 0;
    }

    private static void drop(Held held) {
        held.dropped = true;
        int seat = memoSeat(held.identity);
        if (MEMOED[seat] == held) {
            MEMOED[seat] = null;
        }
        unmap(held);
        if (held.slot >= 0 && slots[held.slot] == held) {
            slots[held.slot] = null;
            ages[held.slot] = (ages[held.slot] + 1) % GENERATIONS;
            if (freeCount == free.length) {
                free = Arrays.copyOf(free, free.length * 2);
            }
            free[freeCount++] = held.slot;
        }
    }

    public static ByteBuffer bytes(long address, int capacity) {
        ByteBuffer slot = byteSlot(address);
        int offset = (int) address;
        if (offset + capacity > slot.capacity()) {
            throw new IllegalArgumentException("address " + address + " has only "
                    + (slot.capacity() - offset) + " bytes left, short of the " + capacity
                    + " asked for");
        }
        return window(slot, offset, offset + capacity);
    }

    public static ByteBuffer bytesToEnd(long address) {
        ByteBuffer slot = byteSlot(address);
        return window(slot, (int) address, slot.capacity());
    }

    private static ByteBuffer window(ByteBuffer slot, int from, int to) {
        int position = slot.position();
        int limit = slot.limit();
        slot.limit(slot.capacity());
        slot.position(from);
        slot.limit(to);
        ByteBuffer view = slot.slice();
        view.order(slot.order());
        slot.limit(slot.capacity());
        slot.position(position);
        slot.limit(limit);
        return view;
    }

    private static ByteBuffer byteSlot(long address) {
        Buffer slot = slot(address);
        if (!(slot instanceof ByteBuffer bytes)) {
            throw new IllegalArgumentException("address " + address + " names a "
                    + slot.getClass().getSimpleName() + ", so it has no bytes to read");
        }
        return bytes;
    }

    public static int mark() {
        sweep();
        return sequence;
    }

    public static void reset(int mark) {
        sweep();
        while (!REGISTERED.isEmpty()) {
            Held held = REGISTERED.get(REGISTERED.size() - 1);
            if (held.sequence < mark) {
                break;
            }
            REGISTERED.remove(REGISTERED.size() - 1);
            if (held.dropped) {
                gone--;
            } else {
                drop(held);
            }
        }
        sequence = Math.min(sequence, mark);
    }

    public static boolean isKnown(long address) {
        return held(address) != null;
    }

    private static Held held(long address) {
        int index = (int) ((address >>> 32) & SLOT_MASK) - 1;
        if (index < 0 || index >= slotCount) {
            return null;
        }
        Held held = slots[index];
        if (held == null || held.get() == null) {
            return null;
        }
        return held.handle >>> 56 == address >>> 56 ? held : null;
    }

    private static Buffer slot(long address) {
        if (address == NULL) {
            throw new NullPointerException("the null address names no buffer");
        }
        Held held = held(address);
        if (held == null) {
            throw new IllegalArgumentException("address " + address + " was never handed out by "
                    + "this build, or the buffer behind it is gone; only a live buffer's own "
                    + "address can be read back");
        }
        return held.get();
    }

    private static int elementSize(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 1;
        } else if (buffer instanceof ShortBuffer) {
            return 2;
        } else if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) {
            return 4;
        } else if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) {
            return 8;
        }
        return 2;
    }
}
