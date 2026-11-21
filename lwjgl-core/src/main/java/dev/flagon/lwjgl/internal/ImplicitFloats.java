package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * desktop GLSL from 1.20 turns an int into a float where a float is wanted, and es refuses. an int
 * literal becomes a float literal where what it meets is known to be a float; where that is not
 * known, the literal stays as it is.
 */
final class ImplicitFloats {

    private enum Type { FLOAT, INT, UNKNOWN }

    private record Token(String text, int start) {
    }

    private record Function(Type returns, List<Type> parameters, int bodyStart, int bodyEnd) {
    }

    private static final Set<String> BINARY = Set.of("+", "-", "*", "/", "<", ">", "<=", ">=",
            "==", "!=");
    private static final Set<String> ASSIGNMENT = Set.of("=", "+=", "-=", "*=", "/=");
    private static final Set<String> KEYWORDS = Set.of("return", "else", "case", "if", "while",
            "for", "do", "switch");
    /** builtins es declares for floats alone */
    private static final Set<String> FLOAT_ONLY = Set.of("radians", "degrees", "sin", "cos", "tan",
            "asin", "acos", "atan", "sinh", "cosh", "tanh", "pow", "exp", "log", "exp2", "log2",
            "sqrt", "inversesqrt", "floor", "ceil", "trunc", "round", "fract", "mod", "step",
            "smoothstep", "length", "distance", "dot", "cross", "normalize", "reflect", "refract",
            "faceforward");
    /** builtins es also declares for ints or bools, so they take the type of their arguments */
    private static final Set<String> GENERIC = Set.of("abs", "sign", "min", "max", "clamp", "mix");
    private static final Function OVERLOADED = new Function(Type.UNKNOWN, List.of(), -1, -1);

    private ImplicitFloats() {
    }

    /** @param code the source with its comments blanked */
    static String rewrite(String source, String code) {
        List<Token> tokens = tokens(code);
        Map<String, Type> variables = new HashMap<>();
        Map<String, Function> functions = new HashMap<>();
        List<Function> bodies = new ArrayList<>();
        declarations(tokens, variables, functions, bodies);

        StringBuilder rewritten = new StringBuilder(source.length() + 16);
        int copied = 0;
        for (int at = 0; at < tokens.size(); at++) {
            if (isIntLiteral(tokens.get(at).text())
                    && wantsFloat(tokens, at, variables, functions, bodies)) {
                int end = tokens.get(at).start() + tokens.get(at).text().length();
                rewritten.append(source, copied, end).append(".0");
                copied = end;
            }
        }
        return rewritten.append(source, copied, source.length()).toString();
    }

    private static boolean wantsFloat(List<Token> tokens, int at, Map<String, Type> variables,
            Map<String, Function> functions, List<Function> bodies) {
        Typing typing = new Typing(tokens, variables, functions);
        int before = at - 1;
        if (before >= 0 && isSign(text(tokens, before)) && !endsOperand(tokens, before - 1)) {
            before--;
        }
        Type left = BINARY.contains(text(tokens, before)) && endsOperand(tokens, before - 1)
                ? typing.of(typing.primaryEndingAt(before - 1), before) : null;
        Type right = BINARY.contains(text(tokens, at + 1))
                ? typing.of(at + 2, typing.primaryAfter(at + 2)) : null;
        if (left == Type.INT || right == Type.INT) {
            return false;
        }
        if (left == Type.FLOAT || right == Type.FLOAT) {
            return true;
        }
        if (left != null || right != null) {
            return false;
        }

        String previous = text(tokens, before);
        String next = text(tokens, at + 1);
        boolean whole = next.equals(";") || next.equals(",") || next.equals(")");
        if (!whole) {
            return false;
        }
        if (ASSIGNMENT.contains(previous)) {
            return typing.of(typing.primaryEndingAt(before - 1), before) == Type.FLOAT;
        }
        if (previous.equals("return")) {
            return returnsFloat(bodies, tokens.get(at).start());
        }
        if (previous.equals("(") || previous.equals(",")) {
            return argumentWantsFloat(tokens, before, at, typing, functions);
        }
        return false;
    }

    private static boolean argumentWantsFloat(List<Token> tokens, int before, int at,
            Typing typing, Map<String, Function> functions) {
        int open = before;
        int depth = 0;
        while (open >= 0) {
            String text = text(tokens, open);
            if (text.equals(")")) {
                depth++;
            } else if (text.equals("(") && depth-- == 0) {
                break;
            }
            open--;
        }
        if (open < 1 || !isIdentifier(text(tokens, open - 1))) {
            return false;
        }
        String callee = text(tokens, open - 1);
        List<int[]> arguments = typing.arguments(open);
        int index = 0;
        while (index < arguments.size() && arguments.get(index)[0] != at) {
            index++;
        }
        if (FLOAT_ONLY.contains(callee)) {
            return true;
        }
        if (GENERIC.contains(callee)) {
            for (int other = 0; other < arguments.size(); other++) {
                if (other != index && typing.of(arguments.get(other)[0],
                        arguments.get(other)[1]) == Type.FLOAT) {
                    return true;
                }
            }
            return false;
        }
        Function function = functions.get(callee);
        return function != null && index < function.parameters().size()
                && function.parameters().get(index) == Type.FLOAT;
    }

