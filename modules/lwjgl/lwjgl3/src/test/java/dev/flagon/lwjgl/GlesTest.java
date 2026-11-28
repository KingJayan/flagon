package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.opengles.GLES30.*;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.EsContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLESCapabilities;

class GlesTest {

    private RecordingEs es;

    @BeforeEach
    void setUp() {
        RecordingGraphicsDevice device = new RecordingGraphicsDevice();
        es = new RecordingEs();
        device.es = es.context;
        GLState.attach(device);
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    @Test
    void reportsGlEsThreeAndTheExtensionsWebglHas() {
        es.answers.put("extensions",
                new String[] {"EXT_color_buffer_float", "WEBGL_compressed_texture_s3tc"});
        GLESCapabilities capabilities = GLES.createCapabilities();
        assertTrue(capabilities.GLES20 && capabilities.GLES30);
        assertFalse(capabilities.GLES31);
        assertTrue(capabilities.GL_EXT_color_buffer_float);
        assertTrue(capabilities.GL_EXT_texture_compression_s3tc);
        assertFalse(capabilities.GL_EXT_texture_filter_anisotropic);
    }

    @Test
    void needsACurrentContext() {
        GLState.detach();
        assertThrows(IllegalStateException.class, GLES::createCapabilities);
    }

    @Test
    void uploadsAClientArrayBeforeTheDraw() {
        es.parameters.put(GL_ARRAY_BUFFER_BINDING, new double[] {7});
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, FloatBuffer.wrap(new float[] {1, 2}));
        glDrawArrays(GL_TRIANGLES, 0, 3);
        assertEquals(List.of("getParameter 34964", "createBuffer", "bindBuffer 34962 1",
                "bufferData 34962 [0, 0, -128, 63, 0, 0, 0, 64] 35040",
                "vertexAttribPointer 0 2 5126 false 0 0", "bindBuffer 34962 7",
                "drawArrays 4 0 3"), es.calls);

        es.calls.clear();
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 16L);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        assertEquals(List.of("vertexAttribPointer 0 2 5126 false 0 16", "drawArrays 4 0 3"),
                es.calls);
    }

    @Test
    void drawsClientIndicesThroughAScratchBuffer() {
        glDrawElements(GL_TRIANGLES, ShortBuffer.wrap(new short[] {0, 1, 2}));
        assertEquals(List.of("getParameter 34965", "createBuffer", "bindBuffer 34963 1",
                "bufferData 34963 [0, 0, 1, 0, 2, 0] 35040", "drawElements 4 3 5123 0",
                "bindBuffer 34963 0"), es.calls);
    }

    @Test
    void readsAnOffsetIntoTheBoundElementBuffer() {
        es.parameters.put(GL_ELEMENT_ARRAY_BUFFER_BINDING, new double[] {3});
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 4L);
        assertEquals("drawElements 4 6 5123 4", es.calls.getLast());
    }

    @Test
    void writesAMappedRangeBackAtUnmap() {
        ByteBuffer mapped = glMapBufferRange(GL_ARRAY_BUFFER, 4, 3,
                GL_MAP_READ_BIT | GL_MAP_WRITE_BIT);
        assertEquals(2, mapped.get(1));
        assertEquals(1, glGetBufferParameteri(GL_ARRAY_BUFFER, GL_BUFFER_MAPPED));
        mapped.put(0, (byte) 9);
        assertTrue(glUnmapBuffer(GL_ARRAY_BUFFER));
        assertEquals("bufferSubData 34962 4 [9, 2, 3]", es.calls.getLast());
        assertFalse(glUnmapBuffer(GL_ARRAY_BUFFER));
    }

    @Test
    void copiesANameTheWayGlDoes() {
        es.answers.put("getActiveUniform",
                new EsContext.Variable("lightColor", 1, GL_FLOAT_VEC3));
        IntBuffer length = IntBuffer.allocate(1);
        IntBuffer size = IntBuffer.allocate(1);
        IntBuffer type = IntBuffer.allocate(1);
        ByteBuffer name = ByteBuffer.allocate(6);
        glGetActiveUniform(1, 0, length, size, type, name);
        assertEquals(5, length.get(0));
        assertEquals(GL_FLOAT_VEC3, type.get(0));
        assertEquals("light", new String(name.array(), 0, 5));
        assertEquals(0, name.get(5));
        assertEquals("lig", glGetActiveUniform(1, 0, 4, size, type));
    }

    @Test
    void passesArraysAsTheirBuffers() {
        glUniform4fv(2, new float[] {1, 2, 3, 4});
        assertEquals("uniformfv 2 4 [1.0, 2.0, 3.0, 4.0]", es.calls.getLast());
        int[] textures = new int[2];
        glGenTextures(textures);
        assertEquals(1, textures[0]);
        assertEquals(2, textures[1]);
    }

    @Test
    void raisesTheErrorsWebglHasNoCallFor() {
        glShaderBinary(IntBuffer.allocate(1), 0, ByteBuffer.allocate(1));
        assertEquals(GL_INVALID_ENUM, glGetError());
        assertEquals(GL_NO_ERROR, glGetError());
    }

    @Test
    void waitsForAFenceAcrossTurns() {
        int[] polls = {0};
        es.answers.put("clientWaitSync", (Supplier<Integer>) () ->
                ++polls[0] < 3 ? GL_TIMEOUT_EXPIRED : GL_ALREADY_SIGNALED);
        long sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        assertEquals(GL_TIMEOUT_EXPIRED, glClientWaitSync(sync, 0, 0));
        assertEquals(GL_ALREADY_SIGNALED, glClientWaitSync(sync, 0, 1_000_000_000L));
        assertEquals(3, polls[0]);
    }

    @Test
    void readsAPixelPointerAsAnAddressOrAnOffset() {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
        assertEquals("texImage2D 3553 0 32856 2 2 0 6408 5121 null", es.calls.getLast());
        es.parameters.put(0x88EF, new double[] {5});
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE, 8L);
        assertEquals("texImage2D 3553 0 32856 2 2 0 6408 5121 8", es.calls.getLast());
    }

    @Test
    void readsPixelsIntoAFloatBuffer() {
        FloatBuffer pixels = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glReadPixels(0, 0, 1, 1, GL_RG, GL_FLOAT, pixels);
        assertEquals(0.5f, pixels.get(0));
        assertEquals(0.25f, pixels.get(1));
        assertEquals(0, pixels.position());
    }
}
