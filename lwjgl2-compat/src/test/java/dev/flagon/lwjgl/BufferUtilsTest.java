package dev.flagon.lwjgl;

import dev.flagon.platform.UnsupportedCapabilityException;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferUtilsTest {

    @Test
    void buffersAreDirectAndSizedInElements() {
        assertTrue(BufferUtils.createByteBuffer(16).isDirect());
        assertEquals(16, BufferUtils.createByteBuffer(16).capacity());
        assertEquals(4, BufferUtils.createFloatBuffer(4).capacity());
        assertEquals(3, BufferUtils.createIntBuffer(3).capacity());
        assertEquals(2, BufferUtils.createDoubleBuffer(2).capacity());
    }

    /** a big-endian default would scramble every typed view of the same bytes. */
    @Test
    void buffersUseNativeByteOrder() {
        assertEquals(ByteOrder.nativeOrder(), BufferUtils.createByteBuffer(4).order());
    }

    @Test
    void typedViewsSeeTheSameBytes() {
        ByteBuffer bytes = BufferUtils.createByteBuffer(8);
        FloatBuffer floats = bytes.asFloatBuffer();
        floats.put(0, 1.5f);
        floats.put(1, -2.25f);

        assertEquals(1.5f, bytes.asFloatBuffer().get(0), 0.0f);
        assertEquals(-2.25f, bytes.asFloatBuffer().get(1), 0.0f);
    }

    @Test
    void elementSizeExponentMatchesTheElementWidth() {
        assertEquals(0, BufferUtils.getElementSizeExponent(BufferUtils.createByteBuffer(1)));
        assertEquals(1, BufferUtils.getElementSizeExponent(BufferUtils.createShortBuffer(1)));
        assertEquals(2, BufferUtils.getElementSizeExponent(BufferUtils.createIntBuffer(1)));
        assertEquals(2, BufferUtils.getElementSizeExponent(BufferUtils.createFloatBuffer(1)));
        assertEquals(3, BufferUtils.getElementSizeExponent(BufferUtils.createLongBuffer(1)));
    }

    @Test
    void zeroBufferClearsOnlyTheRemainingElements() {
        IntBuffer buffer = BufferUtils.createIntBuffer(4);
        for (int i = 0; i < 4; i++) {
            buffer.put(i, 7);
        }
        buffer.position(2);
        BufferUtils.zeroBuffer(buffer);

        assertEquals(7, buffer.get(0));
        assertEquals(7, buffer.get(1));
        assertEquals(0, buffer.get(2));
        assertEquals(0, buffer.get(3));
    }

    @Test
    void textRoundTripsThroughEveryEncoding() {
        assertEquals("flagon", MemoryUtil.decodeASCII(MemoryUtil.encodeASCII("flagon")));
        assertEquals("héllo ☃", MemoryUtil.decodeUTF8(MemoryUtil.encodeUTF8("héllo ☃")));
        assertEquals("héllo ☃", MemoryUtil.decodeUTF16(MemoryUtil.encodeUTF16("héllo ☃")));
    }

    @Test
    void utf8EncodesToBytesNotChars() {
        assertEquals(1, MemoryUtil.encodeUTF8("a").remaining());
        assertEquals(2, MemoryUtil.encodeUTF8("é").remaining());
        assertEquals(3, MemoryUtil.encodeUTF8("☃").remaining());
    }

    @Test
    void decodingLeavesThePositionAlone() {
        ByteBuffer encoded = MemoryUtil.encodeUTF8("flagon");
        MemoryUtil.decodeUTF8(encoded);
        assertEquals(0, encoded.position());
        assertEquals("flagon", MemoryUtil.decodeUTF8(encoded));
    }

    @Test
    void addressesAreRefusedRatherThanFaked() {
        UnsupportedCapabilityException failure = assertThrows(UnsupportedCapabilityException.class,
                () -> MemoryUtil.getAddress(BufferUtils.createByteBuffer(4)));
        assertTrue(failure.getMessage().contains("no address"), failure.getMessage());
    }
}
