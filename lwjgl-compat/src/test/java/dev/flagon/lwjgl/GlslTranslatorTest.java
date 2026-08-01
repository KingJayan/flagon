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
    void aBuiltinNamedInALineCommentIsNotUsed() {
        String source = """
                #version 120
                // ported from gl_Vertex, which this no longer uses
                attribute vec3 position;
                void main() { gl_Position = vec4(position, 1.0); }
                """;

        String translated = GlslTranslator.toGlslEs(source, ShaderStage.VERTEX);

        assertTrue(translated.contains("// ported from gl_Vertex"), translated);
    }

    @Test
    void aBuiltinNamedInABlockCommentIsNotUsed() {
        String source = """
                /* the desktop original multiplied by gl_ModelViewProjectionMatrix
                   and read gl_Color */
                attribute vec3 position;
                void main() { gl_Position = vec4(position, 1.0); }
                """;

        assertEquals(source, GlslTranslator.toGlslEs(source, ShaderStage.VERTEX));
    }

    /** a comment carried onto the next line by a backslash is still a comment */
    @Test
    void aBuiltinNamedInAContinuedLineCommentIsNotUsed() {
        String source = "// the original read \\\ngl_Vertex\n"
                + "void main() { gl_Position = vec4(0.0); }";

        assertEquals(source, GlslTranslator.toGlslEs(source, ShaderStage.VERTEX));
    }

    @Test
    void aVersionInACommentIsNotTheDirective() {
        String translated = GlslTranslator.toGlslEs("""
                // this was #version 330 core on the desktop
                #version 120
                void main() { gl_FragColor = vec4(1.0); }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.startsWith("precision mediump float;"), translated);
        assertFalse(translated.contains("#version 120"), translated);
        assertTrue(translated.contains("#version 330 core"), translated);
    }

    /** the precision a comment mentions is not one the shader declares */
    @Test
    void aCommentedOutPrecisionDoesNotCount() {
        String translated = GlslTranslator.toGlslEs("""
                /* precision highp float; */
                void main() { gl_FragColor = vec4(1.0); }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.startsWith("precision mediump float;"), translated);
    }

    @Test
    void anUnterminatedBlockCommentSwallowsTheRest() {
        String source = "void main() { gl_FragColor = vec4(1.0); }\n/* gl_Color";

        assertEquals(source, GlslTranslator.toGlslEs(source, ShaderStage.VERTEX));
    }

    /** a division is not the start of a comment */
    @Test
    void aSlashBetweenOperandsIsLeftAlone() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs(
                        "void main() { gl_Position = gl_Vertex / 2.0; }", ShaderStage.VERTEX));

        assertTrue(thrown.getMessage().contains("gl_Vertex"), thrown.getMessage());
    }

    @Test
    void nullSourceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GlslTranslator.toGlslEs(null, ShaderStage.VERTEX));
    }
}
