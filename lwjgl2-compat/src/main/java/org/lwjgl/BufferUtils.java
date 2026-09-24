package org.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * allocates the gl nio buffers
 */
public final class BufferUtils {

    private BufferUtils() {
    }

    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size * 2).asShortBuffer();
    }

    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size * 2).asCharBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size * 4).asIntBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size * 8).asLongBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size * 4).asFloatBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size * 8).asDoubleBuffer();
    }

    /** @return log2 of the buffer's element size in bytes */
    public static int getElementSizeExponent(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 0;
        }
        if (buffer instanceof ShortBuffer || buffer instanceof CharBuffer) {
            return 1;
        }
        if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) {
            return 2;
        }
        if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) {
            return 3;
        }
        throw new IllegalStateException("unsupported buffer type: " + buffer);
    }

    public static void zeroBuffer(ByteBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, (byte) 0);
        }
    }

    public static void zeroBuffer(ShortBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, (short) 0);
        }
    }

    public static void zeroBuffer(CharBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, '\0');
        }
    }

    public static void zeroBuffer(IntBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, 0);
        }
    }

    public static void zeroBuffer(LongBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, 0L);
        }
    }

    public static void zeroBuffer(FloatBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, 0.0f);
        }
    }

    public static void zeroBuffer(DoubleBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            buffer.put(i, 0.0);
        }
    }
}
