package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.flagon.lwjgl.internal.Buffers;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

class BuffersTest {

    @Test
    void readsAFlippedBufferUpToItsLimit() {
        FloatBuffer source = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder()).asFloatBuffer();
        source.put(1f).put(2f);
        source.flip();

        byte[] into = new byte[16];
        assertEquals(8, Buffers.readAll(source, into));

        ByteBuffer read = ByteBuffer.wrap(into).order(ByteOrder.nativeOrder());
        assertEquals(1f, read.getFloat(0));
        assertEquals(2f, read.getFloat(4));
    }

    @Test
    void readsAWholeBufferWhenNothingLimitsIt() {
        ByteBuffer source = ByteBuffer.allocate(3).put((byte) 7).put((byte) 8).put((byte) 9);

        byte[] into = new byte[3];
        assertEquals(3, Buffers.readAll(source, into));
        assertArrayEquals(new byte[] {7, 8, 9}, into);
    }
}
