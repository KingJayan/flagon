package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.ShaderStage;

/**
 * desktop glsl in, glsl es out
 */
public final class GlslTranslator {

    /** compatibility-profile built-ins */
    private static final String[] FIXED_FUNCTION_BUILTINS = {
            "gl_ModelViewMatrix",
            "gl_ProjectionMatrix",
            "gl_ModelViewProjectionMatrix",
            "gl_NormalMatrix",
            "gl_TextureMatrix",
            "gl_Vertex",
            "gl_Color",
            "gl_SecondaryColor",
            "gl_Normal",
            "gl_MultiTexCoord0",
            "gl_MultiTexCoord1",
            "gl_MultiTexCoord2",
            "gl_MultiTexCoord3",
            "gl_TexCoord",
            "gl_FrontColor",
            "gl_BackColor",
            "gl_FogFragCoord",
            "gl_LightSource",
            "gl_Fog",
            "ftransform",
    };

    private GlslTranslator() {
    }

    public static String toGlslEs(String source, ShaderStage stage) {
        if (source == null) {
            throw new IllegalArgumentException("shader source must not be null");
        }
        rejectFixedFunction(source);

        int directive = versionDirective(source);
        if (directive < 0) {
            return withPrecision(source, stage);
        }

        int end = lineEnd(source, directive);
        String version = source.substring(directive + "#version".length(), end).trim();
        if (isGlslEs(version)) {
            return source;
        }

        int number = versionNumber(version);
        if (number > 120) {
            throw new UnsupportedOperationException(
                    "shader asks for desktop GLSL " + version + "; webgl2 compiles GLSL ES 1.00 "
                            + "and 3.00 only. GLSL 1.10 and 1.20 are accepted as ES 1.00, so port "
                            + "the shader to '#version 300 es' or write it against 1.20");
        }
        // 1.10 and 1.20 are read as es 1.00, which is what an absent directive already means
        String stripped = source.substring(0, directive) + source.substring(end);
        return withPrecision(stripped, stage);
    }

    private static boolean isGlslEs(String version) {
        return version.endsWith("es") || version.equals("100");
    }

    private static int versionNumber(String version) {
        int digits = 0;
        while (digits < version.length() && Character.isDigit(version.charAt(digits))) {
            digits++;
        }
        if (digits == 0) {
            throw new IllegalArgumentException("unreadable #version directive: " + version);
        }
        return Integer.parseInt(version.substring(0, digits));
    }

    /**
     * es requires a fragment shader to state a float precision; desktop glsl doesnt
     */
    private static String withPrecision(String source, ShaderStage stage) {
        if (stage != ShaderStage.FRAGMENT || declaresFloatPrecision(source)) {
            return source;
        }
        return "precision mediump float;\n" + source;
    }

    private static boolean declaresFloatPrecision(String source) {
        int at = 0;
        while ((at = indexOfWord(source, "precision", at)) >= 0) {
            int end = lineEnd(source, at);
            if (indexOfWord(source.substring(at, end), "float", 0) >= 0) {
                return true;
            }
            at = end;
        }
        return false;
    }

    private static void rejectFixedFunction(String source) {
        for (String builtin : FIXED_FUNCTION_BUILTINS) {
            if (indexOfWord(source, builtin, 0) >= 0) {
                throw new UnsupportedOperationException(
                        "shader uses '" + builtin + "', which belongs to gl's fixed-function state "
                                + "and does not exist in GLSL ES. pass the matrices and vertex "
                                + "attributes in as uniforms and attributes of your own");
            }
        }
    }

    private static int versionDirective(String source) {
        int at = indexOfWord(source, "#version", 0);
        return at;
    }

    /** @return the index of {@code word} where it is not part of a longer identifier */
    private static int indexOfWord(String source, String word, int from) {
        int at = source.indexOf(word, from);
        while (at >= 0) {
            boolean beforeOk = at == 0 || !isIdentifier(source.charAt(at - 1));
            int after = at + word.length();
            boolean afterOk = after >= source.length() || !isIdentifier(source.charAt(after));
            if (beforeOk && afterOk) {
                return at;
            }
            at = source.indexOf(word, at + 1);
        }
        return -1;
    }

    private static boolean isIdentifier(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private static int lineEnd(String source, int from) {
        int newline = source.indexOf('\n', from);
        return newline < 0 ? source.length() : newline;
    }
}
