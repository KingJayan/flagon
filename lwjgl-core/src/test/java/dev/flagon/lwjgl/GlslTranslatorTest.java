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
    void fixedFunctionBuiltinsBecomeTheUniformsThePipelineFeeds() {
        String translated = GlslTranslator.toGlslEs("""
                #version 120
                void main() { gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }
                """, ShaderStage.VERTEX);

        assertTrue(translated.startsWith("#version 300 es"), translated);
        assertTrue(translated.contains("uniform mat4 u_modelView;"), translated);
        assertTrue(translated.contains("uniform mat4 u_projection;"), translated);
        assertTrue(translated.contains("layout(location = 0) in vec3 in_position;"), translated);
        assertTrue(translated.contains("(u_projection * u_modelView) * vec4(in_position, 1.0)"),
                translated);
        assertFalse(translated.contains("gl_Vertex"), translated);
    }

    @Test
    void ftransformBecomesAFunctionOverTheSameUniforms() {
        String translated = GlslTranslator.toGlslEs(
                "void main() { gl_Position = ftransform(); }", ShaderStage.VERTEX);

        assertTrue(translated.contains("vec4 flagon_ftransform()"), translated);
        assertTrue(translated.contains(
                "return u_projection * u_modelView * vec4(in_position, 1.0);"), translated);
        assertTrue(translated.contains("gl_Position = flagon_ftransform();"), translated);
    }

    @Test
    void aShaderIsFixedFunctionOnlyWhenItNamesTheState() {
        assertTrue(GlslTranslator.readsFixedFunction("void main() { gl_Position = ftransform(); }"));
        assertFalse(GlslTranslator.readsFixedFunction(
                "in vec4 a; void main() { gl_Position = a; }"));
    }

    @Test
    void stateWithNothingBehindItIsStillRefused() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs("""
                        #version 120
                        void main() { gl_Position = gl_LightSource[0].position; }
                        """, ShaderStage.VERTEX));

        assertTrue(thrown.getMessage().contains("gl_LightSource"), thrown.getMessage());
    }

    @Test
    void aFragmentShaderReadsTheVaryingsTheVertexShaderWrote() {
        String translated = GlslTranslator.toGlslEs("""
                #version 120
                void main() { gl_FragColor = gl_Color * gl_TexCoord[0]; }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.contains("in vec4 flagon_frontColor;"), translated);
        assertTrue(translated.contains("in vec4 flagon_texCoord[4];"), translated);
        assertTrue(translated.contains("flagon_FragColor = flagon_frontColor * flagon_texCoord[0]"),
                translated);
    }

    @Test
    void fogParametersBecomeAStructTheLayerUploads() {
        String translated = GlslTranslator.toGlslEs("""
                #version 120
                void main() { gl_FragColor = vec4(gl_Fog.color.rgb, gl_Fog.density); }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.contains("uniform flagon_FogParameters u_fog;"), translated);
        assertTrue(translated.contains("u_fog.color.rgb"), translated);
    }

    @Test
    void nineteenTwentyKeywordsBecomeInAndOut() {
        String vertex = GlslTranslator.toGlslEs("""
                #version 120
                attribute float extra;
                varying vec2 uv;
                void main() { uv = gl_MultiTexCoord0.st; gl_Position = ftransform() * extra; }
                """, ShaderStage.VERTEX);

        assertTrue(vertex.contains("in float extra;"), vertex);
        assertTrue(vertex.contains("out vec2 uv;"), vertex);
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
        String translated = GlslTranslator.toGlslEs(
                "void main() { gl_Position = gl_Vertex / 2.0; }", ShaderStage.VERTEX);

        assertTrue(translated.contains("vec4(in_position, 1.0) / 2.0"), translated);
    }

    @Test
    void aCoreProfileVertexShaderBecomesEsThreeHundred() {
        String translated = GlslTranslator.toGlslEs("""
                #version 150 core
                in vec3 position;
                out vec2 uv;
                void main() { gl_Position = vec4(position, 1.0); }
                """, ShaderStage.VERTEX);

        assertTrue(translated.startsWith("#version 300 es\n"), translated);
        assertFalse(translated.contains("150"), translated);
        assertTrue(translated.contains("in vec3 position;"), translated);
        assertFalse(translated.contains("precision"), translated);
    }

    @Test
    void aCoreProfileFragmentShaderGetsAnOutputAndAPrecision() {
        String translated = GlslTranslator.toGlslEs("""
                #version 150 core
                uniform sampler2D atlas;
                in vec2 uv;
                void main() { gl_FragColor = texture2D(atlas, uv); }
                """, ShaderStage.FRAGMENT);

        assertTrue(translated.startsWith("#version 300 es\nprecision highp float;\n"), translated);
        assertTrue(translated.contains("out vec4 flagon_FragColor;"), translated);
        assertTrue(translated.contains("flagon_FragColor = texture(atlas, uv);"), translated);
        assertFalse(translated.contains("gl_FragColor"), translated);
        assertFalse(translated.contains("texture2D("), translated);
    }

    @Test
    void aShaderThatDeclaresItsOwnOutputKeepsIt() {
        String translated = GlslTranslator.toGlslEs("""
                #version 150 core
                precision mediump float;
                out vec4 colour;
                void main() { colour = vec4(1.0); }
                """, ShaderStage.FRAGMENT);

        assertFalse(translated.contains("flagon_FragColor"), translated);
        assertFalse(translated.contains("precision highp float;"), translated);
    }

    @Test
    void anExtensionDirectiveStaysAheadOfTheInjectedDeclarations() {
        String translated = GlslTranslator.toGlslEs("""
                #version 150 core
                #extension GL_ARB_explicit_attrib_location : require
                in vec2 uv;
                void main() { gl_FragColor = vec4(uv, 0.0, 1.0); }
                """, ShaderStage.FRAGMENT);

        int extension = translated.indexOf("#extension");
        assertTrue(extension >= 0, translated);
        assertTrue(extension < translated.indexOf("precision highp float;"), translated);
        assertTrue(extension < translated.indexOf("out vec4 flagon_FragColor;"), translated);
    }

    @Test
    void gl330IsTranslatedAndAnythingAboveIsRefused() {
        assertTrue(GlslTranslator.toGlslEs("#version 330 core\nvoid main() {}\n",
                ShaderStage.VERTEX).startsWith("#version 300 es"));

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs("#version 400 core\nvoid main() {}\n",
                        ShaderStage.VERTEX));
        assertTrue(thrown.getMessage().contains("400"), thrown.getMessage());
    }

    @Test
    void multipleFragmentOutputsAreRefused() {
        assertThrows(UnsupportedOperationException.class,
                () -> GlslTranslator.toGlslEs("""
                        #version 150 core
                        void main() { gl_FragData[0] = vec4(1.0); }
                        """, ShaderStage.FRAGMENT));
    }

    @Test
    void nullSourceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GlslTranslator.toGlslEs(null, ShaderStage.VERTEX));
    }
}
