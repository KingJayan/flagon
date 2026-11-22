package java.lang.reflect;

import java.util.HashMap;
import java.util.Map;

/**
 * the lines dev.flagon.shims.WriteReflectionData wrote into a class, for the reflection types
 * this classlib adds. dev.flagon.platform.ClassData reads the same lines for the rest.
 */
final class KeptData {

    private static final String METHOD = "$flagon$reflection";

    final String text;
    private final Map<String, Class<?>> classes = new HashMap<>();

    private KeptData(Object[] written) {
        text = written.length > 0 ? (String) written[0] : "";
        for (int at = 1; at < written.length; at++) {
            Class<?> named = (Class<?>) written[at];
            classes.put(named.getName(), named);
        }
    }

    static KeptData of(Class<?> type) {
        try {
            Method written = type.getDeclaredMethod(METHOD);
            written.setAccessible(true);
            return new KeptData((Object[]) written.invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                 | ClassCastException absent) {
            return new KeptData(new Object[0]);
        }
    }

    /** the fields of each line with the given tag */
    java.util.List<String[]> lines(char tag) {
        java.util.List<String[]> found = new java.util.ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.length() > 2 && line.charAt(0) == tag) {
                found.add(line.substring(2).split(" "));
            }
        }
        return found;
    }

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
            case '[' -> Array.newInstance(ofDescriptor(descriptor.substring(1)), 0).getClass();
            default -> {
                String name = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                Class<?> found = classes.get(name);
                if (found == null) {
                    throw new TypeNotPresentException(name, null);
                }
                yield found;
            }
        };
    }

    static String keyOf(String name, Class<?>[] parameters, Class<?> result) {
        StringBuilder key = new StringBuilder(name).append('(');
        for (Class<?> parameter : parameters) {
            key.append(descriptorOf(parameter));
        }
        return key.append(')').append(descriptorOf(result)).toString();
    }

    private static String descriptorOf(Class<?> type) {
        if (type.isArray()) {
            return "[" + descriptorOf(type.getComponentType());
        }
        if (!type.isPrimitive()) {
            return "L" + type.getName().replace('.', '/') + ";";
        }
        return switch (type.getName()) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "V";
        };
    }

    static String decode(String text) {
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
