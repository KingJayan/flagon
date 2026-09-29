package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.ClientArray;
import dev.flagon.lwjgl.internal.StagingArrays;
import dev.flagon.lwjgl.internal.VertexArrays;
import dev.flagon.platform.graphics.AttributeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StagingArraysTest {

    private RecordingGraphicsDevice device;
    private StagingArrays staging;
    private VertexArrays arrays;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        staging = new StagingArrays(device, VertexArrays.SLOTS);
        arrays = new VertexArrays();
    }

    private FloatBuffer floats(float... values) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(values.length * Float.BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(values).flip();
        return buffer;
    }

    private ClientArray pointedAt(FloatBuffer client) {
        ClientArray array = arrays.slot(VertexArrays.POSITION);
        array.pointAt(client, 3, AttributeType.FLOAT, false, 0);
        return array;
    }

    private long uploads() {
        return device.calls.stream().filter((call) -> call.startsWith("bufferSubData")).count();
    }

    @Test
    void uploadsTheClientRangeOnce() {
        ClientArray array = pointedAt(floats(1.0f, 2.0f, 3.0f));

        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);
        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);
        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);

        assertEquals(1, uploads());
    }

    @Test
    void uploadsAgainWhenTheClientBytesChange() {
        FloatBuffer client = floats(1.0f, 2.0f, 3.0f);
        ClientArray array = pointedAt(client);
        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);

        client.put(1, 9.0f);
        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);

        assertEquals(2, uploads());
        assertEquals(9.0f, device.lastBufferData[1]);
    }

    @Test
    void uploadsAgainWhenTheBufferIsResized() {
        staging.bind(VertexArrays.POSITION, pointedAt(floats(1.0f, 2.0f, 3.0f)), 0,
                StagingArrays.WHOLE, 0);

        staging.bind(VertexArrays.POSITION, pointedAt(floats(1.0f, 2.0f, 3.0f, 4.0f)), 0,
                StagingArrays.WHOLE, 0);

        assertEquals(2, uploads());
    }

    @Test
    void leavesTheClientPositionAlone() {
        FloatBuffer client = floats(1.0f, 2.0f, 3.0f);
        ClientArray array = pointedAt(client);

        staging.bind(VertexArrays.POSITION, array, 0, StagingArrays.WHOLE, 0);

        assertEquals(0, client.position());
    }
}
