package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GL11Test {

    private static final int VERTEX_FLOATS =
            dev.flagon.lwjgl.internal.FixedFunctionPipeline.VERTEX_FLOATS;

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
                new float[] {1.0f, 2.0f, 3.0f, 1.0f, 0.5f, 0.25f, 1.0f,
                        0.5f, 0.75f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 1.0f},
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
        assertEquals(0.0f, device.lastBufferData[VERTEX_FLOATS + 3], 1.0e-6f);
        assertEquals(1.0f, device.lastBufferData[VERTEX_FLOATS + 4], 1.0e-6f);
    }

    @Test
    void theVertexBufferGrowsBeyondItsInitialCapacity() {
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < 5000; i++) {
            GL11.glVertex3f(i, 0.0f, 0.0f);
        }
        GL11.glEnd();

        assertEquals(java.util.List.of("drawArrays POINTS 0 5000"), device.calls("drawArrays"));
        assertEquals(5000 * VERTEX_FLOATS, device.lastBufferData.length);
        assertEquals(4999.0f, device.lastBufferData[4999 * VERTEX_FLOATS], 1.0e-6f);
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
        // gl_line_stipple, which this layer does not declare and will not honor
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glEnable(0x0B24));
        assertTrue(failure.getMessage().contains("glEnable"), failure.getMessage());
    }

    @Test
    void quadsExpandIntoTwoTrianglesPerQuad() {
        drawQuads(1);

        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, device.lastElementData);
        assertEquals(java.util.List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"),
                device.calls("drawElements"));
        assertTrue(device.calls("drawArrays").isEmpty());
        // the vertices go up untouched; only the index order changes
        assertEquals(4 * VERTEX_FLOATS, device.lastBufferData.length);
    }

    @Test
    void quadIndicesAreBasedOnEachQuadsFirstVertex() {
        drawQuads(3);

        assertArrayEquals(
                new int[] {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11},
                device.lastElementData);
        assertEquals(java.util.List.of("drawElements TRIANGLES 18 UNSIGNED_INT 0"),
                device.calls("drawElements"));
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

        assertEquals(java.util.List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"),
                device.calls("drawElements"));
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

        assertEquals(java.util.List.of("textureImage 0 2x1"), device.calls("textureImage"));
        assertArrayEquals(new byte[] {0x10, 0x20, 0x30, 0x40, (byte) 0xFF, 0, 0, (byte) 0xFF},
                device.lastRgba);
        assertEquals(0, pixels.position(), "the caller's buffer was consumed");
    }

    @Test
    void widensAnRgbUploadToTheRgbaTheDeviceTakes() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(6);
        pixels.put((byte) 0x10).put((byte) 0x20).put((byte) 0x30);
        pixels.put((byte) 0x40).put((byte) 0x50).put((byte) 0x60);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 0x1907, 2, 1, 0,
                0x1907, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(new byte[] {0x10, 0x20, 0x30, (byte) 0xFF,
                0x40, 0x50, 0x60, (byte) 0xFF}, device.lastRgba);
    }

    @Test
    void readsABgraUploadInTheOrderItWasGiven() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);
        pixels.put((byte) 0x10).put((byte) 0x20).put((byte) 0x30).put((byte) 0x40);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, 1, 1, 0,
                GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(new byte[] {0x30, 0x20, 0x10, 0x40}, device.lastRgba);
    }

    @Test
    void takesThePackedPixelTypeMinecraftUploadsWith() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);
        pixels.put((byte) 0x10).put((byte) 0x20).put((byte) 0x30).put((byte) 0x40);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, 1, 1, 0,
                GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);

        assertArrayEquals(new byte[] {0x30, 0x20, 0x10, 0x40}, device.lastRgba);
    }

    @Test
    void reportsThatALargeProxyTextureDoesNotFit() {
        GL11.glTexImage2D(GL11.GL_PROXY_TEXTURE_2D, 0, GL11.GL_RGBA, 8192, 8192, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);

        assertEquals(0, GL11.glGetTexLevelParameteri(GL11.GL_PROXY_TEXTURE_2D, 0,
                GL11.GL_TEXTURE_WIDTH));
        assertEquals(java.util.List.of(), device.calls("textureStorage"));
    }

    @Test
    void reportsTheSizeOfAProxyTextureThatFits() {
        GL11.glTexImage2D(GL11.GL_PROXY_TEXTURE_2D, 0, GL11.GL_RGBA, 4096, 2048, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);

        assertEquals(4096, GL11.glGetTexLevelParameteri(GL11.GL_PROXY_TEXTURE_2D, 0,
                GL11.GL_TEXTURE_WIDTH));
        assertEquals(2048, GL11.glGetTexLevelParameteri(GL11.GL_PROXY_TEXTURE_2D, 0,
                GL11.GL_TEXTURE_HEIGHT));
    }

    @Test
    void reservesALevelForANullIntBuffer() {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, 4, 2, 0,
                GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (java.nio.IntBuffer) null);

        assertEquals(java.util.List.of("textureStorage 0 4x2"), device.calls("textureStorage"));
    }

    @Test
    void takesThePackedTypeFromIntsToo() {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, 1, 1, 0,
                GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, new int[] {0x40102030});

        assertArrayEquals(new byte[] {0x10, 0x20, 0x30, 0x40}, device.lastRgba);
    }

    @Test
    void refusesThePackedTypeForAFormatItCannotFill() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 1, 1, 0,
                        GL11.GL_RGB, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels));
        assertTrue(failure.getMessage().contains("four-component"), failure.getMessage());
    }

    @Test
    void keepsTheNormalRescalingFlagsItIsAlreadyHonouring() {
        assertFalse(GL11.glIsEnabled(GL11.GL_NORMALIZE));
        GL11.glEnable(GL11.GL_NORMALIZE);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        assertTrue(GL11.glIsEnabled(GL11.GL_NORMALIZE));
        assertTrue(GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL));

        GL11.glDisable(GL11.GL_NORMALIZE);
        assertFalse(GL11.glIsEnabled(GL11.GL_NORMALIZE));
        assertTrue(GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL));
    }

    @Test
    void answersTheDrawRangeHintsRatherThanRefusingThem() {
        assertEquals(Integer.MAX_VALUE, GL11.glGetInteger(GL12.GL_MAX_ELEMENTS_VERTICES));
        assertEquals(Integer.MAX_VALUE, GL11.glGetInteger(GL12.GL_MAX_ELEMENTS_INDICES));
    }

    @Test
    void spreadsALuminanceUploadAcrossThecolorChannels() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(2);
        pixels.put((byte) 0x7F).put((byte) 0x40);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_LUMINANCE_ALPHA, 1, 1, 0,
                GL11.GL_LUMINANCE_ALPHA, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(new byte[] {0x7F, 0x7F, 0x7F, 0x40}, device.lastRgba);
    }

    @Test
    void anAlphaUploadIsBlackWhereItIsOpaque() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(1);
        pixels.put((byte) 0x80);
        pixels.flip();

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, 1, 1, 0,
                GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(new byte[] {0, 0, 0, (byte) 0x80}, device.lastRgba);
    }

    @Test
    void aSubImageIsWidenedTheSameWay() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(3);
        pixels.put((byte) 0x11).put((byte) 0x22).put((byte) 0x33);
        pixels.flip();

        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1, 2, 1, 1,
                0x1907, GL11.GL_UNSIGNED_BYTE, pixels);

        assertArrayEquals(new byte[] {0x11, 0x22, 0x33, (byte) 0xFF}, device.lastRgba);
    }

    @Test
    void aFormatWithNoRgbaAnswerNamesItself() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);

        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 0x1902, 1, 1, 0,
                        0x1902, GL11.GL_UNSIGNED_BYTE, pixels));
        assertTrue(failure.getMessage().contains("format 0x1902"), failure.getMessage());
    }

    @Test
    void aPixelTypeThatIsNotBytesNamesItself() {
        java.nio.ByteBuffer pixels = org.lwjgl.BufferUtils.createByteBuffer(4);

        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0,
                        GL11.GL_RGBA, GL11.GL_FLOAT, pixels));
        assertTrue(failure.getMessage().contains("GL_UNSIGNED_BYTE"), failure.getMessage());
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

        assertEquals(java.util.List.of("textureSubImage 0 1,1 1x1"),
                device.calls("textureSubImage"));
        assertArrayEquals(new byte[] {0x11, 0x22, 0x33, (byte) 0xFF}, device.lastRgba);
    }

    @Test
    void setsWrappingAndFilteringIndependentlyOnEachAxis() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_MIRRORED_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        assertEquals(java.util.List.of("textureWrapS REPEAT"), device.calls("textureWrapS"));
        assertEquals(java.util.List.of("textureWrapT MIRRORED_REPEAT"),
                device.calls("textureWrapT"));
        assertEquals(java.util.List.of("textureMinFilter NEAREST"),
                device.calls("textureMinFilter"));
        assertEquals(java.util.List.of("textureMagFilter LINEAR"),
                device.calls("textureMagFilter"));
    }

    @Test
    void desktopClampBecomesTheEdgeClampWebglHas() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);

        assertEquals(java.util.List.of("textureWrapS CLAMP_TO_EDGE"), device.calls("textureWrapS"));
    }

    @Test
    void takesAMipmappedFilterForMinificationAndRefusesOneForMagnification() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR_MIPMAP_LINEAR);
        assertEquals(java.util.List.of("textureMinFilter LINEAR_MIPMAP_LINEAR"),
                device.calls("textureMinFilter"));

        assertThrows(IllegalArgumentException.class,
                () -> GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                        GL11.GL_LINEAR_MIPMAP_LINEAR));
    }

    @Test
    void uploadsIntoTheLevelItWasGiven() {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 2, GL11.GL_RGBA, 1, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF});
        assertEquals(java.util.List.of("textureImage 2 1x1"), device.calls("textureImage"));

        assertThrows(IllegalArgumentException.class,
                () -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, -1, GL11.GL_RGBA, 1, 1, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF}));
    }

    @Test
    void buildsTheLevelsBelowTheBaseOnRequest() {
        GL11.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        assertTrue(device.calls.contains("generateMipmap"));
    }

    @Test
    void generateMipmapRebuildsTheLevelsAfterEveryUpload() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
        int built = java.util.Collections.frequency(device.calls, "generateMipmap");

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF});
        assertEquals(built + 1, java.util.Collections.frequency(device.calls, "generateMipmap"));

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_FALSE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, new int[] {0xFFFFFFFF});
        assertEquals(built + 1, java.util.Collections.frequency(device.calls, "generateMipmap"));
    }

    @Test
    void boundsTheLevelsThatMayBeSampled() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BASE_LEVEL, 1);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAX_LEVEL, 4);

        assertEquals(java.util.List.of("textureBaseLevel 1"), device.calls("textureBaseLevel"));
        assertEquals(java.util.List.of("textureMaxLevel 4"), device.calls("textureMaxLevel"));
    }

    @Test
    void setsTheLevelRangeAndIgnoresTheBias() {
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0.5f);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 4);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0.0f);

        assertEquals(java.util.List.of("textureMinLod 0.5"), device.calls("textureMinLod"));
        assertEquals(java.util.List.of("textureMaxLod 4.0"), device.calls("textureMaxLod"));
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
        device.extensions = "EXT_color_buffer_float";
        assertEquals("EXT_color_buffer_float", GL11.glGetString(GL11.GL_EXTENSIONS));
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

    @Test
    void aDisplayListReplaysTheCallsItRecorded() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glClearColor(0.25f, 0.5f, 0.75f, 1.0f);
        GL11.glEndList();

        assertEquals(java.util.List.of(), device.calls("clearColor"));

        GL11.glCallList(list);
        assertEquals(java.util.List.of("clearColor 0.25 0.5 0.75 1.0"), device.calls("clearColor"));
    }

    @Test
    void compileAndExecuteBothRecordsAndRuns() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE);
        GL11.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GL11.glEndList();

        assertEquals(1, device.calls("clearColor").size());
        GL11.glCallList(list);
        assertEquals(2, device.calls("clearColor").size());
    }

    @Test
    void aListHoldsTheVerticesAQuadBlockBuiltRatherThanEachCall() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glColor3f(1.0f, 0.0f, 0.0f);
        GL11.glVertex2f(1.0f, 2.0f);
        GL11.glEnd();
        GL11.glEndList();

        assertEquals(java.util.List.of(), device.calls("drawArrays"));

        GL11.glCallList(list);
        assertEquals(java.util.List.of("drawArrays TRIANGLES 0 1"), device.calls("drawArrays"));
        assertEquals(1.0f, device.lastBufferData[3], 1.0e-6f);
    }

    @Test
    void aCallToAListThatWasNeverBuiltIsIgnored() {
        GL11.glCallList(4242);
        assertEquals(java.util.List.of(), device.calls("drawArrays"));
    }

    @Test
    void deletingAListForgetsIt() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GL11.glEndList();
        assertTrue(GL11.glIsList(list));

        GL11.glDeleteLists(list, 1);
        assertFalse(GL11.glIsList(list));
        GL11.glCallList(list);
        assertEquals(java.util.List.of(), device.calls("clearColor"));
    }

    @Test
    void refusesToCompileACallWhoseDataWouldHaveMovedOnByTheReplay() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3));
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                        GL11.GL_REPEAT));
        GL11.glEndList();
    }

    @Test
    void loadsAndMultipliesTheMatrixTheApplicationSupplies() {
        float[] scale = new float[16];
        scale[0] = 2.0f;
        scale[5] = 2.0f;
        scale[10] = 2.0f;
        scale[15] = 1.0f;

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrixf(scale);
        float[] read = new float[16];
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, java.nio.FloatBuffer.wrap(read));
        assertEquals(2.0f, read[0], 1.0e-6f);

        GL11.glMultMatrixf(scale);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, java.nio.FloatBuffer.wrap(read));
        assertEquals(4.0f, read[0], 1.0e-6f);
    }

    @Test
    void aLoadedMatrixIsCopiedSoTheCallerMayReuseTheArray() {
        float[] matrix = new float[16];
        matrix[0] = 3.0f;
        GL11.glLoadMatrixf(matrix);
        matrix[0] = 9.0f;

        float[] read = new float[16];
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, java.nio.FloatBuffer.wrap(read));
        assertEquals(3.0f, read[0], 1.0e-6f);
    }

    @Test
    void popAttribPutsBackOnlyTheGroupsTheMaskNamed() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glPopAttrib();

        assertTrue(GL11.glIsEnabled(GL11.GL_DEPTH_TEST));
        assertTrue(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
    }

    @Test
    void popAttribRestoresTheColorBufferGroupThroughToTheDevice() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glPopAttrib();

        assertEquals("blendFunc SRC_ALPHA ONE_MINUS_SRC_ALPHA",
                device.calls("blendFunc").getLast());
        assertEquals(GL11.GL_SRC_ALPHA, GL11.glGetInteger(GL11.GL_BLEND_SRC));
    }

    @Test
    void anEmptyAttributeStackSaysSoRatherThanRestoringNothing() {
        assertThrows(IllegalStateException.class, GL11::glPopAttrib);
    }

    @Test
    void reportsTheStateItWasSetTo() {
        GL11.glViewport(10, 20, 30, 40);
        GL11.glScissor(1, 2, 3, 4);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glDepthFunc(GL11.GL_GEQUAL);

        int[] read = new int[4];
        GL11.glGetInteger(GL11.GL_VIEWPORT, java.nio.IntBuffer.wrap(read));
        assertArrayEquals(new int[] {10, 20, 30, 40}, read);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, java.nio.IntBuffer.wrap(read));
        assertArrayEquals(new int[] {1, 2, 3, 4}, read);
        assertEquals(GL11.GL_PROJECTION, GL11.glGetInteger(GL11.GL_MATRIX_MODE));
        assertEquals(GL11.GL_GEQUAL, GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
    }

    @Test
    void reportsWhichCapabilitiesAreOn() {
        assertFalse(GL11.glIsEnabled(GL11.GL_BLEND));
        GL11.glEnable(GL11.GL_BLEND);
        assertTrue(GL11.glIsEnabled(GL11.GL_BLEND));
        assertThrows(UnsupportedOperationException.class, () -> GL11.glIsEnabled(0x4242));
    }

    @Test
    void stencilStateReachesTheDeviceAsTheEnumsItNames() {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0x0F);
        GL11.glClearStencil(3);

        assertEquals(java.util.List.of("stencilTest true"), device.calls("stencilTest"));
        assertEquals(java.util.List.of("stencilFunc EQUAL 1 255"), device.calls("stencilFunc"));
        assertEquals(java.util.List.of("stencilOp KEEP KEEP REPLACE"), device.calls("stencilOp"));
        assertEquals(java.util.List.of("stencilMask 15"), device.calls("stencilMask"));
        assertEquals(java.util.List.of("clearStencil 3"), device.calls("clearStencil"));
    }

    @Test
    void refusesAStencilOperationItCannotName() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, 0x4242));
    }

    @Test
    void offsetsPolygonsAndWidensLines() {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0f, 2.0f);
        GL11.glLineWidth(3.0f);

        assertEquals(java.util.List.of("polygonOffsetFill true"),
                device.calls("polygonOffsetFill"));
        assertEquals(java.util.List.of("polygonOffset 1.0 2.0"), device.calls("polygonOffset"));
        assertEquals(java.util.List.of("lineWidth 3.0"), device.calls("lineWidth"));
        assertThrows(IllegalArgumentException.class, () -> GL11.glLineWidth(0.0f));
    }

    @Test
    void aPointSizeReachesTheShaderThatDrawsThePoints() {
        GL11.glPointSize(8.0f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(1.0f, 1.0f);
        GL11.glEnd();

        assertTrue(device.calls("uniform1f").stream().anyMatch(call -> call.endsWith(" 8.0")),
                "the point size was not sent: " + device.calls("uniform1f"));
        assertThrows(IllegalArgumentException.class, () -> GL11.glPointSize(-1.0f));
    }

    @Test
    void takesAHintAndIgnoresIt() {
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glHint(0x4242, GL11.GL_NICEST));
        assertThrows(IllegalArgumentException.class,
                () -> GL11.glHint(GL11.GL_FOG_HINT, 0x4242));
    }

    @Test
    void fillIsTheOnlyPolygonModeThereIs() {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE));
    }

    @Test
    void readsThePixelsBackAsBytesAndAsPackedInts() {
        java.nio.ByteBuffer bytes = java.nio.ByteBuffer.allocate(8);
        GL11.glReadPixels(2, 3, 2, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
        assertEquals(java.util.List.of("readPixels 2 3 2 1"), device.calls("readPixels"));
        assertEquals(0x10, bytes.get(0) & 0xFF);
        assertEquals(0x40, bytes.get(3) & 0xFF);

        java.nio.IntBuffer packed = java.nio.IntBuffer.allocate(2);
        GL11.glReadPixels(0, 0, 2, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, packed);
        assertEquals(0x40102030, packed.get(0));
    }

    @Test
    void refusesToReadIntoABufferThatIsTooSmall() {
        java.nio.ByteBuffer bytes = java.nio.ByteBuffer.allocate(4);
        assertThrows(IllegalArgumentException.class,
                () -> GL11.glReadPixels(0, 0, 2, 2, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes));
    }

    @Test
    void copiesTheFramebufferIntoTheBoundTexture() {
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1, 2, 3, 4, 5, 6);
        assertEquals(java.util.List.of("copyTextureSubImage 1 2 3 4 5 6"),
                device.calls("copyTextureSubImage"));
        assertThrows(IllegalArgumentException.class,
                () -> GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, -1, 0, 0, 0, 0, 1, 1));
    }

    @Test
    void finishAndFlushReachTheDevice() {
        GL11.glFinish();
        GL11.glFlush();
        assertTrue(device.calls.contains("finish"));
        assertTrue(device.calls.contains("flush"));
    }

    @Test
    void aTextureIsBoundToTheUnitThatIsActive() {
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 7);

        assertEquals(java.util.List.of("activeTexture 1"), device.calls("activeTexture"));
        assertEquals(java.util.List.of("bindTexture 1 7"), device.calls("bindTexture"));
        assertEquals(7, GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
    }

    @Test
    void eachUnitIsEnabledSeparately() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        assertFalse(GL11.glIsEnabled(GL11.GL_TEXTURE_2D));

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        assertTrue(GL11.glIsEnabled(GL11.GL_TEXTURE_2D));
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        assertTrue(GL11.glIsEnabled(GL11.GL_TEXTURE_2D));
    }

    @Test
    void everyUnitGetsItsOwnCoordinateInTheVertex() {
        GL11.glTexCoord2f(0.1f, 0.2f);
        org.lwjgl.opengl.GL13.glMultiTexCoord2f(org.lwjgl.opengl.GL13.GL_TEXTURE1, 0.3f, 0.4f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glEnd();

        assertEquals(0.1f, device.lastBufferData[7], 1.0e-6f);
        assertEquals(0.2f, device.lastBufferData[8], 1.0e-6f);
        assertEquals(0.3f, device.lastBufferData[9], 1.0e-6f);
        assertEquals(0.4f, device.lastBufferData[10], 1.0e-6f);
    }

    @Test
    void theCoordinateArrayBelongsToTheClientActiveUnit() {
        org.lwjgl.opengl.GL13.glClientActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glTexCoordPointer(2, 0, java.nio.FloatBuffer.wrap(new float[] {0.0f, 0.0f}));

        org.lwjgl.opengl.GL13.glClientActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

        assertTrue(GLState.get().arrays().texCoord(1).isEnabled());
        assertFalse(GLState.get().arrays().texCoord(0).isEnabled());
    }

    @Test
    void refusesAUnitTheFixedFunctionPipelineDoesNotHave() {
        assertThrows(UnsupportedOperationException.class,
                () -> org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE5));
        assertThrows(IllegalArgumentException.class,
                () -> org.lwjgl.opengl.GL13.glActiveTexture(0));
    }

    @Test
    void theTextureEnvironmentChoosesHowAUnitIsCombined() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glEnd();

        assertTrue(device.calls("uniform1i").stream().anyMatch(call -> call.endsWith(" 1")),
                "the environment mode was not sent: " + device.calls("uniform1i"));
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
                        GL11.GL_COMBINE));
    }
}
