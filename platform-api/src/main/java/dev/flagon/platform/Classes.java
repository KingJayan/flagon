package dev.flagon.platform;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;

public final class Classes {

    private static final Object[] NO_SIGNERS = new Object[0];
    private static final int SYNTHETIC = 0x1000;

    private Classes() {
    }

    /** without the method the build adds to carry a class's own reflection data */
    public static Method[] getDeclaredMethods(Class<?> type) {
        return withoutData(type.getDeclaredMethods());
    }

    public static Method[] getMethods(Class<?> type) {
        return withoutData(type.getMethods());
    }

    static Method[] withoutData(Method[] all) {
        int kept = 0;
        for (Method method : all) {
            if (!method.getName().equals(ClassData.METHOD)) {
                all[kept++] = method;
            }
        }
        return kept == all.length ? all : java.util.Arrays.copyOf(all, kept);
    }

    public static Type getGenericSuperclass(Class<?> type) {
        Signatures.Read read = Signatures.of(type);
        return read != null ? read.superclass() : GenericSupertypes.superclass(type);
    }

    public static Type[] getGenericInterfaces(Class<?> type) {
        Signatures.Read read = Signatures.of(type);
        return read != null ? read.interfaces() : GenericSupertypes.interfaces(type);
    }

    public static TypeVariable<?>[] getTypeParameters(Class<?> type) {
        Signatures.Read read = Signatures.of(type);
        if (read != null) {
            return read.parameters();
        }
        TypeVariable<?>[] made = GenericSupertypes.parameters(type);
        return made != null ? made : type.getTypeParameters();
    }

    public static boolean isRecord(Class<?> type) {
        return type.getSuperclass() == Record.class && Modifier.isFinal(type.getModifiers());
    }

    public static boolean isSealed(Class<?> type) {
        return !ClassData.of(type).permitted.isEmpty();
    }

    public static Class<?>[] getPermittedSubclasses(Class<?> type) {
        ClassData data = ClassData.of(type);
        if (data.permitted.isEmpty()) {
            return null;
        }
        Class<?>[] permitted = new Class<?>[data.permitted.size()];
        for (int at = 0; at < permitted.length; at++) {
            permitted[at] = data.named(data.permitted.get(at));
        }
        return permitted;
    }

    public static Method getEnclosingMethod(Class<?> type) {
        String[] around = ClassData.of(type).enclosing;
        if (around == null || around[1].equals("-") || around[1].equals("<init>")) {
            return null;
        }
        Class<?> owner = ClassData.of(type).named(around[0]);
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(around[1]) && ClassData.keyOf(method.getName(),
                    method.getParameterTypes(), method.getReturnType())
                    .equals(around[1] + around[2])) {
                return method;
            }
        }
        return null;
    }

    public static Constructor<?> getEnclosingConstructor(Class<?> type) {
        String[] around = ClassData.of(type).enclosing;
        if (around == null || !around[1].equals("<init>")) {
            return null;
        }
        Class<?> owner = ClassData.of(type).named(around[0]);
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            if (ClassData.keyOf("<init>", constructor.getParameterTypes(), void.class)
                    .equals("<init>" + around[2])) {
                return constructor;
            }
        }
        return null;
    }

    public static <A extends Annotation> A getDeclaredAnnotation(Class<?> type, Class<A> asked) {
        for (Annotation annotation : type.getDeclaredAnnotations()) {
            if (asked.isInstance(annotation)) {
                return asked.cast(annotation);
            }
        }
        return null;
    }

    public static <A extends Annotation> A[] getAnnotationsByType(Class<?> type, Class<A> asked) {
        return Annotations.byType(type.getAnnotations(), asked);
    }

    public static <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<?> type,
                                                                         Class<A> asked) {
        return Annotations.byType(type.getDeclaredAnnotations(), asked);
    }

    public static String getPackageName(Class<?> type) {
        Class<?> named = type;
        while (named.isArray()) {
            named = named.getComponentType();
        }
        if (named.isPrimitive()) {
            return "java.lang";
        }
        int dot = named.getName().lastIndexOf('.');
        return dot < 0 ? "" : named.getName().substring(0, dot);
    }

    public static Class<?> arrayType(Class<?> type) {
        return Array.newInstance(type, 0).getClass();
    }

    public static Class<?> componentType(Class<?> type) {
        return type.getComponentType();
    }

    public static String descriptorString(Class<?> type) {
        return ClassData.descriptorOf(type);
    }

    public static boolean isSynthetic(Class<?> type) {
        return (type.getModifiers() & SYNTHETIC) != 0;
    }

    public static boolean isHidden(Class<?> type) {
        return false;
    }

    public static Class<?> getNestHost(Class<?> type) {
        Class<?> host = type;
        while (host.getEnclosingClass() != null) {
            host = host.getEnclosingClass();
        }
        return host;
    }

    public static boolean isNestmateOf(Class<?> type, Class<?> other) {
        return getNestHost(type) == getNestHost(other);
    }

    public static Object[] getSigners(Class<?> type) {
        return NO_SIGNERS.clone();
    }

    public static ProtectionDomain getProtectionDomain(Class<?> type) {
        return new ProtectionDomain(null, null);
    }

    public static URL getResource(Class<?> type, String name) {
        return ClassLoaders.getResource(null, beside(type, name));
    }

    public static InputStream getResourceAsStream(Class<?> type, String name) {
        return Resources.open(beside(type, name));
    }

    private static String beside(Class<?> type, String name) {
        if (name == null || name.startsWith("/")) {
            return name;
        }
        String owner = type.getName();
        int marker = owner.lastIndexOf('.');
        return marker < 0 ? name : owner.substring(0, marker).replace('.', '/') + "/" + name;
    }

    public static boolean isAnonymousClass(Class<?> type) {
        String name = type.getName();
        int marker = name.lastIndexOf('$');
        if (marker < 0 || marker == name.length() - 1) {
            return false;
        }
        for (int at = marker + 1; at < name.length(); at++) {
            if (name.charAt(at) < '0' || name.charAt(at) > '9') {
                return false;
            }
        }
        return true;
    }
}