    private static boolean returnsFloat(List<Function> bodies, int position) {
        for (Function function : bodies) {
            if (position > function.bodyStart() && position < function.bodyEnd()) {
                return function.returns() == Type.FLOAT;
            }
        }
        return false;
    }

    /** the types every name is declared with; a name declared with two types stays unknown */
    private static void declarations(List<Token> tokens, Map<String, Type> variables,
            Map<String, Function> functions, List<Function> bodies) {
        for (int at = 0; at + 1 < tokens.size(); at++) {
            Type type = typeNamed(text(tokens, at));
            if (type == null || !isIdentifier(text(tokens, at + 1))
                    || KEYWORDS.contains(text(tokens, at + 1))) {
                continue;
            }
            String name = text(tokens, at + 1);
            if (text(tokens, at + 2).equals("(")) {
                int close = matching(tokens, at + 2);
                List<Type> parameters = new ArrayList<>();
                for (int[] parameter : new Typing(tokens, variables, functions)
                        .arguments(at + 2)) {
                    Type declared = Type.UNKNOWN;
                    for (int word = parameter[0]; word < parameter[1]; word++) {
                        Type named = typeNamed(text(tokens, word));
                        if (named != null) {
                            declared = named;
                        }
                    }
                    parameters.add(declared);
                }
                if (text(tokens, close + 1).equals("{")) {
                    int end = Math.min(matching(tokens, close + 1), tokens.size() - 1);
                    bodies.add(new Function(type, parameters, tokens.get(close).start(),
                            tokens.get(end).start()));
                }
                Function made = new Function(type, parameters, -1, -1);
                // overloads leave the name's types unknown
                functions.merge(name, made, (one, other) -> one.equals(other) ? one : OVERLOADED);
                continue;
            }
            declare(variables, name, type);
            for (int next = at + 2; next + 1 < tokens.size(); next++) {
                String text = text(tokens, next);
                if (text.equals(";") || text.equals(")") || text.equals("{")) {
                    break;
                }
                if (text.equals("(") || text.equals("[")) {
                    next = matching(tokens, next);
                } else if (text.equals(",") && isIdentifier(text(tokens, next + 1))) {
                    declare(variables, text(tokens, next + 1), type);
                }
            }
        }
    }

    private static void declare(Map<String, Type> variables, String name, Type type) {
        variables.merge(name, type, (one, other) -> one == other ? one : Type.UNKNOWN);
    }

    private static Type typeNamed(String word) {
        if (word.equals("float") || word.matches("vec[234]|mat[234](x[234])?")) {
            return Type.FLOAT;
        }
        if (word.equals("int") || word.equals("uint") || word.equals("bool")
                || word.matches("[iub]vec[234]")) {
            return Type.INT;
        }
        return null;
    }

    /** types the expressions between two tokens, as far as names and literals tell */
    private record Typing(List<Token> tokens, Map<String, Type> variables,
            Map<String, Function> functions) {

        /** float if any operand at the top level is a float, int if all are ints */
        Type of(int from, int to) {
            if (from < 0 || from >= to) {
                return Type.UNKNOWN;
            }
            boolean allInt = true;
            int at = from;
            while (at < to) {
                String text = text(tokens, at);
                if (isSign(text) || text.equals("!") || BINARY.contains(text)) {
                    at++;
                    continue;
                }
                int end = Math.min(primaryAfter(at), to);
                Type type = primary(at, end);
                if (type == Type.FLOAT) {
                    return Type.FLOAT;
                }
                allInt &= type == Type.INT;
                at = Math.max(end, at + 1);
            }
            return allInt ? Type.INT : Type.UNKNOWN;
        }

        private Type primary(int at, int end) {
            String text = text(tokens, at);
            if (text.equals("(")) {
                return of(at + 1, matching(tokens, at));
            }
            if (text.matches("[0-9][0-9a-fA-FxXuU]*")) {
                return Type.INT;
            }
            if (!text.isEmpty() && (Character.isDigit(text.charAt(0)) || text.charAt(0) == '.')) {
                return Type.FLOAT;
            }
            if (!isIdentifier(text)) {
                return Type.UNKNOWN;
            }
            if (!text(tokens, at + 1).equals("(")) {
                return variables.getOrDefault(text, Type.UNKNOWN);
            }
            Type constructed = typeNamed(text);
            if (constructed != null) {
                return constructed;
            }
            if (FLOAT_ONLY.contains(text)) {
                return Type.FLOAT;
            }
            if (GENERIC.contains(text)) {
                return of(at + 2, matching(tokens, at + 1));
            }
            Function function = functions.get(text);
            return function == null ? Type.UNKNOWN : function.returns();
        }

        /** the token after the primary that starts at a token, with its calls and swizzles */
        int primaryAfter(int at) {
            int end = at;
            if (text(tokens, end).equals("(")) {
                end = matching(tokens, end) + 1;
            } else {
                end++;
            }
            while (true) {
                String text = text(tokens, end);
                if (text.equals("(") || text.equals("[")) {
                    end = matching(tokens, end) + 1;
                } else if (text.equals(".") && isIdentifier(text(tokens, end + 1))) {
                    end += 2;
                } else {
                    return end;
                }
            }
        }

        /** the first token of the primary that ends at a token */
        int primaryEndingAt(int at) {
            int start = at;
            while (start >= 0) {
                String text = text(tokens, start);
                if (text.equals(")") || text.equals("]")) {
                    start = opening(tokens, start);
                    if (start > 0 && isIdentifier(text(tokens, start - 1))
                            && !KEYWORDS.contains(text(tokens, start - 1))) {
                        start--;
                    } else if (text.equals(")")) {
                        return start;
                    } else {
                        start--;
                        continue;
                    }
                }
                if (start > 1 && text(tokens, start - 1).equals(".")) {
                    start -= 2;
                    continue;
                }
                return start;
            }
            return -1;
        }

        /** each argument of the call whose parenthesis opens at a token, as token ranges */
        List<int[]> arguments(int open) {
            List<int[]> found = new ArrayList<>();
            int close = matching(tokens, open);
            int from = open + 1;
            for (int at = open + 1; at <= close && at < tokens.size(); at++) {
                String text = text(tokens, at);
                if (text.equals("(") || text.equals("[")) {
                    at = matching(tokens, at);
                } else if (text.equals(",") || at == close) {
                    if (at > from) {
                        found.add(new int[] {from, at});
                    }
                    from = at + 1;
                }
            }
            return found;
        }
    }

