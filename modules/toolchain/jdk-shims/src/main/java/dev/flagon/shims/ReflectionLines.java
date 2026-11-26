package dev.flagon.shims;

import org.teavm.asm.Opcodes;
import org.teavm.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ReflectionLines {

    private ReflectionLines() {
    }

    static String write(ClassFacts facts, boolean full, boolean serializable, Set<String> classes) {
        List<String> lines = new ArrayList<>();
        if (full) {
            if (facts.signature != null) {
                lines.add("S " + facts.signature);
                new Names(facts.signature, classes).classSignature();
            }
            if (facts.enclosingOwner != null) {
                lines.add("E " + facts.enclosingOwner + " " + orDash(facts.enclosingMethod) + " "
                        + orDash(facts.enclosingDescriptor));
                classes.add(facts.enclosingOwner);
            }
            for (String subclass : facts.permitted) {
                lines.add("P " + subclass);
                classes.add(subclass);
            }
            for (ClassFacts.Parameters parameters : facts.parameters) {
                lines.add("A " + parameters.method() + " " + String.join(",",
                        parameters.names().stream().map(ReflectionLines::encode).toList()));
            }
            if ((facts.access & Opcodes.ACC_INTERFACE) != 0) {
                for (ClassFacts.Member method : facts.methods) {
                    int flags = method.access();
                    if ((flags & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC
                            | Opcodes.ACC_PRIVATE)) == 0) {
                        lines.add("B " + method.name() + method.descriptor());
                    }
                }
            }
            for (ClassFacts.Default value : facts.defaults) {
                lines.add("D " + value.method() + " " + value.value());
                new Names(value.value(), classes).literals();
            }
        }
        if (full || serializable) {
            for (ClassFacts.Component component : facts.components) {
                lines.add("R " + component.name() + " " + component.descriptor() + " "
                        + orDash(component.signature()));
                new Names(component.descriptor(), classes).type();
                if (component.signature() != null) {
                    new Names(component.signature(), classes).type();
                }
            }
        }
        boolean enumerated = (facts.access & Opcodes.ACC_ENUM) != 0;
        if (serializable && !facts.record && !enumerated && !facts.declares("serialVersionUID")) {
            lines.add("U " + facts.defaultSerialId());
        }
        return String.join("\n", lines);
    }

    static String literal(Object value, Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN -> "z" + (Boolean.TRUE.equals(value)
                    || Integer.valueOf(1).equals(value) ? 1 : 0);
            case Type.BYTE -> "b" + ((Number) value).byteValue();
            case Type.CHAR -> "c" + (int) (value instanceof Character character ? character
                    : (char) ((Number) value).intValue());
            case Type.SHORT -> "h" + ((Number) value).shortValue();
            case Type.INT -> "i" + ((Number) value).intValue();
            case Type.LONG -> "j" + ((Number) value).longValue();
            case Type.FLOAT -> "f" + Float.floatToRawIntBits(((Number) value).floatValue());
            case Type.DOUBLE -> "d" + Double.doubleToRawLongBits(((Number) value).doubleValue());
            default -> value instanceof Type constant ? "t" + constant.getDescriptor()
                    : "s" + encode(String.valueOf(value));
        };
    }

    /** text with no space, comma, bracket, percent or line break left in it */
    static String encode(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            if (unit <= ' ' || unit == ',' || unit == '[' || unit == ']' || unit == '%'
                    || unit >= 0x7F) {
                out.append('%').append(String.format("%04x", (int) unit));
            } else {
                out.append(unit);
            }
        }
        return out.toString();
    }

    private static String orDash(String text) {
        return text == null ? "-" : text;
    }

    /** reads the class names out of a signature, a descriptor, or a written default */
    static final class Names {

        private final String text;
        private final Set<String> into;
        private int at;

        Names(String text, Set<String> into) {
            this.text = text;
            this.into = into;
        }

        void classSignature() {
            if (peek() == '<') {
                formals();
            }
            while (at < text.length()) {
                type();
            }
        }

        void literals() {
            for (String part : text.split("[\\[\\],]")) {
                if (part.startsWith("t") || part.startsWith("e")) {
                    String descriptor = part.substring(1);
                    int dot = part.startsWith("e") ? descriptor.indexOf(';') + 1
                            : descriptor.length();
                    new Names(descriptor.substring(0, dot), into).type();
                }
            }
        }

        void type() {
            char kind = peek();
            if (kind == 'L') {
                classType();
            } else if (kind == 'T') {
                at = text.indexOf(';', at) + 1;
            } else if (kind == '[') {
                at++;
                type();
            } else {
                at++;
            }
        }

        private void formals() {
            at++;
            while (peek() != '>') {
                at = text.indexOf(':', at);
                while (peek() == ':') {
                    at++;
                    if (peek() == 'L' || peek() == 'T' || peek() == '[') {
                        type();
                    }
                }
            }
            at++;
        }

        private void classType() {
            at++;
            StringBuilder name = new StringBuilder();
            while (true) {
                char next = peek();
                if (next == '<') {
                    arguments();
                } else if (next == '.') {
                    at++;
                    name.append('$');
                } else if (next == ';') {
                    at++;
                    break;
                } else {
                    name.append(next);
                    at++;
                }
            }
            into.add(name.toString());
        }

        private void arguments() {
            at++;
            while (peek() != '>') {
                char next = peek();
                if (next == '*') {
                    at++;
                } else {
                    if (next == '+' || next == '-') {
                        at++;
                    }
                    type();
                }
            }
            at++;
        }

        private char peek() {
            return text.charAt(at);
        }
    }
}
