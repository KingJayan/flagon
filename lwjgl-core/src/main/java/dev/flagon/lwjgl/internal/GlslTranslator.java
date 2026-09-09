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
        String code = withoutComments(source);
        rejectFixedFunction(code);

        int directive = versionDirective(code);
        if (directive < 0) {
            return withPrecision(source, code, stage);
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
        return withPrecision(stripped, code.substring(0, directive) + code.substring(end), stage);
    }

    /** blanks comments out, keeping every index and line break, so scans line up with the source */
    private static String withoutComments(String source) {
        char[] code = source.toCharArray();
        int at = 0;
        while (at + 1 < code.length) {
            if (code[at] != '/') {
                at++;
            } else if (code[at + 1] == '/') {
                at = blankLineComment(code, at);
            } else if (code[at + 1] == '*') {
                at = blankBlockComment(code, at);
            } else {
                at++;
            }
        }
        return new String(code);
    }

    private static int blankLineComment(char[] code, int from) {
        int at = from;
        while (at < code.length && !(code[at] == '\n' && !continued(code, at))) {
            at++;
        }
        blank(code, from, at);
        return at;
    }

    private static boolean continued(char[] code, int newline) {
        int at = newline - 1;
        while (at >= 0 && code[at] == '\r') {
            at--;
        }
        return at >= 0 && code[at] == '\\';
    }

    private static int blankBlockComment(char[] code, int from) {
        int at = from + 2;
        while (at + 1 < code.length && !(code[at] == '*' && code[at + 1] == '/')) {
            at++;
        }
        int end = at + 1 < code.length ? at + 2 : code.length;
        blank(code, from, end);
        return end;
    }

    private static void blank(char[] code, int from, int to) {
        for (int at = from; at < to; at++) {
            if (code[at] != '\n') {
                code[at] = ' ';
            }
        }
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
     * es requires a fragment shader to state a float precision & desktop glsl doesnt
     */
    private static String withPrecision(String source, String code, ShaderStage stage) {
        if (stage != ShaderStage.FRAGMENT || declaresFloatPrecision(code)) {
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
