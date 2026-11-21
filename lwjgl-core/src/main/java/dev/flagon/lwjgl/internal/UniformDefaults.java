package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * desktop GLSL from 1.20 gives a uniform its starting value in the source, and es drops that. the
 * initializer comes out of the source, and the value is set once the program links.
 */
public final class UniformDefaults {

    /** one uniform's starting value; an int or bool uniform is set as ints */
    public record Default(String name, boolean integer, float[] values) {
    }

    private static final Pattern DECLARATION = Pattern.compile(
            "\\buniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(float|int|bool|vec[234]|ivec[234]"
                    + "|bvec[234])\\s+(\\w+)\\s*=\\s*([^;]+);");
    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?[fF]?|true|false");

    private UniformDefaults() {
    }

    /** @param code the source with its comments blanked */
    public static List<Default> of(String code) {
        List<Default> found = new ArrayList<>();
        Matcher declared = DECLARATION.matcher(code);
        while (declared.find()) {
            Default made = parse(declared.group(1), declared.group(2), declared.group(3).trim());
            if (made != null) {
                found.add(made);
            }
        }
        return found;
    }

    /** the source without the initializers {@link #of} reads; others stay, and fail to compile */
    public static String strip(String source, String code) {
        StringBuilder stripped = new StringBuilder();
        Matcher declared = DECLARATION.matcher(code);
        int copied = 0;
        while (declared.find()) {
            if (parse(declared.group(1), declared.group(2), declared.group(3).trim()) == null) {
                continue;
            }
            int name = declared.end(2);
            stripped.append(source, copied, name).append(';');
            copied = declared.end();
        }
        return stripped.append(source, copied, source.length()).toString();
    }

    private static Default parse(String type, String name, String value) {
        int size = type.length() > 3 && Character.isDigit(type.charAt(type.length() - 1))
                ? type.charAt(type.length() - 1) - '0' : 1;
        boolean integer = type.startsWith("i") || type.startsWith("b");
        String[] parts;
        if (size == 1) {
            parts = new String[] {value};
        } else {
            if (!value.startsWith(type + "(") || !value.endsWith(")")) {
                return null;
            }
            parts = value.substring(type.length() + 1, value.length() - 1).split(",");
        }
        if (parts.length != 1 && parts.length != size) {
            return null;
        }
        float[] values = new float[size];
        for (int at = 0; at < size; at++) {
            String part = parts[parts.length == 1 ? 0 : at].trim();
            if (!NUMBER.matcher(part).matches()) {
                return null;
            }
            values[at] = part.equals("true") ? 1 : part.equals("false") ? 0
                    : Float.parseFloat(part.replaceAll("[fF]$", ""));
        }
        return new Default(name, integer, values);
    }
}
