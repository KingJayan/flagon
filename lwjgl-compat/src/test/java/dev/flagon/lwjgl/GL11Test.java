package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GL11Test {

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

    @Test
    void clearTranslatesTheBitMask() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        assertEquals(java.util.List.of("clear true true false"), device.calls("clear"));
    }

    @Test
    void immediateModeBuildsInterleavedVertices() {
        GL11.glColor4f(1.0f, 0.5f, 0.25f, 1.0f);
        GL11.glTexCoord2f(0.5f, 0.75f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(1.0f, 2.0f, 3.0f);
        GL11.glEnd();

        assertArrayEquals(
                new float[] {1.0f, 2.0f, 3.0f, 1.0f, 0.5f, 0.25f, 1.0f, 0.5f, 0.75f},
                device.lastBufferData, 1.0e-6f);
        assertEquals(java.util.List.of("drawArrays TRIANGLES 0 1"), device.calls("drawArrays"));
    }

    @Test
    void colorAppliesToLaterVerticesOnly() {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor3f(1.0f, 0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glColor3f(0.0f, 1.0f, 0.0f);
        GL11.glVertex2f(1.0f, 1.0f);
        GL11.glEnd();

        assertEquals(1.0f, device.lastBufferData[3], 1.0e-6f);
        assertEquals(0.0f, device.lastBufferData[4], 1.0e-6f);
        assertEquals(0.0f, device.lastBufferData[12], 1.0e-6f);
        assertEquals(1.0f, device.lastBufferData[13], 1.0e-6f);
    }

    @Test
    void theVertexBufferGrowsBeyondItsInitialCapacity() {
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < 5000; i++) {
            GL11.glVertex3f(i, 0.0f, 0.0f);
        }
        GL11.glEnd();

        assertEquals(java.util.List.of("drawArrays POINTS 0 5000"), device.calls("drawArrays"));
        assertEquals(5000 * 9, device.lastBufferData.length);
        assertEquals(4999.0f, device.lastBufferData[4999 * 9], 1.0e-6f);
    }

    @Test
    void emptyBatchesDoNotIssueADrawCall() {
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glEnd();
        assertTrue(device.calls("drawArrays").isEmpty());
    }

    @Test
    void matrixStateReachesTheShader() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, 800.0, 600.0, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(10.0f, 20.0f, 0.0f);

        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glEnd();

        assertEquals(10.0f, device.lastModelView[12], 1.0e-6f);
        assertEquals(20.0f, device.lastModelView[13], 1.0e-6f);
        assertEquals(2.0f / 800.0f, device.lastProjection[0], 1.0e-6f);
    }

    @Test
    void pushAndPopRestoreTheMatrix() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glPushMatrix();
        GL11.glTranslatef(5.0f, 0.0f, 0.0f);
        GL11.glPopMatrix();

        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glEnd();

        assertEquals(0.0f, device.lastModelView[12], 1.0e-6f);
    }

    @Test
    void poppingAnEmptyStackFailsLoudly() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        assertThrows(IllegalStateException.class, GL11::glPopMatrix);
    }

    @Test
    void nestedBeginIsRejected() {
        GL11.glBegin(GL11.GL_TRIANGLES);
        assertThrows(IllegalStateException.class, () -> GL11.glBegin(GL11.GL_TRIANGLES));
    }

    @Test
    void vertexOutsideBeginIsRejected() {
        assertThrows(IllegalStateException.class, () -> GL11.glVertex2f(0.0f, 0.0f));
    }

    @Test
    void unsupportedCapabilitiesAreReportedRatherThanIgnored() {
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glEnable(GL11.GL_LIGHTING));
        assertTrue(failure.getMessage().contains("glEnable"), failure.getMessage());
    }

    @Test
    void quadsAreReportedAsNotYetEmulated() {
        assertThrows(UnsupportedOperationException.class, () -> GL11.glBegin(GL11.GL_QUADS));
    }

    @Test
    void capabilitiesReachTheDevice() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        assertEquals(java.util.List.of("depthTest true"), device.calls("depthTest"));
        assertEquals(java.util.List.of("blend false"), device.calls("blend"));
        assertEquals(java.util.List.of("blendFunc SRC_ALPHA ONE_MINUS_SRC_ALPHA"),
                device.calls("blendFunc"));
    }

    @Test
    void glCallsBeforeDisplayCreateFailWithAClearMessage() {
        GLState.detach();
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> GL11.glClear(0));
        assertTrue(failure.getMessage().contains("Display.create()"), failure.getMessage());
    }
}
