package dev.flagon.platform;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class ReflectionWatch {

    private static final Set<String> told = new HashSet<>();

    private ReflectionWatch() {
    }

    public static Method[] declaredMethods(Class<?> type) {
        Method[] found = Classes.withoutData(type.getDeclaredMethods());
        return warnIfStripped(type, found, found.length, "declared methods");
    }

    public static Method[] methods(Class<?> type) {
        Method[] found = Classes.withoutData(type.getMethods());
        return warnIfStripped(type, found, found.length, "public methods");
    }

    public static Field[] declaredFields(Class<?> type) {
        Field[] found = type.getDeclaredFields();
        return warnIfStripped(type, found, found.length, "declared fields");
    }

    public static Field[] fields(Class<?> type) {
        Field[] found = type.getFields();
        return warnIfStripped(type, found, found.length, "public fields");
    }

    public static Constructor<?>[] declaredConstructors(Class<?> type) {
        Constructor<?>[] found = Classes.withoutSerial(type.getDeclaredConstructors());
        return warnIfStripped(type, found, found.length, "declared constructors");
    }

    public static Constructor<?>[] constructors(Class<?> type) {
        Constructor<?>[] found = Classes.withoutSerial(type.getConstructors());
        return warnIfStripped(type, found, found.length, "public constructors");
    }

    private static <T> T warnIfStripped(Class<?> type, T found, int count, String what) {
        if (count == 0 && wasStripped(type) && told.add(type.getName())) {
            say(type.getName() + " reports no " + what + ", because the build kept no members "
                    + "for it. name the class to keep them: put @Reflectable on it, or add it to "
                    + "flagon.reflect.classes in the pom. see flagon-nu.vercel.app/docs");
        }
        return found;
    }

    /** the platform is not up yet while a class initialiser runs, so the stream is the fallback */
    private static void say(String warning) {
        try {
            Platforms.get().console().warn("flagon: " + warning);
        } catch (RuntimeException noPlatformYet) {
            System.err.println("flagon: " + warning);
        }
    }

    /**
     * every class declares a constructor, so a class reporting none of those kept none of
     * anything. a class that merely declares no methods of its own is not worth a warning.
     */
    private static boolean wasStripped(Class<?> type) {
        if (type.isInterface() || type.isArray() || type.isPrimitive() || type.isEnum()) {
            return false;
        }
        return type.getDeclaredConstructors().length == 0;
    }
}
