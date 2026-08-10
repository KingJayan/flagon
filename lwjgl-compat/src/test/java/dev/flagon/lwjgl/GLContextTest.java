package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLContextTest {

    private RecordingGraphicsDevice device;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        GLState.attach(device);
        GLContext.useContext(device);
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    @Test
    void theVersionsWithEntryPointsAreTheOnesReported() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        assertTrue(capabilities.OpenGL11);
        assertTrue(capabilities.OpenGL12);
        assertTrue(capabilities.OpenGL13);
        assertTrue(capabilities.OpenGL14);
        assertTrue(capabilities.OpenGL15);
        assertTrue(capabilities.OpenGL20);
        assertFalse(capabilities.OpenGL21);
        assertFalse(capabilities.OpenGL30);
    }

    @Test
    void whatWebglCarriesInCoreIsAlwaysAvailable() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        assertTrue(capabilities.GL_ARB_multitexture);
        assertTrue(capabilities.GL_ARB_vertex_buffer_object);
        assertTrue(capabilities.GL_ARB_shader_objects);
        assertTrue(capabilities.GL_ARB_texture_non_power_of_two);
        assertTrue(capabilities.GL_EXT_blend_func_separate);
    }

    @Test
    void anExtensionTheHostDoesNotListIsNotClaimed() {
        assertFalse(GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic);
        assertFalse(GLContext.getCapabilities().GL_EXT_texture_compression_s3tc);
    }

    @Test
    void anExtensionTheHostListsIsClaimed() {
        device.extensions = "EXT_texture_filter_anisotropic WEBGL_compressed_texture_s3tc";
        GLContext.useContext(device);

        ContextCapabilities capabilities = GLContext.getCapabilities();
        assertTrue(capabilities.GL_EXT_texture_filter_anisotropic);
        assertTrue(capabilities.GL_EXT_texture_compression_s3tc);
    }

    @Test
    void anExtensionIsFoundWithOrWithoutItsPrefix() {
        device.extensions = "EXT_color_buffer_float";
        GLContext.useContext(device);

        ContextCapabilities capabilities = GLContext.getCapabilities();
        assertTrue(capabilities.isSupported("EXT_color_buffer_float"));
        assertTrue(capabilities.isSupported("GL_EXT_color_buffer_float"));
        assertFalse(capabilities.isSupported("EXT_disjoint_timer_query"));
    }

    @Test
    void theExtensionStringIsTheOneGlGetStringGives() {
        device.extensions = "EXT_color_buffer_float OES_texture_float_linear";
        GLContext.useContext(device);

        assertEquals(GL11.glGetString(GL11.GL_EXTENSIONS),
                GLContext.getCapabilities().getExtensions());
    }

    @Test
    void theCapabilitiesAreBuiltOncePerContext() {
        ContextCapabilities first = GLContext.getCapabilities();

        assertSame(first, GLContext.getCapabilities());

        RecordingGraphicsDevice replacement = new RecordingGraphicsDevice();
        GLState.attach(replacement);
        assertNotSame(first, GLContext.getCapabilities());
    }

    @Test
    void askingBeforeTheDisplayIsOpenSaysSo() {
        GLState.detach();

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, GLContext::getCapabilities);
        assertTrue(failure.getMessage().contains("Display.create()"), failure.getMessage());
    }
}
