package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VertexArrayTest {

    private RecordingGraphicsDevice device;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        GLState.attach(device);
        device.calls.clear();
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    private static FloatBuffer positions(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    @Test
    void aClientArrayIsUploadedAndDrawn() {
        FloatBuffer vertices = positions(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, vertices);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        assertEquals(List.of("byteDataARRAY 36 STREAM"), device.calls("byteDataARRAY"));
        assertEquals(List.of("drawArrays TRIANGLES 0 3"), device.calls("drawArrays"));
    }

    @Test
    void aClientArrayIsRereadOnEveryDraw() {
        FloatBuffer vertices = positions(1.0f, 2.0f, 3.0f);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, vertices);

        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);
        byte[] first = device.lastByteData.clone();

        vertices.put(0, 9.0f);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        // the pointer was set once; desktop code refills the buffer behind it every frame
        assertFalse(java.util.Arrays.equals(first, device.lastByteData));
        assertEquals(2, device.calls("byteDataARRAY").size());
    }

    @Test
    void aStrideOfZeroMeansTightlyPacked() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls("attributePointer").contains(
                "attributePointer 0 3 FLOAT false 12 0"));
    }

    @Test
    void aDisabledColorArrayFallsBackToTheCurrentColor() {
        GL11.glColor4f(0.25f, 0.5f, 0.75f, 1.0f);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls.contains("disableAttribute 1"));
        assertTrue(device.calls.contains("constantAttribute 1 0.25 0.5 0.75 1.0"));
    }

    @Test
    void byteColorsAreNormalized() {
        ByteBuffer colors = BufferUtils.createByteBuffer(4);
        colors.put(new byte[] {(byte) 255, 0, 0, (byte) 255}).flip();
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));
        GL11.glColorPointer(4, true, 0, colors);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls("attributePointer").contains(
                "attributePointer 1 4 UNSIGNED_BYTE true 4 0"));
    }

    @Test
    void nothingIsDrawnWithoutPositions() {
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glColorPointer(4, 0, positions(1.0f, 1.0f, 1.0f, 1.0f));
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls("drawArrays").isEmpty());
    }

    @Test
    void quadsFromAVertexArrayExpandFromTheirFirstVertex() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(new float[24]));
        GL11.glDrawArrays(GL11.GL_QUADS, 4, 4);

        assertArrayContains(new int[] {4, 5, 6, 4, 6, 7}, device.lastElementData);
        assertEquals(List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"),
                device.calls("drawElements"));
    }

    @Test
    void anUnsupportedClientArrayNamesItself() {
        // gl_edge_flag_array, undeclared here for the same reason
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glEnableClientState(0x8079));
        assertTrue(failure.getMessage().contains("glEnableClientState"));
    }

    @Test
    void aVertexBufferIsBoundRatherThanUploadedAtDrawTime() {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positions(0.0f, 0.0f, 0.0f), GL15.GL_STATIC_DRAW);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0L);
        device.calls.clear();

        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls("byteDataARRAY").isEmpty());
        assertTrue(device.calls.contains("bindARRAY " + buffer));
    }

    @Test
    void bufferUploadsCarryOnlyTheRemainingElements() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
        FloatBuffer data = positions(1.0f, 2.0f, 3.0f, 4.0f);
        data.position(2);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);

        assertEquals(List.of("byteDataARRAY 8 STATIC"), device.calls("byteDataARRAY"));
    }

    @Test
    void aSizedBufferReservesStorageWithoutData() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, GL15.glGenBuffers());
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 1024L, GL15.GL_DYNAMIC_DRAW);

        assertEquals(List.of("reserveARRAY 1024 DYNAMIC"), device.calls("reserveARRAY"));
    }

    @Test
    void theApplicationsBindingsSurviveADraw() {
        int vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));
        device.calls.clear();

        // the pipeline binds its own staging buffer to upload the client array
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        List<String> binds = device.calls("bindARRAY");
        assertEquals("bindARRAY " + vertexBuffer, binds.get(binds.size() - 1));
    }

    @Test
    void deletingABufferUnbindsIt() {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glDeleteBuffers(buffer);

        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0L));
    }

    @Test
    void anOffsetPointerWithoutABufferSaysWhatToDoInstead() {
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0L));
        assertTrue(failure.getMessage().contains("glVertexPointer"));
        assertTrue(failure.getMessage().contains("GL_ARRAY_BUFFER"));
    }

    @Test
    void drawElementsNeedsAnElementBuffer() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));

        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glDrawElements(GL11.GL_TRIANGLES, 3, GL11.GL_UNSIGNED_SHORT, 0L));
        assertTrue(failure.getMessage().contains("client-side index arrays"));
    }

    @Test
    void indexedQuadsAreRefusedRatherThanDrawnWrong() {
        IntBuffer indices = BufferUtils.createIntBuffer(6);
        indices.put(new int[] {0, 1, 2, 0, 2, 3}).flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, GL15.glGenBuffers());
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(new float[12]));

        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glDrawElements(GL11.GL_QUADS, 6, GL11.GL_UNSIGNED_INT, 0L));
        assertTrue(failure.getMessage().contains("GL_QUADS"));
    }

    @Test
    void indexedTrianglesDrawFromTheBoundElementBuffer() {
        IntBuffer indices = BufferUtils.createIntBuffer(3);
        indices.put(new int[] {0, 1, 2}).flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, GL15.glGenBuffers());
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(new float[9]));

        GL11.glDrawElements(GL11.GL_TRIANGLES, 3, GL11.GL_UNSIGNED_INT, 0L);

        assertEquals(List.of("drawElements TRIANGLES 3 UNSIGNED_INT 0"),
                device.calls("drawElements"));
    }

    @Test
    void drawingInsideGlBeginIsRejected() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, positions(0.0f, 0.0f, 0.0f));
        GL11.glBegin(GL11.GL_TRIANGLES);

        assertThrows(IllegalStateException.class,
                () -> GL11.glDrawArrays(GL11.GL_POINTS, 0, 1));
    }

    private static void assertArrayContains(int[] expected, int[] actual) {
        assertTrue(actual.length >= expected.length,
                "expected at least " + expected.length + " indices, got " + actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "index " + i);
        }
    }
}
