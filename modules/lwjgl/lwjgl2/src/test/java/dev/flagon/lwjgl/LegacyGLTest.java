package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.GraphicsDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyGLTest {

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

    private static IntBuffer ints(int... values) {
        IntBuffer buffer = BufferUtils.createIntBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    @Test
    void theArbAndExtNamesReachTheSameFramebufferCalls() {
        int framebuffer = ARBFramebufferObject.glGenFramebuffers();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, framebuffer);
        assertEquals(framebuffer, device.boundFramebuffer());

        int other = EXTFramebufferObject.glGenFramebuffersEXT();
        EXTFramebufferObject.glBindFramebufferEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, other);
        assertEquals(other, device.boundFramebuffer());

        assertEquals(ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE,
                EXTFramebufferObject.glCheckFramebufferStatusEXT(
                        EXTFramebufferObject.GL_FRAMEBUFFER_EXT));
    }

    @Test
    void theArbBufferNamesReachTheSameBufferCalls() {
        int buffer = ARBVertexBufferObject.glGenBuffersARB();
        ARBVertexBufferObject.glBindBufferARB(
                ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, buffer);
        ByteBuffer data = BufferUtils.createByteBuffer(8);
        ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, data,
                ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
        assertTrue(device.calls.stream().anyMatch(call -> call.startsWith("byteData")));
    }

    @Test
    void theArbMultitextureNamesSelectTheSameUnits() {
        ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE1_ARB);
        assertEquals(1, GLState.get().activeTexture());
        ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE2_ARB);
        assertEquals(2, GLState.get().clientActiveTexture());
    }

    @Test
    void theExtBlendNameSeparatesTheSameFactors() {
        EXTBlendFuncSeparate.glBlendFuncSeparateEXT(GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        assertTrue(device.calls.stream().anyMatch(call -> call.startsWith("blendFuncSeparate")));
    }

    @Test
    void aDoubleMatrixCallGoesTheSameWayAsTheFloatOne() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslated(1.0, 2.0, 3.0);
        GL11.glScaled(2.0, 2.0, 2.0);
        float[] moved = GLState.get().modelView();
        assertEquals(1.0f, moved[12], 1.0e-6f);
        assertEquals(2.0f, moved[13], 1.0e-6f);
        assertEquals(2.0f, moved[0], 1.0e-6f);
    }

    @Test
    void aTextureLevelRemembersTheSizeItWasGiven() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 4, 2, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        assertEquals(4, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
        assertEquals(2,
                GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT));
    }

    @Test
    void aReadBackAttachesTheLevelAndPutsTheBindingBack() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 1, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        device.calls.clear();

        IntBuffer read = BufferUtils.createIntBuffer(2);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, read);

        assertTrue(device.calls.stream().anyMatch(call -> call.startsWith("framebufferTexture")));
        assertTrue(device.calls.contains("readPixels 0 0 2 1"));
        assertEquals(GraphicsDevice.NO_FRAMEBUFFER, device.boundFramebuffer());
        assertEquals(0x40102030, read.get(0));
    }

    @Test
    void aReadBackOfALevelWithNoUploadSaysSo() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        IntBuffer read = BufferUtils.createIntBuffer(4);
        assertThrows(IllegalStateException.class, () ->
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                        read));
    }

    @Test
    void anUploadReadsTheRowsPixelStoreiPointsAt() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 1);
        device.calls.clear();

        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 2, 1, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, ints(0, 0, 0, 0, 0, 0x00FF0000, 0x000000FF, 0));
        assertTrue(device.calls.contains("textureSubImage 0 0,0 2x1"));
        assertEquals((byte) 0xFF, device.lastTextureBytes[0]);
        assertEquals((byte) 0xFF, device.lastTextureBytes[6]);
    }

    @Test
    void aPixelStoreiTheDeviceCannotKeepIsRefusedByName() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, GL11.GL_TRUE));
        assertThrows(IllegalArgumentException.class,
                () -> GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 3));
    }

    @Test
    void whatWebglHasNoStageForIsRefusedByName() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, 0x8511));
    }
}
