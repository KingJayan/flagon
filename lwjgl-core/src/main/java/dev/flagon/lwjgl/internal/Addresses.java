package dev.flagon.lwjgl.internal;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Addresses {

    public static final long NULL = 0L;

    private static final List<Buffer> SLOTS = new ArrayList<>();
    private static final List<Buffer> REGISTERED = new ArrayList<>();
    private static final List<Integer> SLOTS_THEN = new ArrayList<>();
    private static final Map<Buffer, Long> HANDLES = new IdentityHashMap<>();

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
        if (HANDLES.put(view, handle) == null) {
            register(view);
        }
        return view;
    }

    private static long base(Buffer buffer) {
        Long known = HANDLES.get(buffer);
        if (known != null) {
            return known;
        }
        SLOTS.add(buffer);
        long handle = (long) SLOTS.size() << 32;
        HANDLES.put(buffer, handle);
        register(buffer);
        return handle;
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

    private static void register(Buffer buffer) {
        REGISTERED.add(buffer);
        SLOTS_THEN.add(SLOTS.size());
    }

    public static int mark() {
        return REGISTERED.size();
    }

    public static void reset(int mark) {
        int slots = mark == 0 ? 0 : SLOTS_THEN.get(mark - 1);
        while (REGISTERED.size() > mark) {
            int last = REGISTERED.size() - 1;
            HANDLES.remove(REGISTERED.remove(last));
            SLOTS_THEN.remove(last);
        }
        while (SLOTS.size() > slots) {
            SLOTS.remove(SLOTS.size() - 1);
        }
    }

    public static boolean isKnown(long address) {
        int slot = (int) (address >>> 32);
        return slot > 0 && slot <= SLOTS.size();
    }

    private static Buffer slot(long address) {
        if (address == NULL) {
            throw new NullPointerException("the null address names no buffer");
        }
        if (!isKnown(address)) {
            throw new IllegalArgumentException("address " + address + " was never handed out by "
                    + "this build; only a buffer's own address can be read back");
        }
        return SLOTS.get((int) (address >>> 32) - 1);
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
