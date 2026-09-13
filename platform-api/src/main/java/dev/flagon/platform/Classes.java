package dev.flagon.platform;

import java.lang.reflect.Type;

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
