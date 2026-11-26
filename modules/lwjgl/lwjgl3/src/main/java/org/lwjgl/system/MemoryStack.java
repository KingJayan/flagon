package org.lwjgl.system;

import dev.flagon.lwjgl.internal.Addresses;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.PointerBuffer;

/** lwjgl 3's scratch allocator, over one arena that frames carve out of */
public final class MemoryStack implements AutoCloseable, Pointer {

    private static final int DEFAULT_SIZE = 64 * 1024;
    private static final MemoryStack SHARED = new MemoryStack(DEFAULT_SIZE);

    private final ByteBuffer arena;
    private final int[] frames = new int[64];
    private final int[] marks = new int[64];
    private int depth;
    private int used;
    private int start;

    private MemoryStack(int size) {
        arena = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static MemoryStack create() {
        return new MemoryStack(DEFAULT_SIZE);
    }

    public static MemoryStack create(int size) {
        return new MemoryStack(size);
    }

    public static MemoryStack stackGet() {
        return SHARED;
    }

    public static MemoryStack stackPush() {
        return SHARED.push();
    }

    public static void stackPop() {
        SHARED.pop();
    }

    public MemoryStack push() {
        if (depth == frames.length) {
            throw new IllegalStateException("memory stack is " + frames.length + " frames deep; "
                    + "a frame is never being popped");
        }
        frames[depth] = used;
        marks[depth] = Addresses.mark();
        depth++;
        return this;
    }

    public MemoryStack pop() {
        if (depth == 0) {
            throw new IllegalStateException("no memory stack frame to pop");
        }
        depth--;
        used = frames[depth];
        Addresses.reset(marks[depth]);
        return this;
    }

    @Override
    public void close() {
        pop();
    }

    public int getSize() {
        return arena.capacity();
    }

    public int getPointer() {
        return used;
    }

    public int getFrameIndex() {
        return depth;
    }

    public ByteBuffer malloc(int size) {
        return malloc(1, size);
    }

    /** @param alignment the byte boundary the block must start on */
    public ByteBuffer malloc(int alignment, int size) {
        start = (used + alignment - 1) / alignment * alignment;
        if (start + size > arena.capacity()) {
            throw new OutOfMemoryError("memory stack holds " + arena.capacity() + " bytes and "
                    + (start + size) + " were asked for; create one with more");
        }
        used = start + size;
        ByteBuffer view = arena.duplicate();
        view.position(start);
        view.limit(start + size);
        ByteBuffer block = view.slice();
        block.order(arena.order());
        return Addresses.derive(block, arena, start);
    }

    public ByteBuffer calloc(int size) {
        ByteBuffer block = malloc(size);
        for (int at = 0; at < size; at++) {
            block.put(at, (byte) 0);
        }
        return block;
    }

    public ByteBuffer bytes(byte... values) {
        ByteBuffer block = malloc(values.length);
        block.put(values);
        block.flip();
        return block;
    }

    public ShortBuffer mallocShort(int size) {
        return typed(malloc(Short.BYTES, size * Short.BYTES).asShortBuffer());
    }

    public ShortBuffer callocShort(int size) {
        return typed(calloc(Short.BYTES, size * Short.BYTES).asShortBuffer());
    }

    public IntBuffer mallocInt(int size) {
        return typed(malloc(Integer.BYTES, size * Integer.BYTES).asIntBuffer());
    }

    public IntBuffer callocInt(int size) {
        return typed(calloc(Integer.BYTES, size * Integer.BYTES).asIntBuffer());
    }

    public IntBuffer ints(int... values) {
        IntBuffer block = mallocInt(values.length);
        block.put(values);
        block.flip();
        return block;
    }

    public LongBuffer mallocLong(int size) {
        return typed(malloc(Long.BYTES, size * Long.BYTES).asLongBuffer());
    }

    public LongBuffer callocLong(int size) {
        return typed(calloc(Long.BYTES, size * Long.BYTES).asLongBuffer());
    }

    public LongBuffer longs(long... values) {
        LongBuffer block = mallocLong(values.length);
        block.put(values);
        block.flip();
        return block;
    }

    public FloatBuffer mallocFloat(int size) {
        return typed(malloc(Float.BYTES, size * Float.BYTES).asFloatBuffer());
    }

    public FloatBuffer callocFloat(int size) {
        return typed(calloc(Float.BYTES, size * Float.BYTES).asFloatBuffer());
    }

    public FloatBuffer floats(float... values) {
        FloatBuffer block = mallocFloat(values.length);
        block.put(values);
        block.flip();
        return block;
    }

    public DoubleBuffer mallocDouble(int size) {
        return typed(malloc(Double.BYTES, size * Double.BYTES).asDoubleBuffer());
    }

    public DoubleBuffer callocDouble(int size) {
        return typed(calloc(Double.BYTES, size * Double.BYTES).asDoubleBuffer());
    }

    public DoubleBuffer doubles(double... values) {
        DoubleBuffer block = mallocDouble(values.length);
        block.put(values);
        block.flip();
        return block;
    }

    public PointerBuffer mallocPointer(int size) {
        return PointerBuffer.create(malloc(Long.BYTES, size * Long.BYTES));
    }

    public PointerBuffer callocPointer(int size) {
        return PointerBuffer.create(calloc(Long.BYTES, size * Long.BYTES));
    }

    public PointerBuffer pointers(long... values) {
        PointerBuffer block = mallocPointer(values.length);
        for (long value : values) {
            block.put(value);
        }
        block.flip();
        return block;
    }

    public ByteBuffer UTF8(CharSequence text) {
        return UTF8(text, true);
    }

    public ByteBuffer UTF8(CharSequence text, boolean terminated) {
        byte[] encoded = text.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer block = malloc(encoded.length + (terminated ? 1 : 0));
        block.put(encoded);
        if (terminated) {
            block.put((byte) 0);
        }
        block.flip();
        return block;
    }

    public ByteBuffer ASCII(CharSequence text) {
        return ASCII(text, true);
    }

    public ByteBuffer ASCII(CharSequence text, boolean terminated) {
        ByteBuffer block = malloc(text.length() + (terminated ? 1 : 0));
        for (int at = 0; at < text.length(); at++) {
            block.put((byte) text.charAt(at));
        }
        if (terminated) {
            block.put((byte) 0);
        }
        block.flip();
        return block;
    }

    public long UTF8Safe(CharSequence text) {
        return text == null ? MemoryUtil.NULL : Addresses.of(UTF8(text));
    }

    /** a view of the block just handed out, addressed inside the arena it was cut from */
    private <B extends java.nio.Buffer> B typed(B view) {
        return Addresses.derive(view, arena, start);
    }

    private ByteBuffer calloc(int alignment, int size) {
        ByteBuffer block = malloc(alignment, size);
        for (int at = 0; at < size; at++) {
            block.put(at, (byte) 0);
        }
        return block;
    }

    @Override
    public long address() {
        return Addresses.of(arena, used);
    }
}
