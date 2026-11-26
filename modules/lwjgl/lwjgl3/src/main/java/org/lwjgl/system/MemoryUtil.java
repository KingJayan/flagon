package org.lwjgl.system;

import dev.flagon.lwjgl.internal.Addresses;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.PointerBuffer;

/**
 * lwjgl 3's memory utilities. addresses name a slot in {@link Addresses}, not a location.
 */
public final class MemoryUtil {

    public static final long NULL = Addresses.NULL;

    private MemoryUtil() {
    }

    public static ByteBuffer memAlloc(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer memCalloc(int size) {
        return memAlloc(size);
    }

    public static ShortBuffer memAllocShort(int size) {
        return view(memAlloc(size * Short.BYTES), buffer -> buffer.asShortBuffer());
    }

    public static IntBuffer memAllocInt(int size) {
        return view(memAlloc(size * Integer.BYTES), buffer -> buffer.asIntBuffer());
    }

    public static LongBuffer memAllocLong(int size) {
        return view(memAlloc(size * Long.BYTES), buffer -> buffer.asLongBuffer());
    }

    public static FloatBuffer memAllocFloat(int size) {
        return view(memAlloc(size * Float.BYTES), buffer -> buffer.asFloatBuffer());
    }

    public static DoubleBuffer memAllocDouble(int size) {
        return view(memAlloc(size * Double.BYTES), buffer -> buffer.asDoubleBuffer());
    }

    public static PointerBuffer memAllocPointer(int size) {
        return PointerBuffer.allocateDirect(size);
    }

    /** a typed view addressed inside the bytes it was cut from */
    private static <B extends Buffer> B view(ByteBuffer bytes,
                                             java.util.function.Function<ByteBuffer, B> cut) {
        return Addresses.derive(cut.apply(bytes), bytes, 0);
    }

    /** the collector owns these buffers, so freeing one is nothing but a promise not to use it */
    public static void memFree(Buffer buffer) {
    }

    public static void memFree(PointerBuffer buffer) {
    }

    public static long memAddress(Buffer buffer) {
        return Addresses.of(buffer);
    }

    public static long memAddress(Buffer buffer, int index) {
        return Addresses.of(buffer, index);
    }

    public static long memAddress(PointerBuffer buffer) {
        return buffer.address();
    }

    /** @return the address, or {@link #NULL} when there is no buffer, which glfw reads as absent */
    public static long memAddressSafe(Buffer buffer) {
        return buffer == null ? NULL : Addresses.of(buffer);
    }

    public static long memAddressSafe(PointerBuffer buffer) {
        return buffer == null ? NULL : buffer.address();
    }

    public static ByteBuffer memByteBuffer(long address, int capacity) {
        return Addresses.bytes(address, capacity);
    }

    public static ByteBuffer memByteBufferSafe(long address, int capacity) {
        return address == NULL ? null : memByteBuffer(address, capacity);
    }

    public static IntBuffer memIntBuffer(long address, int capacity) {
        return view(memByteBuffer(address, capacity * Integer.BYTES), ByteBuffer::asIntBuffer);
    }

    public static FloatBuffer memFloatBuffer(long address, int capacity) {
        return view(memByteBuffer(address, capacity * Float.BYTES), ByteBuffer::asFloatBuffer);
    }

    public static LongBuffer memLongBuffer(long address, int capacity) {
        return view(memByteBuffer(address, capacity * Long.BYTES), ByteBuffer::asLongBuffer);
    }

    public static PointerBuffer memPointerBuffer(long address, int capacity) {
        return PointerBuffer.create(memByteBuffer(address, capacity * Long.BYTES));
    }

    public static void memCopy(ByteBuffer source, ByteBuffer target) {
        for (int at = 0; at < source.remaining(); at++) {
            target.put(target.position() + at, source.get(source.position() + at));
        }
    }

    public static ByteBuffer memUTF8(CharSequence text) {
        return memUTF8(text, true);
    }

    public static ByteBuffer memUTF8(CharSequence text, boolean terminated) {
        byte[] encoded = text.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer target = memAlloc(encoded.length + (terminated ? 1 : 0));
        target.put(encoded);
        if (terminated) {
            target.put((byte) 0);
        }
        target.flip();
        return target;
    }

    public static ByteBuffer memASCII(CharSequence text) {
        return memASCII(text, true);
    }

    public static ByteBuffer memASCII(CharSequence text, boolean terminated) {
        ByteBuffer target = memAlloc(text.length() + (terminated ? 1 : 0));
        for (int at = 0; at < text.length(); at++) {
            target.put((byte) text.charAt(at));
        }
        if (terminated) {
            target.put((byte) 0);
        }
        target.flip();
        return target;
    }

    /** @return the text the address names, up to the terminator c strings end with */
    public static String memUTF8(long address) {
        return new String(untilTerminator(address), StandardCharsets.UTF_8);
    }

    public static String memUTF8Safe(long address) {
        return address == NULL ? null : memUTF8(address);
    }

    public static String memASCII(long address) {
        byte[] bytes = untilTerminator(address);
        StringBuilder text = new StringBuilder(bytes.length);
        for (byte value : bytes) {
            text.append((char) (value & 0xFF));
        }
        return text.toString();
    }

    public static String memASCIISafe(long address) {
        return address == NULL ? null : memASCII(address);
    }

    public static String memUTF8(ByteBuffer source) {
        byte[] bytes = new byte[source.remaining()];
        for (int at = 0; at < bytes.length; at++) {
            bytes[at] = source.get(source.position() + at);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] untilTerminator(long address) {
        ByteBuffer bytes = Addresses.bytesToEnd(address);
        int length = 0;
        while (length < bytes.limit() && bytes.get(length) != 0) {
            length++;
        }
        byte[] read = new byte[length];
        for (int at = 0; at < length; at++) {
            read[at] = bytes.get(at);
        }
        return read;
    }
}
