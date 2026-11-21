package dev.flagon.platform;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;

public final class Classes {

    private static final Object[] NO_SIGNERS = new Object[0];

    private Classes() {
    }

    public static Type getGenericSuperclass(Class<?> type) {
        return GenericSupertypes.superclass(type);
    }

    public static Type[] getGenericInterfaces(Class<?> type) {
        return GenericSupertypes.interfaces(type);
    }

    public static TypeVariable<?>[] getTypeParameters(Class<?> type) {
        TypeVariable<?>[] made = GenericSupertypes.parameters(type);
        return made != null ? made : type.getTypeParameters();
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
