package dev.flagon.platform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * what the build wrote into a reflectable class about itself: the lines of
 * dev.flagon.shims.ReflectionLines and a class for each type they name.
 */
final class ClassData {

    static final String METHOD = "$flagon$reflection";

    private static final ClassData NONE = new ClassData(new Object[] {""});
    private static final Map<Class<?>, ClassData> READ = new HashMap<>();

    final String signature;
    final List<String[]> components = new ArrayList<>();
    final List<String> permitted = new ArrayList<>();
    final String[] enclosing;
    final Map<String, String> parameters = new HashMap<>();
    final Map<String, String> defaults = new HashMap<>();
    final java.util.Set<String> bodies = new java.util.HashSet<>();
    final Long serialId;
    private final Map<String, Class<?>> classes = new HashMap<>();

    private ClassData(Object[] written) {
        for (int at = 1; at < written.length; at++) {
            Class<?> named = (Class<?>) written[at];
            classes.put(named.getName(), named);
        }
        String found = null;
        String[] around = null;
        Long id = null;
        for (String line : ((String) written[0]).split("\n")) {
            if (line.length() < 2) {
                continue;
            }
            String[] parts = line.substring(2).split(" ");
            switch (line.charAt(0)) {
                case 'S' -> found = parts[0];
                case 'R' -> components.add(parts);
                case 'P' -> permitted.add(parts[0]);
                case 'E' -> around = parts;
                case 'A' -> parameters.put(parts[0], parts.length > 1 ? parts[1] : "");
                case 'D' -> defaults.put(parts[0], parts[1]);
                case 'B' -> bodies.add(parts[0]);
                case 'U' -> id = Long.parseLong(parts[0]);
                default -> {
                }
            }
        }
        signature = found;
        enclosing = around;
        serialId = id;
    }

    static ClassData of(Class<?> type) {
        ClassData known = READ.get(type);
        if (known == null) {
            known = read(type);
            READ.put(type, known);
        }
        return known;
    }

    private static ClassData read(Class<?> type) {
        if (type.isPrimitive() || type.isArray()) {
            return NONE;
        }
        try {
            Method written = type.getDeclaredMethod(METHOD);
            written.setAccessible(true);
            return new ClassData((Object[]) written.invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | ClassCastException absent) {
            return NONE;
        }
    }

    /** a class by its internal or binary name; the build gave a constant for each one it named */
    Class<?> named(String name) {
        String binary = name.replace('/', '.');
        Class<?> found = classes.get(binary);
        if (found != null) {
            return found;
        }
        try {
            return Class.forName(binary);
        } catch (ClassNotFoundException missing) {
            throw new TypeNotPresentException(binary, missing);
        }
    }

    /** a field descriptor as the class it names */
    Class<?> ofDescriptor(String descriptor) {
        return switch (descriptor.charAt(0)) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            case 'V' -> void.class;
            case '[' -> java.lang.reflect.Array.newInstance(
                    ofDescriptor(descriptor.substring(1)), 0).getClass();
            default -> named(descriptor.substring(1, descriptor.length() - 1));
        };
    }

    /** the key the build wrote a method under: its name and descriptor */
    static String keyOf(String name, Class<?>[] parameters, Class<?> result) {
        StringBuilder key = new StringBuilder(name).append('(');
        for (Class<?> parameter : parameters) {
            key.append(descriptorOf(parameter));
        }
        return key.append(')').append(descriptorOf(result)).toString();
    }

    static String descriptorOf(Class<?> type) {
        if (type.isArray()) {
            return "[" + descriptorOf(type.getComponentType());
        }
        if (type == boolean.class) {
            return "Z";
        } else if (type == byte.class) {
            return "B";
        } else if (type == char.class) {
            return "C";
        } else if (type == short.class) {
            return "S";
        } else if (type == int.class) {
            return "I";
        } else if (type == long.class) {
            return "J";
        } else if (type == float.class) {
            return "F";
        } else if (type == double.class) {
            return "D";
        } else if (type == void.class) {
            return "V";
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    static String decode(String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int at = 0; at < text.length(); at++) {
            char unit = text.charAt(at);
            if (unit == '%') {
                out.append((char) Integer.parseInt(text.substring(at + 1, at + 5), 16));
                at += 4;
            } else {
                out.append(unit);
            }
        }
        return out.toString();
    }
}
