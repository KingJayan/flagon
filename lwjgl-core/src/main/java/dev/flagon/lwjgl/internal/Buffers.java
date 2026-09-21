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

    private static final boolean LITTLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

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
        // read only the part the program filled
        return copy(source, 0, source.limit(), into);
    }

    public static byte[] readAll(Buffer source) {
        byte[] bytes = new byte[source.capacity() * elementSize(source)];
        readAll(source, bytes);
        return bytes;
    }

    public static byte[] readRemaining(Buffer source) {
        byte[] bytes = new byte[source.remaining() * elementSize(source)];
        copy(source, source.position(), source.limit(), bytes);
        return bytes;
    }

    /** copies elements [from, to) as native-order bytes; answers byte count */
    public static int copy(Buffer source, int from, int to, byte[] into) {
        int count = to - from;
        if (count <= 0) {
            return 0;
        }
        if (source instanceof ByteBuffer bytes) {
            read(bytes, from, into, count);
            return count;
        }
        if (source instanceof ShortBuffer shorts) {
            short[] read = new short[count];
            read(shorts, from, read, count);
            for (int i = 0; i < count; i++) {
                putShort(into, i * 2, read[i]);
            }
            return count * 2;
        }
        if (source instanceof IntBuffer ints) {
            int[] read = new int[count];
            read(ints, from, read, count);
            for (int i = 0; i < count; i++) {
                putInt(into, i * 4, read[i]);
            }
            return count * 4;
        }
        if (source instanceof FloatBuffer floats) {
            float[] read = new float[count];
            read(floats, from, read, count);
            for (int i = 0; i < count; i++) {
                putInt(into, i * 4, Float.floatToRawIntBits(read[i]));
            }
            return count * 4;
        }
        throw new IllegalArgumentException(
                "unsupported buffer type " + source.getClass().getName());
    }

    private static void read(ByteBuffer source, int from, byte[] into, int count) {
        ByteBuffer view = source.duplicate();
        view.position(from);
        view.get(into, 0, count);
    }

    private static void read(ShortBuffer source, int from, short[] into, int count) {
        ShortBuffer view = source.duplicate();
        view.position(from);
        view.get(into, 0, count);
    }

    private static void read(IntBuffer source, int from, int[] into, int count) {
        IntBuffer view = source.duplicate();
        view.position(from);
        view.get(into, 0, count);
    }

    private static void read(FloatBuffer source, int from, float[] into, int count) {
        FloatBuffer view = source.duplicate();
        view.position(from);
        view.get(into, 0, count);
    }

    private static void putShort(byte[] into, int at, short value) {
        if (LITTLE) {
            into[at] = (byte) value;
            into[at + 1] = (byte) (value >> 8);
        } else {
            into[at] = (byte) (value >> 8);
            into[at + 1] = (byte) value;
        }
    }

    private static void putInt(byte[] into, int at, int value) {
        if (LITTLE) {
            into[at] = (byte) value;
            into[at + 1] = (byte) (value >> 8);
            into[at + 2] = (byte) (value >> 16);
            into[at + 3] = (byte) (value >> 24);
        } else {
            into[at] = (byte) (value >> 24);
            into[at + 1] = (byte) (value >> 16);
            into[at + 2] = (byte) (value >> 8);
            into[at + 3] = (byte) value;
        }
    }
}
