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
import java.util.List;

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

    private void draw(ClientArray array) {
        staging.begin();
        staging.need(array, 0, StagingArrays.WHOLE);
        staging.bind(VertexArrays.POSITION, array, 0);
    }

    private long uploads() {
        return device.calls.stream().filter((call) -> call.startsWith("bufferSubData")).count();
    }

    @Test
    void uploadsTheClientRangeOnce() {
        ClientArray array = pointedAt(floats(1.0f, 2.0f, 3.0f));

        draw(array);
        draw(array);
        draw(array);

        assertEquals(1, uploads());
    }

    @Test
    void uploadsAgainWhenTheClientBytesChange() {
        FloatBuffer client = floats(1.0f, 2.0f, 3.0f);
        ClientArray array = pointedAt(client);
        draw(array);

        client.put(1, 9.0f);
        draw(array);

        assertEquals(2, uploads());
        assertEquals(9.0f, device.lastBufferData[1]);
    }

    @Test
    void uploadsAgainWhenTheBufferIsResized() {
        draw(pointedAt(floats(1.0f, 2.0f, 3.0f)));

        draw(pointedAt(floats(1.0f, 2.0f, 3.0f, 4.0f)));

        assertEquals(2, uploads());
    }

    @Test
    void leavesTheClientPositionAlone() {
        FloatBuffer client = floats(1.0f, 2.0f, 3.0f);
        ClientArray array = pointedAt(client);

        draw(array);

        assertEquals(0, client.position());
    }

    /** position and color interleaved in one buffer, the way minecraft lays out a vertex */
    @Test
    void uploadsAnInterleavedBufferOnceForEveryArrayInIt() {
        ByteBuffer vertices = ByteBuffer.allocateDirect(2 * 16).order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < 2; vertex++) {
            vertices.putFloat(vertex).putFloat(0.0f).putFloat(0.0f).putInt(0xFF0000FF);
        }
        vertices.flip();
        ClientArray position = arrays.slot(VertexArrays.POSITION);
        position.pointAt(vertices, 3, AttributeType.FLOAT, false, 16);
        vertices.position(12);
        ClientArray color = arrays.slot(VertexArrays.COLOR);
        color.pointAt(vertices, 4, AttributeType.UNSIGNED_BYTE, true, 16);
        vertices.position(0);

        staging.begin();
        staging.need(position, 0, 2);
        staging.need(color, 0, 2);
        staging.bind(VertexArrays.POSITION, position, 0);
        staging.bind(VertexArrays.COLOR, color, 0);

        assertEquals(List.of("byteSubDataARRAY 0 32"), device.calls.stream()
                .filter((call) -> call.startsWith("byteSubData")).toList());
    }
}
