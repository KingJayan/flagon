package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.FixedFunctionStage;
import dev.flagon.lwjgl.internal.GlslTranslator;
import dev.flagon.platform.graphics.ShaderStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedFunctionStageTest {

    private static String translated(String source, ShaderStage stage) {
        return GlslTranslator.toGlslEs(source, stage);
    }

    @Test
    void aVertexShaderAloneGetsAFragmentStageOverTheVaryingsItWrites() {
        String vertex = translated("""
                #version 120
                void main() {
                    gl_FrontColor = gl_Color;
                    gl_TexCoord[0] = gl_MultiTexCoord0;
                    gl_Position = ftransform();
                }
                """, ShaderStage.VERTEX);

        String fragment = FixedFunctionStage.companionFor(vertex, ShaderStage.VERTEX);

        assertTrue(fragment.startsWith("#version 300 es"), fragment);
        assertTrue(fragment.contains("in vec4 flagon_frontColor;"), fragment);
        assertTrue(fragment.contains("in vec4 flagon_texCoord[4];"), fragment);
        assertTrue(fragment.contains("out vec4 out_color;"), fragment);
        assertTrue(fragment.contains("texture(u_texture0, flagon_texCoord[0].st)"), fragment);
        assertTrue(fragment.contains("u_alphaTestEnabled"), fragment);
    }

    /** a vertex output nothing reads is legal, so a varying of the shader's own is ignored */
    @Test
    void aVaryingOfItsOwnIsNotCarriedIntoTheWrittenStage() {
        String vertex = translated("""
                #version 120
                varying vec2 uv;
                void main() {
                    uv = vec2(0.0);
                    gl_FrontColor = gl_Color;
                    gl_Position = ftransform();
                }
                """, ShaderStage.VERTEX);

        String fragment = FixedFunctionStage.companionFor(vertex, ShaderStage.VERTEX);

        assertFalse(fragment.contains("uv"), fragment);
    }

    @Test
    void aVertexShaderThatWritesNoColorStillGetsAStageThatDraws() {
        String vertex = translated(
                "void main() { gl_Position = ftransform(); }", ShaderStage.VERTEX);

        String fragment = FixedFunctionStage.companionFor(vertex, ShaderStage.VERTEX);

        assertTrue(fragment.contains("vec4 color = vec4(1.0);"), fragment);
        assertFalse(fragment.contains("in vec4 flagon_frontColor;"), fragment);
    }

    @Test
    void aFragmentShaderAloneGetsAVertexStageThatFeedsIt() {
        String fragment = translated("""
                #version 120
                void main() { gl_FragColor = gl_Color * gl_TexCoord[0]; }
                """, ShaderStage.FRAGMENT);

        String vertex = FixedFunctionStage.companionFor(fragment, ShaderStage.FRAGMENT);

        assertTrue(vertex.contains("layout(location = 0) in vec3 in_position;"), vertex);
        assertTrue(vertex.contains("layout(location = 1) in vec4 in_color;"), vertex);
        assertTrue(vertex.contains("out vec4 flagon_frontColor;"), vertex);
        assertTrue(vertex.contains("out vec4 flagon_texCoord[4];"), vertex);
        assertTrue(vertex.contains("gl_Position = u_projection * eye;"), vertex);
    }

    @Test
    void fogIsCarriedOnlyWhenTheVertexStageWritesTheCoordinate() {
        String withFog = FixedFunctionStage.companionFor(translated("""
                #version 120
                void main() { gl_FogFragCoord = 1.0; gl_Position = ftransform(); }
                """, ShaderStage.VERTEX), ShaderStage.VERTEX);
        String withoutFog = FixedFunctionStage.companionFor(translated(
                "void main() { gl_Position = ftransform(); }", ShaderStage.VERTEX),
                ShaderStage.VERTEX);

        assertTrue(withFog.contains("u_fogEnabled"), withFog);
        assertFalse(withoutFog.contains("u_fogEnabled"), withoutFog);
    }

    /** nothing can write a varying only the application's fragment shader knows about */
    @Test
    void aFragmentShaderReadingItsOwnVaryingIsRefused() {
        String fragment = translated("""
                #version 150 core
                in vec2 uv;
                void main() { gl_FragColor = vec4(uv, 0.0, 1.0); }
                """, ShaderStage.FRAGMENT);

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> FixedFunctionStage.companionFor(fragment, ShaderStage.FRAGMENT));

        assertTrue(thrown.getMessage().contains("uv"), thrown.getMessage());
    }
}
