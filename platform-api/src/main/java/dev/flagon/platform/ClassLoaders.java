package dev.flagon.platform;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class ClassLoaders {

    private static final Set<String> TOLD = new HashSet<>();

    private ClassLoaders() {
    }

    public static Class<?> loadClass(ClassLoader loader, String name)
            throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | LinkageError missing) {
            throw new ClassNotFoundException(name + " is not in this bundle. nothing is loaded at"
                    + " runtime: a class reaches the browser only if the build compiled it. name"
                    + " it in flagon.reflect.classes to keep it. see docs/COMPATIBILITY.md",
                    missing);
        }
    }

    public static InputStream getResourceAsStream(ClassLoader loader, String name) {
        return Resources.open(name);
    }

    public static Enumeration<URL> getResources(ClassLoader loader, String name) {
        if (TOLD.add(name)) {
            say("nothing answers getResources(\"" + name + "\"): a bundle is one artifact with no"
                    + " classpath to scan. read the file with getResourceAsStream instead.");
        }
        return Collections.emptyEnumeration();
    }

    public static URL getResource(ClassLoader loader, String name) {
        if (TOLD.add(name)) {
            say("nothing answers getResource(\"" + name + "\"): a bundle has no url for a file it"
                    + " carries. read the file with getResourceAsStream instead.");
        }
        return null;
    }

    private static void say(String warning) {
        try {
            Platforms.get().console().warn("flagon: " + warning);
        } catch (RuntimeException noPlatformYet) {
            System.err.println("flagon: " + warning);
        }
    }
}
