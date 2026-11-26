package org.lwjgl;

import dev.flagon.platform.UnsupportedCapabilityException;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

/**
 * the text encoding half of lwjgl utils
 */
public final class MemoryUtil {

    private MemoryUtil() {
    }

    public static ByteBuffer encodeASCII(CharSequence text) {
        ByteBuffer target = BufferUtils.createByteBuffer(text.length());
        for (int i = 0; i < text.length(); i++) {
            target.put((byte) text.charAt(i));
        }
        target.flip();
        return target;
    }

    public static ByteBuffer encodeUTF8(CharSequence text) {
        byte[] encoded = text.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer target = BufferUtils.createByteBuffer(encoded.length);
        target.put(encoded);
        target.flip();
        return target;
    }

    public static ByteBuffer encodeUTF16(CharSequence text) {
        ByteBuffer target = BufferUtils.createByteBuffer(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            target.putChar(text.charAt(i));
        }
        target.flip();
        return target;
    }

    public static String decodeASCII(ByteBuffer source) {
        StringBuilder text = new StringBuilder(source.remaining());
        for (int i = source.position(); i < source.limit(); i++) {
            text.append((char) (source.get(i) & 0xFF));
        }
        return text.toString();
    }

    public static String decodeUTF8(ByteBuffer source) {
        return new String(copyRemaining(source), StandardCharsets.UTF_8);
    }

    public static String decodeUTF16(ByteBuffer source) {
        StringBuilder text = new StringBuilder(source.remaining() / 2);
        for (int i = source.position(); i + 1 < source.limit(); i += 2) {
            text.append(source.getChar(i));
        }
        return text.toString();
    }

    private static byte[] copyRemaining(ByteBuffer source) {
        byte[] bytes = new byte[source.remaining()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = source.get(source.position() + i);
        }
        return bytes;
    }

    public static long getAddress0(Buffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(ByteBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(ByteBuffer buffer, int position) {
        throw noAddresses();
    }

    public static long getAddress(ShortBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(CharBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(IntBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(LongBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(FloatBuffer buffer) {
        throw noAddresses();
    }

    public static long getAddress(DoubleBuffer buffer) {
        throw noAddresses();
    }

    private static UnsupportedCapabilityException noAddresses() {
        return new UnsupportedCapabilityException(
                "buffers have no address in the browser; pass the buffer itself instead");
    }
}
