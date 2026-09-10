package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.ShaderStage;

import java.util.ArrayList;
import java.util.List;

/**
 * desktop glsl written against gl's fixed-function state, rewritten to read the same attributes
 * and uniforms the fixed-function pipeline already feeds
 */
final class FixedFunctionGlsl {

    /** what a shader has to name for the fixed-function pipeline to draw it */
    static final String[] BUILTINS = {
            "gl_ModelViewMatrix",
            "gl_ProjectionMatrix",
            "gl_ModelViewProjectionMatrix",
            "gl_NormalMatrix",
            "gl_Vertex",
            "gl_Color",
            "gl_Normal",
            "gl_MultiTexCoord0",
            "gl_MultiTexCoord1",
            "gl_MultiTexCoord2",
            "gl_MultiTexCoord3",
            "gl_TexCoord",
            "gl_FrontColor",
            "gl_BackColor",
            "gl_FogFragCoord",
            "gl_Fog",
            "ftransform",
    };

    /** state with no equivalent here, named so the message can say which one was asked for */
    private static final String[][] UNSUPPORTED = {
            {"gl_LightSource", "per-light state; glLight* feeds the built-in pipeline, but a "
                    + "shader of your own has to take the lights as uniforms it declares"},
            {"gl_TextureMatrix", "the texture matrix stack, which this layer does not keep"},
            {"gl_SecondaryColor", "the secondary colour, which gl 1.4 added and nothing"
                    + " here feeds"},
            {"gl_ClipPlane", "user clip planes, which webgl has no equivalent for"},
    };

    private FixedFunctionGlsl() {
    }

    static boolean isUsedIn(String code) {
        for (String builtin : BUILTINS) {
            if (GlslTranslator.indexOfWord(code, builtin, 0) >= 0) {
                return true;
            }
        }
        return false;
    }

    static void rejectWhatCannotBeFed(String code) {
        for (String[] refused : UNSUPPORTED) {
            if (GlslTranslator.indexOfWord(code, refused[0], 0) >= 0) {
                throw new UnsupportedOperationException(
                        "shader uses '" + refused[0] + "': " + refused[1]);
            }
        }
    }

    /**
     * @param body the shader with its version directive already removed
     * @return the same shader reading flagon's attributes and uniforms instead of gl's
     */
    static String rewrite(String body, ShaderStage stage) {
        String code = GlslTranslator.withoutComments(body);
        List<String> declared = new ArrayList<>();
        String out = body;

        if (stage == ShaderStage.VERTEX) {
            out = rewriteVertex(code, out, declared);
        } else {
            out = rewriteFragment(code, out, declared);
        }
        if (uses(code, "gl_Fog")) {
            declared.add("struct flagon_FogParameters {");
            declared.add("    vec4 color;");
            declared.add("    float density;");
            declared.add("    float start;");
            declared.add("    float end;");
            declared.add("    float scale;");
            declared.add("};");
            declared.add("uniform flagon_FogParameters u_fog;");
            out = GlslTranslator.replaceWord(out, "gl_Fog", "u_fog");
        }
        return String.join("\n", declared) + (declared.isEmpty() ? "" : "\n") + out;
    }

    private static String rewriteVertex(String code, String body, List<String> declared) {
        String out = body;
        boolean position = uses(code, "gl_Vertex") || uses(code, "ftransform");
        boolean modelView = uses(code, "gl_ModelViewMatrix")
                || uses(code, "gl_ModelViewProjectionMatrix") || uses(code, "ftransform");
        boolean projection = uses(code, "gl_ProjectionMatrix")
                || uses(code, "gl_ModelViewProjectionMatrix") || uses(code, "ftransform");

        if (position) {
            declared.add("layout(location = " + VertexArrays.POSITION + ") in vec3 in_position;");
        }
        if (uses(code, "gl_Color")) {
            declared.add("layout(location = " + VertexArrays.COLOR + ") in vec4 in_color;");
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (uses(code, "gl_MultiTexCoord" + unit)) {
                declared.add("layout(location = " + (VertexArrays.TEX_COORD + unit)
                        + ") in vec2 in_texCoord" + unit + ";");
            }
        }
        if (uses(code, "gl_Normal")) {
            declared.add("layout(location = " + VertexArrays.NORMAL + ") in vec3 in_normal;");
        }
        if (modelView) {
            declared.add("uniform mat4 u_modelView;");
        }
        if (projection) {
            declared.add("uniform mat4 u_projection;");
        }
        if (uses(code, "gl_NormalMatrix")) {
            declared.add("uniform mat3 u_normalMatrix;");
        }
        if (uses(code, "gl_FrontColor") || uses(code, "gl_BackColor")) {
            declared.add("out vec4 flagon_frontColor;");
        }
        if (uses(code, "gl_TexCoord")) {
            declared.add("out vec4 flagon_texCoord[" + VertexArrays.UNITS + "];");
        }
        if (uses(code, "gl_FogFragCoord")) {
            declared.add("out float flagon_fogFragCoord;");
        }
        if (uses(code, "ftransform")) {
            declared.add("vec4 flagon_ftransform() {");
            declared.add("    return u_projection * u_modelView * vec4(in_position, 1.0);");
            declared.add("}");
            out = GlslTranslator.replaceWord(out, "ftransform", "flagon_ftransform");
        }

        out = GlslTranslator.replaceWord(out, "gl_ModelViewProjectionMatrix",
                "(u_projection * u_modelView)");
        out = GlslTranslator.replaceWord(out, "gl_ModelViewMatrix", "u_modelView");
        out = GlslTranslator.replaceWord(out, "gl_ProjectionMatrix", "u_projection");
        out = GlslTranslator.replaceWord(out, "gl_NormalMatrix", "u_normalMatrix");
        out = GlslTranslator.replaceWord(out, "gl_Vertex", "vec4(in_position, 1.0)");
        out = GlslTranslator.replaceWord(out, "gl_Color", "in_color");
        out = GlslTranslator.replaceWord(out, "gl_Normal", "in_normal");
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            out = GlslTranslator.replaceWord(out, "gl_MultiTexCoord" + unit,
                    "vec4(in_texCoord" + unit + ", 0.0, 1.0)");
        }
        out = GlslTranslator.replaceWord(out, "gl_FrontColor", "flagon_frontColor");
        out = GlslTranslator.replaceWord(out, "gl_BackColor", "flagon_frontColor");
        out = GlslTranslator.replaceWord(out, "gl_TexCoord", "flagon_texCoord");
        out = GlslTranslator.replaceWord(out, "gl_FogFragCoord", "flagon_fogFragCoord");
        return out;
    }

    private static String rewriteFragment(String code, String body, List<String> declared) {
        String out = body;
        if (uses(code, "gl_Color")) {
            declared.add("in vec4 flagon_frontColor;");
            out = GlslTranslator.replaceWord(out, "gl_Color", "flagon_frontColor");
        }
        if (uses(code, "gl_TexCoord")) {
            declared.add("in vec4 flagon_texCoord[" + VertexArrays.UNITS + "];");
            out = GlslTranslator.replaceWord(out, "gl_TexCoord", "flagon_texCoord");
        }
        if (uses(code, "gl_FogFragCoord")) {
            declared.add("in float flagon_fogFragCoord;");
            out = GlslTranslator.replaceWord(out, "gl_FogFragCoord", "flagon_fogFragCoord");
        }
        return out;
    }

    private static boolean uses(String code, String word) {
        return GlslTranslator.indexOfWord(code, word, 0) >= 0;
    }
}
