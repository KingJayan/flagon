package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.ShaderStage;

/**
 * desktop glsl in, glsl es out
 */
public final class GlslTranslator {

    private static final int LOWEST_CORE = 130;
    private static final int HIGHEST_CORE = 330;

    private static final String FRAGMENT_OUT = "flagon_FragColor";

    private static final String[][] SAMPLER_CALLS = {
            {"texture2DProjLod", "textureProjLod"},
            {"texture2DLodEXT", "textureLod"},
            {"texture2DProj", "textureProj"},
            {"texture2DLod", "textureLod"},
            {"textureCubeLod", "textureLod"},
            {"texture3DLod", "textureLod"},
            {"texture3DProj", "textureProj"},
            {"shadow2DProj", "textureProj"},
            {"texture2D", "texture"},
            {"textureCube", "texture"},
            {"texture3D", "texture"},
            {"shadow2D", "texture"},
    };

    private GlslTranslator() {
    }

    /** words es 3.00 reserves that desktop GLSL up to 3.30 lets a shader name things */
    private static final String[] RESERVED = {
        "sample", "patch", "subroutine", "active", "common", "partition", "filter", "resource",
        "input", "output", "coherent", "readonly", "writeonly",
    };

    /** keywords from GLSL 1.30 on, which a 1.10 or 1.20 shader may still use as names */
    private static final String[] LATER_KEYWORDS = {"smooth", "flat", "layout", "uint"};

    private static String toGlslEs300(String source, int directive, int end, ShaderStage stage) {
        String stripped = renamed(source.substring(0, directive) + source.substring(end),
                RESERVED);
        return finishGlslEs300(stripped, stage);
    }

    /** es 1.00 limits loops and indexing to constants, so 1.10 and 1.20 go to es 3.00 too */
    private static String fromLegacy(String stripped, ShaderStage stage) {
        String named = renamed(renamed(stripped, RESERVED), LATER_KEYWORDS);
        return finishGlslEs300(asInAndOut(named, stage), stage);
    }

    private static String renamed(String source, String[] words) {
        String renamed = source;
        for (String word : words) {
            renamed = replaceWord(renamed, word, "flagon_" + word);
        }
        return renamed;
    }

    private static String finishGlslEs300(String stripped, ShaderStage stage) {
        String renamed = renameSamplerCalls(stripped);
        String body = ImplicitFloats.rewrite(renamed, withoutComments(renamed));
        String bare = withoutComments(body);
        StringBuilder injected = new StringBuilder();
        boolean fragment = stage == ShaderStage.FRAGMENT;
        if (fragment) {
            if (!declaresFloatPrecision(bare)) {
                injected.append("precision highp float;\n");
            }
            injected.append(fragmentOutput(bare));
        }

        int after = afterExtensions(body, bare);
        String assembled = "#version 300 es\n" + body.substring(0, after) + injected
                + body.substring(after);

        return fragment ? replaceWord(assembled, "gl_FragColor", FRAGMENT_OUT) : assembled;
    }

    private static int afterExtensions(String body, String bare) {
        int after = 0;
        int at = 0;
        while ((at = bare.indexOf("#extension", at)) >= 0) {
            int end = lineEnd(body, at);
            after = end < body.length() ? end + 1 : end;
            at = end;
        }
        return after;
    }

    private static String renameSamplerCalls(String source) {
        String renamed = source;
        for (String[] call : SAMPLER_CALLS) {
            renamed = replaceWord(renamed, call[0], call[1]);
        }
        return renamed;
    }

    private static String fragmentOutput(String code) {
        if (indexOfWord(code, "gl_FragData", 0) >= 0) {
            throw new UnsupportedOperationException(
                    "shader writes gl_FragData, which es 3.00 dropped and webgl2 draws to one "
                            + "colour buffer here. declare an out variable instead");
        }
        if (indexOfWord(code, "gl_FragColor", 0) < 0) {
            return "";
        }
        return "out vec4 " + FRAGMENT_OUT + ";\n";
    }

    static String replaceWord(String source, String word, String replacement) {
        int at = indexOfWord(source, word, 0);
        if (at < 0) {
            return source;
        }
        StringBuilder rewritten = new StringBuilder(source.length());
        int copied = 0;
        while (at >= 0) {
            rewritten.append(source, copied, at).append(replacement);
            copied = at + word.length();
            at = indexOfWord(source, word, copied);
        }
        return rewritten.append(source, copied, source.length()).toString();
    }

    public static boolean readsFixedFunction(String source) {
        return source != null && FixedFunctionGlsl.isUsedIn(withoutComments(source));
    }

    public static String toGlslEs(String source, ShaderStage stage) {
        if (source == null) {
            throw new IllegalArgumentException("shader source must not be null");
        }
        String code = withoutComments(source);
        FixedFunctionGlsl.rejectWhatCannotBeFed(code);

        int directive = versionDirective(code);
        if (directive < 0) {
            return FixedFunctionGlsl.isUsedIn(code)
                    ? toFixedFunction(source, stage)
                    : withPrecision(source, code, stage);
        }

        int end = lineEnd(source, directive);
        String version = source.substring(directive + "#version".length(), end).trim();
        if (isGlslEs(version)) {
            return source;
        }
        // the directive comes first, so dropping initializers after it leaves its offsets alone
        source = UniformDefaults.strip(source, code);
        if (FixedFunctionGlsl.isUsedIn(code)) {
            return toFixedFunction(source.substring(0, directive) + source.substring(end), stage);
        }

        int number = versionNumber(version);
        if (number > HIGHEST_CORE) {
            throw new UnsupportedOperationException(
                    "shader asks for desktop GLSL " + version + "; webgl2 compiles GLSL ES 1.00 "
                            + "and 3.00 only. GLSL 1.10 to 3.30 are translated, so port the shader "
                            + "to '#version 300 es' or write it against one of those");
        }
        if (number >= LOWEST_CORE) {
            return toGlslEs300(source, directive, end, stage);
        }

        return fromLegacy(source.substring(0, directive) + source.substring(end), stage);
    }

    public static String withoutComments(String source) {
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

    private static String withPrecision(String source, String code, ShaderStage stage) {
        if (stage != ShaderStage.FRAGMENT || declaresFloatPrecision(code)) {
            return source;
        }
        int after = afterExtensions(source, code);
        return source.substring(0, after) + "precision mediump float;\n" + source.substring(after);
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

    private static String toFixedFunction(String stripped, ShaderStage stage) {
        String modern = asInAndOut(stripped, stage);
        return finishGlslEs300(FixedFunctionGlsl.rewrite(modern, stage), stage);
    }

    private static String asInAndOut(String source, ShaderStage stage) {
        String rewritten = replaceWord(source, "attribute", "in");
        return replaceWord(rewritten, "varying", stage == ShaderStage.VERTEX ? "out" : "in");
    }

    private static int versionDirective(String source) {
        int at = indexOfWord(source, "#version", 0);
        return at;
    }

    static int indexOfWord(String source, String word, int from) {
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
