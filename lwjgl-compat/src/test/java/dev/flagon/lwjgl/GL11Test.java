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
        // gl_lighting, which this layer does not declare and will not honour
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glEnable(0x0B50));
        assertTrue(failure.getMessage().contains("glEnable"), failure.getMessage());
    }

    @Test
    void quadsExpandIntoTwoTrianglesPerQuad() {
        drawQuads(1);

        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, device.lastElementData);
        assertEquals(java.util.List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"), device.calls("drawElements"));
        assertTrue(device.calls("drawArrays").isEmpty());
        // the vertices go up untouched; only the index order changes
        assertEquals(4 * 9, device.lastBufferData.length);
    }

    @Test
    void quadIndicesAreBasedOnEachQuadsFirstVertex() {
        drawQuads(3);

        assertArrayEquals(
                new int[] {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11},
                device.lastElementData);
        assertEquals(java.util.List.of("drawElements TRIANGLES 18 UNSIGNED_INT 0"), device.calls("drawElements"));
    }

    @Test
    void quadIndicesAreUploadedOnlyWhenTheyHaveToGrow() {
        drawQuads(4);
        drawQuads(2);
        assertEquals(1, device.calls("elementData").size());

        drawQuads(9);
        assertEquals(java.util.List.of("elementData 24 STATIC", "elementData 54 STATIC"),
                device.calls("elementData"));
    }

    /** gl drops a trailing partial primitive rather than drawing a malformed one. */
    @Test
    void aTrailingIncompleteQuadIsDropped() {
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < 6; i++) {
            GL11.glVertex2f(i, 0.0f);
        }
        GL11.glEnd();

        assertEquals(java.util.List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"), device.calls("drawElements"));
    }

    @Test
    void aQuadWithTooFewVerticesDrawsNothing() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glVertex2f(1.0f, 0.0f);
        GL11.glEnd();

        assertTrue(device.calls("drawElements").isEmpty());
        assertTrue(device.calls("drawArrays").isEmpty());
    }

    private void drawQuads(int quads) {
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < quads * 4; i++) {
            GL11.glVertex2f(i, 0.0f);
        }
        GL11.glEnd();
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
    @Test
    void uploadsATextureFromTheBufferRealLwjglTakes() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(8);
        pixels.put((byte) 0x10).put((byte) 0x20).put((byte) 0x30).put((byte) 0x40);
        pixels.put((byte) 0xFF).put((byte) 0x00).put((byte) 0x00).put((byte) 0xFF);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        assertEquals(java.util.List.of("textureImage 2x1"), device.calls("textureImage"));
        assertArrayEquals(new byte[] {0x10, 0x20, 0x30, 0x40, (byte) 0xFF, 0, 0, (byte) 0xFF},
                device.lastRgba);
        assertEquals(0, pixels.position(), "the caller's buffer was consumed");
    }

    @Test
    void uploadsATextureFromAnIntBufferToo() {
        java.nio.IntBuffer pixels = org.lwjgl.BufferUtils.createIntBuffer(2);
        pixels.put(0xFF112233).put(0xFF445566);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(
                new byte[] {0x11, 0x22, 0x33, (byte) 0xFF, 0x44, 0x55, 0x66, (byte) 0xFF},
                device.lastRgba);
    }

    @Test
    void uploadsOnlyTheRectangleASubImageNames() {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1, 1, 1, 1,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFF112233});

        assertEquals(java.util.List.of("textureSubImage 1,1 1x1"), device.calls("textureSubImage"));
        assertArrayEquals(new byte[] {0x11, 0x22, 0x33, (byte) 0xFF}, device.lastRgba);
    }

    @Test
    void setsWrappingAndFilteringIndependentlyOnEachAxis() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_MIRRORED_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        assertEquals(java.util.List.of("textureWrapS REPEAT"), device.calls("textureWrapS"));
        assertEquals(java.util.List.of("textureWrapT MIRRORED_REPEAT"), device.calls("textureWrapT"));
        assertEquals(java.util.List.of("textureMinFilter NEAREST"), device.calls("textureMinFilter"));
        assertEquals(java.util.List.of("textureMagFilter LINEAR"), device.calls("textureMagFilter"));
    }

    @Test
    void desktopClampBecomesTheEdgeClampWebglHas() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);

        assertEquals(java.util.List.of("textureWrapS CLAMP_TO_EDGE"), device.calls("textureWrapS"));
    }

    @Test
    void saysThereAreNoMipmapsRatherThanFilteringFromOne() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                        GL11.GL_LINEAR_MIPMAP_LINEAR));

        assertTrue(thrown.getMessage().contains("level 0"), thrown.getMessage());
    }

    @Test
    void refusesAnUploadAboveTheBaseLevelInsteadOfOverwritingIt() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA, 1, 1, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF}));
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 2, 0, 0, 1, 1,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF}));
    }

    @Test
    void reportsTheDeviceErrorOnceAndThenNoError() {
        device.nextError = dev.flagon.platform.graphics.GraphicsError.INVALID_VALUE;

        assertEquals(GL11.GL_INVALID_VALUE, GL11.glGetError());
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
    }

    @Test
    void aLostContextIsReportedAsAnInvalidOperation() {
        device.nextError = dev.flagon.platform.graphics.GraphicsError.CONTEXT_LOST;

        assertEquals(GL11.GL_INVALID_OPERATION, GL11.glGetError());
    }

    @Test
    void answersTheStringsAndLimitsAnApplicationChecksItselfAgainst() {
        assertEquals("recorded RENDERER", GL11.glGetString(GL11.GL_RENDERER));
        assertEquals("recorded EXTENSIONS", GL11.glGetString(GL11.GL_EXTENSIONS));
        assertEquals(4096, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        assertEquals(16, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_IMAGE_UNITS));
        assertThrows(UnsupportedOperationException.class, () -> GL11.glGetInteger(GL11.GL_BLEND));
        assertThrows(UnsupportedOperationException.class, () -> GL11.glGetString(GL11.GL_BLEND));
    }

    @Test
    void turnsTheScissorTestOnAndSetsItsBox() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(10, 20, 30, 40);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        assertEquals(java.util.List.of("scissorTest true", "scissorTest false"),
                device.calls("scissorTest"));
        assertEquals(java.util.List.of("scissor 10,20 30x40"), device.calls("scissor"));
    }

    @Test
    void refusesAScissorBoxThatIsNotARectangle() {
        assertThrows(IllegalArgumentException.class, () -> GL11.glScissor(0, 0, -1, 10));
    }

    @Test
    void choosesWhichFaceIsCulledAndWhichWindingIsFront() {
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glFrontFace(GL11.GL_CW);

        assertEquals(java.util.List.of("culledFaces FRONT"), device.calls("culledFaces"));
        assertEquals(java.util.List.of("frontFace CLOCKWISE"), device.calls("frontFace"));
        assertThrows(UnsupportedOperationException.class, () -> GL11.glCullFace(GL11.GL_BLEND));
        assertThrows(UnsupportedOperationException.class, () -> GL11.glFrontFace(GL11.GL_BLEND));
    }

    @Test
    void namesTheFormatItCannotUploadWhateverTheBufferIs() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);

        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0,
                        GL11.GL_RGBA, GL11.GL_FLOAT, pixels));
    }
}
