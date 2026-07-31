package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GlslTranslator;
import dev.flagon.platform.graphics.ShaderStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslTranslatorTest {

    @Test
    void aDesktopVersionDirectiveIsDropped() {
        String translated = GlslTranslator.toGlslEs("""
                #version 120
                attribute vec3 position;
                void main() { gl_Position = vec4(position, 1.0); }
                """, ShaderStage.VERTEX);

        assertFalse(translated.contains("#version"), translated);
        assertTrue(translated.contains("attribute vec3 position;"));
    }

    @Test
    void anEsVersionDirectiveIsLeftAlone() {
        String source = """
                #version 300 es
                in vec3 position;
                void main() { gl_Position = vec4(position, 1.0); }
                """;

        assertEquals(source, GlslTranslator.toGlslEs(source, ShaderStage.VERTEX));
    }

    @Test
    void aFragmentShaderIsGivenADefaultPrecision() {
        String translated = GlslTranslator.toGlslEs("""
                #version 120
                void main() { gl_FragColor = vec4(1.0); }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.startsWith("precision mediump float;"), translated);
    }

    @Test
    void aFragmentShaderThatSetsItsOwnPrecisionKeepsIt() {
        String translated = GlslTranslator.toGlslEs("""
                precision highp float;
                void main() { gl_FragColor = vec4(1.0); }
                """, ShaderStage.FRAGMENT);

        assertFalse(translated.contains("mediump"), translated);
    }

    /** the vertex stage has a default precision of its own, so adding one would be noise. */
    @Test
    void aVertexShaderIsNotGivenAPrecision() {
        String translated = GlslTranslator.toGlslEs(
                "void main() { gl_Position = vec4(0.0); }", ShaderStage.VERTEX);

        assertFalse(translated.contains("precision"), translated);
    }

    @Test
    void glsl130AndAboveIsRejectedByName() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs("""
                        #version 330 core
                        in vec3 position;
                        void main() { gl_Position = vec4(position, 1.0); }
                        """, ShaderStage.VERTEX));

        assertTrue(thrown.getMessage().contains("330"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("300 es"), thrown.getMessage());
    }

    @Test
    void fixedFunctionBuiltinsAreRejectedByName() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs("""
                        #version 120
                        void main() { gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }
                        """, ShaderStage.VERTEX));

        assertTrue(thrown.getMessage().contains("gl_ModelViewProjectionMatrix"),
                thrown.getMessage());
    }

    @Test
    void ftransformIsRejected() {
        assertThrows(UnsupportedOperationException.class, () -> GlslTranslator.toGlslEs(
                "void main() { gl_Position = ftransform(); }", ShaderStage.VERTEX));
    }

    /** a name that merely contains a built-in is not that built-in. */
    @Test
    void anIdentifierThatOnlyLooksLikeABuiltinIsKept() {
        String source = "attribute vec4 gl_VertexOfMine;\nvoid main() { gl_Position = vec4(0.0); }";

        assertEquals(source, GlslTranslator.toGlslEs(source, ShaderStage.VERTEX));
    }

    @Test
    void nullSourceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GlslTranslator.toGlslEs(null, ShaderStage.VERTEX));
    }
}
