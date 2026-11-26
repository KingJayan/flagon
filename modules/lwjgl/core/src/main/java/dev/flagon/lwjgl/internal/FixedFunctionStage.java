package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.ShaderStage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * gl links a program from whichever stages it was given and leaves the rest fixed-function.
 * webgl needs both, so the stage the application left out is written here over the same
 * varyings {@link FixedFunctionGlsl} names.
 */
public final class FixedFunctionStage {

    static final String FRONT_COLOR = "flagon_frontColor";
    static final String TEX_COORD = "flagon_texCoord";
    static final String FOG_FRAG_COORD = "flagon_fogFragCoord";

    /** what a stage written here can hand the application's stage */
    private static final Set<String> FEEDABLE =
            Set.of(FRONT_COLOR, TEX_COORD, FOG_FRAG_COORD);

    private static final Pattern DECLARATION = Pattern.compile(
            "(?m)^\\s*(in|out)\\s+\\w+\\s+(\\w+)\\s*(\\[[^\\]]*\\])?\\s*;");

    private FixedFunctionStage() {
    }

    /**
     * @param source the stage the application supplied, already translated to glsl es
     * @param supplied which stage that is
     * @return the other stage, reproducing gl's fixed-function behaviour over the same varyings
     */
    public static String companionFor(String source, ShaderStage supplied) {
        String code = GlslTranslator.withoutComments(source);
        return supplied == ShaderStage.VERTEX
                ? fragmentFor(declarations(code, "out"))
                : vertexFor(readBy(declarations(code, "in")));
    }

    private static Set<String> declarations(String code, String direction) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = DECLARATION.matcher(code);
        while (matcher.find()) {
            if (direction.equals(matcher.group(1))) {
                found.add(matcher.group(2));
            }
        }
        return found;
    }

    /**
     * a vertex output nothing reads is allowed, but a fragment input nothing writes is a link
     * error, so the stage written for a fragment shader has to cover every input it declares.
     */
    private static Set<String> readBy(Set<String> inputs) {
        List<String> strangers = new ArrayList<>();
        for (String input : inputs) {
            if (!FEEDABLE.contains(input)) {
                strangers.add(input);
            }
        }
        if (!strangers.isEmpty()) {
            throw new UnsupportedOperationException(
                    "the program has no vertex shader, so gl's fixed-function one runs, and it "
                            + "cannot write " + String.join(", ", strangers)
                            + "; supply a vertex shader that does, or read gl_Color and "
                            + "gl_TexCoord instead");
        }
        return inputs;
    }

    private static String fragmentFor(Set<String> written) {
        List<String> lines = new ArrayList<>();
        lines.add("#version 300 es");
        lines.add("precision highp float;");
        boolean color = written.contains(FRONT_COLOR);
        boolean texture = written.contains(TEX_COORD);
        boolean fog = written.contains(FOG_FRAG_COORD);

        if (color) {
            lines.add("in vec4 " + FRONT_COLOR + ";");
        }
        if (texture) {
            lines.add("in vec4 " + TEX_COORD + "[" + VertexArrays.UNITS + "];");
        }
        if (fog) {
            lines.add("in float " + FOG_FRAG_COORD + ";");
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            lines.add("uniform sampler2D u_texture" + unit + ";");
        }
        lines.add("uniform bool u_textureEnabled[" + VertexArrays.UNITS + "];");
        lines.add("uniform int u_textureEnv[" + VertexArrays.UNITS + "];");
        lines.add("uniform bool u_alphaTestEnabled;");
        lines.add("uniform float u_alphaTestThreshold;");
        if (fog) {
            lines.add("uniform bool u_fogEnabled;");
            lines.add("uniform int u_fogMode;");
            lines.add("uniform vec4 u_fogColor;");
            lines.add("uniform float u_fogDensity;");
            lines.add("uniform float u_fogStart;");
            lines.add("uniform float u_fogEnd;");
        }
        lines.add("out vec4 out_color;");
        lines.addAll(COMBINE);
        if (fog) {
            lines.addAll(FOG_FACTOR);
        }

        lines.add("void main() {");
        lines.add("    vec4 color = " + (color ? FRONT_COLOR : "vec4(1.0)") + ";");
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                lines.add("    if (u_textureEnabled[" + unit + "]) {");
                lines.add("        color = combine(color, texture(u_texture" + unit + ", "
                        + TEX_COORD + "[" + unit + "].st), u_textureEnv[" + unit + "]);");
                lines.add("    }");
            }
        }
        lines.add("    if (u_alphaTestEnabled && color.a <= u_alphaTestThreshold) {");
        lines.add("        discard;");
        lines.add("    }");
        if (fog) {
            lines.add("    if (u_fogEnabled) {");
            lines.add("        float blend = clamp(fogFactor(" + FOG_FRAG_COORD + "), 0.0, 1.0);");
            lines.add("        color = vec4(mix(u_fogColor.rgb, color.rgb, blend), color.a);");
            lines.add("    }");
        }
        lines.add("    out_color = color;");
        lines.add("}");
        return String.join("\n", lines) + "\n";
    }

    private static String vertexFor(Set<String> read) {
        List<String> lines = new ArrayList<>();
        lines.add("#version 300 es");
        lines.add("precision highp float;");
        boolean color = read.contains(FRONT_COLOR);
        boolean texture = read.contains(TEX_COORD);
        boolean fog = read.contains(FOG_FRAG_COORD);

        lines.add("layout(location = " + VertexArrays.POSITION + ") in vec3 in_position;");
        if (color) {
            lines.add("layout(location = " + VertexArrays.COLOR + ") in vec4 in_color;");
            lines.add("out vec4 " + FRONT_COLOR + ";");
        }
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                lines.add("layout(location = " + (VertexArrays.TEX_COORD + unit)
                        + ") in vec2 in_texCoord" + unit + ";");
            }
            lines.add("out vec4 " + TEX_COORD + "[" + VertexArrays.UNITS + "];");
        }
        if (fog) {
            lines.add("out float " + FOG_FRAG_COORD + ";");
        }
        lines.add("uniform mat4 u_modelView;");
        lines.add("uniform mat4 u_projection;");

        lines.add("void main() {");
        if (color) {
            lines.add("    " + FRONT_COLOR + " = in_color;");
        }
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                lines.add("    " + TEX_COORD + "[" + unit + "] = vec4(in_texCoord" + unit
                        + ", 0.0, 1.0);");
            }
        }
        lines.add("    vec4 eye = u_modelView * vec4(in_position, 1.0);");
        if (fog) {
            lines.add("    " + FOG_FRAG_COORD + " = length(eye.xyz);");
        }
        lines.add("    gl_Position = u_projection * eye;");
        lines.add("}");
        return String.join("\n", lines) + "\n";
    }

    private static final List<String> COMBINE = List.of(
            "vec4 combine(vec4 incoming, vec4 sampled, int mode) {",
            "    if (mode == 1) {",
            "        return sampled;",
            "    }",
            "    if (mode == 2) {",
            "        return vec4(incoming.rgb + sampled.rgb, incoming.a * sampled.a);",
            "    }",
            "    return incoming * sampled;",
            "}");

    private static final List<String> FOG_FACTOR = List.of(
            "float fogFactor(float distance) {",
            "    if (u_fogMode == 0) {",
            "        return (u_fogEnd - distance) / max(u_fogEnd - u_fogStart, 0.0001);",
            "    }",
            "    if (u_fogMode == 2) {",
            "        float d = u_fogDensity * distance;",
            "        return exp(-d * d);",
            "    }",
            "    return exp(-u_fogDensity * distance);",
            "}");
}
