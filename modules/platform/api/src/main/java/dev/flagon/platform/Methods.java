package dev.flagon.platform;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * the {@link Method} calls the browser classlib leaves out, which the build sends here.
 */
public final class Methods {

    private Methods() {
    }

    /** teavm reports every interface method as abstract, so the build notes which have a body */
    public static boolean isDefault(Method method) {
        int modifiers = method.getModifiers();
        if (!method.getDeclaringClass().isInterface() || !Modifier.isPublic(modifiers)
                || Modifier.isStatic(modifiers)) {
            return false;
        }
        return ClassData.of(method.getDeclaringClass()).bodies.contains(ClassData.keyOf(
                method.getName(), method.getParameterTypes(), method.getReturnType()));
    }

    /** the default of an annotation member, as the build wrote it, or null */
    public static Object getDefaultValue(Method method) {
        ClassData data = ClassData.of(method.getDeclaringClass());
        String written = data.defaults.get(ClassData.keyOf(method.getName(),
                method.getParameterTypes(), method.getReturnType()));
        return written == null ? null : value(data, written, method.getReturnType());
    }

    public static <A extends Annotation> A[] getAnnotationsByType(Method method, Class<A> asked) {
        return Annotations.byType(method.getAnnotations(), asked);
    }

    public static <A extends Annotation> A[] getDeclaredAnnotationsByType(Method method,
                                                                         Class<A> asked) {
        return Annotations.byType(method.getDeclaredAnnotations(), asked);
    }

    private static Object value(ClassData data, String written, Class<?> type) {
        if (written.startsWith("[")) {
            List<String> items = split(written.substring(1, written.length() - 1));
            Class<?> item = type.getComponentType();
            Object array = Array.newInstance(item, items.size());
            for (int at = 0; at < items.size(); at++) {
                Array.set(array, at, value(data, items.get(at), item));
            }
            return array;
        }
        String rest = written.substring(1);
        return switch (written.charAt(0)) {
            case 'z' -> rest.equals("1");
            case 'b' -> Byte.parseByte(rest);
            case 'c' -> (char) Integer.parseInt(rest);
            case 'h' -> Short.parseShort(rest);
            case 'i' -> Integer.parseInt(rest);
            case 'j' -> Long.parseLong(rest);
            case 'f' -> Float.intBitsToFloat(Integer.parseInt(rest));
            case 'd' -> Double.longBitsToDouble(Long.parseLong(rest));
            case 's' -> ClassData.decode(rest);
            case 't' -> data.ofDescriptor(rest);
            case 'e' -> constant(data, rest);
            default -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object constant(ClassData data, String written) {
        int end = written.indexOf(';') + 1;
        Class<? extends Enum> type = (Class<? extends Enum>) data.ofDescriptor(
                written.substring(0, end));
        return Enum.valueOf(type, ClassData.decode(written.substring(end + 1)));
    }

    private static List<String> split(String items) {
        List<String> parts = new ArrayList<>();
        if (!items.isEmpty()) {
            parts.addAll(List.of(items.split(",")));
        }
        return parts;
    }
}
