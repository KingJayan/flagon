package dev.flagon.lwjgl.internal;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * reads nio buffers as flat bytes
 */
public final class Buffers {

    private Buffers() {
    }

    public static int elementSize(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return 1;
        }
        if (buffer instanceof ShortBuffer || buffer instanceof java.nio.CharBuffer) {
            return 2;
        }
        if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) {
            return 4;
        }
        if (buffer instanceof java.nio.LongBuffer || buffer instanceof DoubleBuffer) {
            return 8;
        }
        throw new IllegalArgumentException("unsupported buffer type "
                + buffer.getClass().getName());
    }

    public static int readAll(Buffer source, byte[] into) {
        ByteBuffer target = ByteBuffer.wrap(into).order(ByteOrder.nativeOrder());
        // read only the part the program filled
        int elements = source.limit();
        if (source instanceof ByteBuffer bytes) {
            for (int i = 0; i < elements; i++) {
                target.put(bytes.get(i));
            }
        } else if (source instanceof ShortBuffer shorts) {
            for (int i = 0; i < elements; i++) {
                target.putShort(shorts.get(i));
            }
        } else if (source instanceof IntBuffer ints) {
            for (int i = 0; i < elements; i++) {
                target.putInt(ints.get(i));
            }
        } else if (source instanceof FloatBuffer floats) {
            for (int i = 0; i < elements; i++) {
                target.putFloat(floats.get(i));
            }
        } else {
            throw new IllegalArgumentException(
                    "unsupported buffer type " + source.getClass().getName());
        }
        return target.position();
    }

    public static byte[] readAll(Buffer source) {
        byte[] bytes = new byte[source.capacity() * elementSize(source)];
        readAll(source, bytes);
        return bytes;
    }

    public static byte[] readRemaining(Buffer source) {
        int elementSize = elementSize(source);
        byte[] bytes = new byte[source.remaining() * elementSize];
        ByteBuffer target = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        int from = source.position();
        int to = source.limit();
        if (source instanceof ByteBuffer buffer) {
            for (int i = from; i < to; i++) {
                target.put(buffer.get(i));
            }
        } else if (source instanceof ShortBuffer buffer) {
            for (int i = from; i < to; i++) {
                target.putShort(buffer.get(i));
            }
        } else if (source instanceof IntBuffer buffer) {
            for (int i = from; i < to; i++) {
                target.putInt(buffer.get(i));
            }
        } else if (source instanceof FloatBuffer buffer) {
            for (int i = from; i < to; i++) {
                target.putFloat(buffer.get(i));
            }
        } else {
            throw new IllegalArgumentException(
                    "unsupported buffer type " + source.getClass().getName());
        }
        return bytes;
    }
}
