package dev.flagon.platform;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
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
        URL found = getResource(loader, name);
        return found == null ? Collections.emptyEnumeration()
                : Collections.enumeration(List.of(found));
    }

    public static URL getResource(ClassLoader loader, String name) {
        URL found = ResourceUrls.of(name);
        if (found == null) {
            found = packageAt(name);
        }
        if (found == null && TOLD.add(name)) {
            say("nothing answers getResource(\"" + name + "\"): this bundle packs no such file.");
        }
        return found;
    }

    /** finds the directory that holds a package, for a scan to read */
    private static URL packageAt(String name) {
        String path = Resources.clean(name);
        if (path.isEmpty() || path.endsWith("/") || !KeptClasses.holds(path)) {
            return null;
        }
        File directory = KeptClasses.layOut(path);
        try {
            return directory.toURI().toURL();
        } catch (RuntimeException | MalformedURLException noUrl) {
            return null;
        }
    }

    public static URL getSystemResource(String name) {
        return getResource(null, name);
    }

    private static void say(String warning) {
        try {
            Platforms.get().console().warn("flagon: " + warning);
        } catch (RuntimeException noPlatformYet) {
            System.err.println("flagon: " + warning);
        }
    }
}