    private static boolean endsOperand(List<Token> tokens, int at) {
        String text = text(tokens, at);
        return text.equals(")") || text.equals("]")
                || (!text.isEmpty() && (Character.isLetterOrDigit(text.charAt(0))
                || text.charAt(0) == '_' || text.charAt(0) == '.')
                && !KEYWORDS.contains(text));
    }

    private static int matching(List<Token> tokens, int open) {
        String opener = text(tokens, open);
        String closer = opener.equals("(") ? ")" : opener.equals("[") ? "]" : "}";
        int depth = 0;
        for (int at = open; at < tokens.size(); at++) {
            String text = text(tokens, at);
            if (text.equals(opener)) {
                depth++;
            } else if (text.equals(closer) && --depth == 0) {
                return at;
            }
        }
        return tokens.size();
    }

    private static int opening(List<Token> tokens, int close) {
        String closer = text(tokens, close);
        String opener = closer.equals(")") ? "(" : "[";
        int depth = 0;
        for (int at = close; at >= 0; at--) {
            String text = text(tokens, at);
            if (text.equals(closer)) {
                depth++;
            } else if (text.equals(opener) && --depth == 0) {
                return at;
            }
        }
        return -1;
    }

    private static String text(List<Token> tokens, int at) {
        return at >= 0 && at < tokens.size() ? tokens.get(at).text() : "";
    }

    private static boolean isSign(String text) {
        return text.equals("-") || text.equals("+");
    }

    private static boolean isIdentifier(String text) {
        return !text.isEmpty() && (Character.isLetter(text.charAt(0)) || text.charAt(0) == '_');
    }

    private static boolean isIntLiteral(String text) {
        return text.matches("[1-9][0-9]*|0");
    }

    /** the code's words, numbers and punctuation, leaving out preprocessor lines */
    private static List<Token> tokens(String code) {
        List<Token> tokens = new ArrayList<>();
        int at = 0;
        boolean lineStart = true;
        while (at < code.length()) {
            char c = code.charAt(at);
            if (c == '\n') {
                lineStart = true;
                at++;
            } else if (Character.isWhitespace(c)) {
                at++;
            } else if (c == '#' && lineStart) {
                while (at < code.length() && code.charAt(at) != '\n') {
                    at++;
                }
            } else {
                lineStart = false;
                int end = at + 1;
                if (Character.isLetter(c) || c == '_') {
                    while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end))
                            || code.charAt(end) == '_')) {
                        end++;
                    }
                } else if (Character.isDigit(c)
                        || (c == '.' && end < code.length() && Character.isDigit(code.charAt(end)))) {
                    end = numberEnd(code, at);
                } else if (end < code.length() && "=<>!+-*/".indexOf(c) >= 0
                        && code.charAt(end) == '=') {
                    end++;
                }
                tokens.add(new Token(code.substring(at, end), at));
                at = end;
            }
        }
        return tokens;
    }

    private static int numberEnd(String code, int from) {
        int at = from;
        while (at < code.length()) {
            char c = code.charAt(at);
            boolean exponentSign = (c == '+' || c == '-')
                    && (code.charAt(at - 1) == 'e' || code.charAt(at - 1) == 'E');
            if (!Character.isLetterOrDigit(c) && c != '.' && !exponentSign) {
                return at;
            }
            at++;
        }
        return at;
    }
}
