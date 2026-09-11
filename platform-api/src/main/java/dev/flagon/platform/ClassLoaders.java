package dev.flagon.platform;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * the browser classlib's {@link ClassLoader} has no loadClass and no getResources, so the build
 * sends both here. there is no loading at runtime: everything a bundle has, it was compiled with.
 */
public final class ClassLoaders {

    private static final Set<String> TOLD = new HashSet<>();

    private ClassLoaders() {
    }

    /**
     * the compiler kept the class or it did not. {@code Class.forName} asks exactly that,
     * so a name the build saw is found and a name it never saw is reported, not invented.
     */
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

    /**
     * a bundle is one artifact, so there is no classpath to walk and no url to hand back. code
     * that scans for every copy of a file finds none, and is told so rather than left to guess.
     */
    public static Enumeration<URL> getResources(ClassLoader loader, String name) {
        if (TOLD.add(name)) {
            say("nothing answers getResources(\"" + name + "\"): a bundle is one artifact with no"
                    + " classpath to scan. read the file with getResourceAsStream instead.");
        }
        return Collections.emptyEnumeration();
    }

    /** the platform is not up yet while a class initialiser runs, so the stream is the fallback */
    private static void say(String warning) {
        try {
            Platforms.get().console().warn("flagon: " + warning);
        } catch (RuntimeException noPlatformYet) {
            System.err.println("flagon: " + warning);
        }
    }
}
