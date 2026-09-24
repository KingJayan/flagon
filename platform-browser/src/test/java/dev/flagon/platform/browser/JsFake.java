package dev.flagon.platform.browser;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsFake {

    private final List<String> calls = new ArrayList<>();
    private final Map<String, Object> answers = new HashMap<>();
    private int handed;

    public <T> T of(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                new Recorder(type.getSimpleName())));
    }

    public JsFake answer(String method, Object value) {
        answers.put(method, value);
        return this;
    }

    public void record(String call) {
        calls.add(call);
    }

    public List<String> calls() {
        return calls;
    }

    public int count(String method) {
        return (int) calls.stream().filter(call -> call.startsWith(method + "(")).count();
    }

    public boolean called(String call) {
        return calls.contains(call);
    }

    public String lastCall(String method) {
        for (int at = calls.size() - 1; at >= 0; at--) {
            if (calls.get(at).startsWith(method + "(")) {
                return calls.get(at);
            }
        }
        return null;
    }

    private final class Recorder implements InvocationHandler {

        private final String label;
        private final Map<String, Object> properties = new HashMap<>();
        private final Map<String, Object> held = new HashMap<>();

        Recorder(String label) {
            this.label = label;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            String name = method.getName();
            switch (name) {
                case "equals" -> {
                    return proxy == arguments[0];
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return label;
                }
                default -> { }
            }
            calls.add(name + "(" + describe(arguments) + ")");
            if (answers.containsKey(name)) {
                return answers.get(name);
            }
            if (isSetter(method, arguments)) {
                properties.put(property(name), arguments[0]);
                return null;
            }
            if (arguments == null && name.startsWith("get")) {
                String property = property(name);
                if (properties.containsKey(property)) {
                    return properties.get(property);
                }
                return held.computeIfAbsent(name,
                        any -> fallback(method.getReturnType(), name));
            }
            return fallback(method.getReturnType(), name);
        }

        private boolean isSetter(Method method, Object[] arguments) {
            return method.getReturnType() == void.class && arguments != null
                    && arguments.length == 1 && method.getName().startsWith("set");
        }

        private String property(String method) {
            return method.substring(3).toLowerCase(java.util.Locale.ROOT);
        }

        private String describe(Object[] arguments) {
            if (arguments == null) {
                return "";
            }
            StringBuilder written = new StringBuilder();
            for (Object argument : arguments) {
                if (written.length() > 0) {
                    written.append(", ");
                }
                written.append(argument == null ? "null" : text(argument));
            }
            return written.toString();
        }

        private String text(Object argument) {
            if (argument instanceof float[] floats) {
                return Arrays.toString(floats);
            }
            if (argument instanceof int[] ints) {
                return Arrays.toString(ints);
            }
            return String.valueOf(argument);
        }
    }

    private Object fallback(Class<?> returned, String method) {
        if (returned == void.class) {
            return null;
        }
        if (returned == boolean.class) {
            return true;
        }
        if (returned == int.class || returned == short.class || returned == byte.class) {
            return 0;
        }
        if (returned == long.class) {
            return 0L;
        }
        if (returned == float.class) {
            return 0.0f;
        }
        if (returned == double.class) {
            return 0.0;
        }
        if (returned == String.class) {
            return "";
        }
        if (returned.isInterface()) {
            handed++;
            return Proxy.newProxyInstance(returned.getClassLoader(), new Class<?>[] {returned},
                    new Recorder(method + "#" + handed));
        }
        return null;
    }
}
