package dev.flagon.lwjgl;

import dev.flagon.platform.graphics.EsContext;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** an {@link EsContext} that writes down each call and answers from what a test sets */
final class RecordingEs {

    final List<String> calls = new ArrayList<>();
    final Map<Integer, double[]> parameters = new HashMap<>();
    final Map<String, Object> answers = new HashMap<>();
    private int names;

    final EsContext context = (EsContext) Proxy.newProxyInstance(
            EsContext.class.getClassLoader(), new Class<?>[] {EsContext.class},
            (proxy, method, arguments) -> {
                String name = method.getName();
                calls.add(name + (arguments == null ? "" : " " + describe(arguments)));
                if (name.startsWith("create") || name.equals("fenceSync")) {
                    return ++names;
                }
                if (name.equals("getParameter")) {
                    return parameters.getOrDefault((Integer) arguments[0], new double[] {0});
                }
                Object answer = answers.get(name);
                if (answer instanceof java.util.function.Supplier<?> supplier) {
                    return supplier.get();
                }
                if (answer != null) {
                    return answer;
                }
                if (name.equals("readPixels") && arguments[6] instanceof byte[] into) {
                    java.nio.ByteBuffer.wrap(into).order(java.nio.ByteOrder.nativeOrder())
                            .asFloatBuffer().put(new float[] {0.5f, 0.25f});
                }
                if (name.equals("getBufferSubData")) {
                    byte[] into = (byte[]) arguments[2];
                    for (int i = 0; i < into.length; i++) {
                        into[i] = (byte) (i + 1);
                    }
                }
                return defaultFor(method.getReturnType());
            });

    private static Object defaultFor(Class<?> type) {
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == double[].class) {
            return new double[] {0};
        }
        if (type == int[].class) {
            return new int[0];
        }
        if (type == String[].class) {
            return new String[0];
        }
        return null;
    }

    private static String describe(Object[] arguments) {
        List<String> parts = new ArrayList<>();
        for (Object argument : arguments) {
            parts.add(switch (argument) {
                case byte[] bytes -> Arrays.toString(bytes);
                case int[] ints -> Arrays.toString(ints);
                case float[] floats -> Arrays.toString(floats);
                case Object[] objects -> Arrays.toString(objects);
                case null -> "null";
                default -> String.valueOf(argument);
            });
        }
        return String.join(" ", parts);
    }
}
