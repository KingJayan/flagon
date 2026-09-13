package dev.flagon.platform;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;

public final class Classes {

    private static final Object[] NO_SIGNERS = new Object[0];

    private Classes() {
    }

    public static Type getGenericSuperclass(Class<?> type) {
        return type.getSuperclass();
    }

    public static Type[] getGenericInterfaces(Class<?> type) {
        return type.getInterfaces();
    }

    public static Object[] getSigners(Class<?> type) {
        return NO_SIGNERS.clone();
    }

    public static URL getResource(Class<?> type, String name) {
        return ClassLoaders.getResource(null, name);
    }

    public static InputStream getResourceAsStream(Class<?> type, String name) {
        return Resources.open(beside(type, name));
    }

    /** a name with no leading slash is read beside the class, the way the jdk reads it. */
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
