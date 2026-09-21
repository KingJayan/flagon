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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Addresses {

    public static final long NULL = 0L;

    private static final int SLOT_BITS = 24;
    private static final long SLOT_MASK = (1L << SLOT_BITS) - 1;
    private static final int GENERATIONS = 1 << 8;

    private static final ReferenceQueue<Buffer> COLLECTED = new ReferenceQueue<>();
    private static final List<Held> SLOTS = new ArrayList<>();
    private static final List<Integer> AGES = new ArrayList<>();
    private static final List<Integer> FREE = new ArrayList<>();
    private static final Map<Integer, List<Held>> BY_IDENTITY = new HashMap<>();
    private static final List<Held> REGISTERED = new ArrayList<>();

    private static int sequence;

    /**
     * one buffer's address. the collector may take the buffer at any time, which frees the slot
     * and leaves every address on it unknown.
     */
    private static final class Held extends WeakReference<Buffer> {

        private final int identity;
        private final int sequence;
        private final int slot;

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

    /** @return the address of the buffer's current position */
    public static long of(Buffer buffer) {
        return buffer == null ? NULL : base(buffer)
                + (long) buffer.position() * elementSize(buffer);
    }

    /** @return the address of {@code index} elements into the buffer, counted from zero */
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
        sweep();
        int identity = System.identityHashCode(buffer);
        Held known = find(identity, buffer);
        if (known != null) {
            return known.handle;
        }
        int slot = takeSlot();
        Held held = new Held(buffer, identity, sequence++, slot);
        held.handle = ((long) AGES.get(slot) << 56) | ((long) (slot + 1) << 32);
        SLOTS.set(slot, held);
        add(held);
        return held.handle;
    }

    private static int takeSlot() {
        if (!FREE.isEmpty()) {
            return FREE.remove(FREE.size() - 1);
        }
        if (SLOTS.size() >= SLOT_MASK) {
            throw new IllegalStateException("this build hands out at most " + SLOT_MASK
                    + " addresses at a time, and every one of them is still in use");
        }
        SLOTS.add(null);
        AGES.add(0);
        return SLOTS.size() - 1;
    }

    private static Held find(int identity, Buffer buffer) {
        List<Held> bucket = BY_IDENTITY.get(identity);
        if (bucket != null) {
            for (Held held : bucket) {
                if (held.get() == buffer) {
                    return held;
                }
            }
        }
        return null;
    }

    private static void add(Held held) {
        BY_IDENTITY.computeIfAbsent(held.identity, key -> new ArrayList<>()).add(held);
        REGISTERED.add(held);
    }

    private static void sweep() {
        for (Reference<? extends Buffer> dead = COLLECTED.poll(); dead != null;
                dead = COLLECTED.poll()) {
            if (dead instanceof Held held) {
                REGISTERED.remove(held);
                drop(held);
            }
        }
    }

    private static void drop(Held held) {
        List<Held> bucket = BY_IDENTITY.get(held.identity);
        if (bucket != null) {
            bucket.remove(held);
            if (bucket.isEmpty()) {
                BY_IDENTITY.remove(held.identity);
            }
        }
        if (held.slot >= 0 && SLOTS.get(held.slot) == held) {
            SLOTS.set(held.slot, null);
            AGES.set(held.slot, (AGES.get(held.slot) + 1) % GENERATIONS);
            FREE.add(held.slot);
        }
    }

    /**
     * @param capacity how many bytes the caller expects to reach
     * @return the bytes the address names
     */
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
        ByteBuffer view = slot.duplicate();
        view.position(from);
        view.limit(to);
        ByteBuffer sliced = view.slice();
        sliced.order(slot.order());
        return sliced;
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
        for (int at = REGISTERED.size() - 1; at >= 0; at--) {
            Held held = REGISTERED.get(at);
            if (held.sequence < mark) {
                break;
            }
            REGISTERED.remove(at);
            drop(held);
        }
        sequence = Math.min(sequence, mark);
    }

    public static boolean isKnown(long address) {
        return held(address) != null;
    }

    private static Held held(long address) {
        int index = (int) ((address >>> 32) & SLOT_MASK) - 1;
        if (index < 0 || index >= SLOTS.size()) {
            return null;
        }
        Held held = SLOTS.get(index);
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
