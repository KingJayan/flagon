package dev.flagon.appentry;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * writes the entry points for whatever jar was dropped in
 *
 * <p>nobody has to say anything about the jar. it is asked what it runs -- its manifest first,
 * then every class in it with a {@code main} -- and each answer becomes a program. a jar with one
 * runs on sight; a jar with several offers all of them rather than guessing between them.
 *
 * <p>what it writes are ordinary calls, not reflection. that is the point: the compiler ships what
 * it can see being reached, and a reflected call reaches nothing.
 *
 * <p>run by the build before {@code bundle-app} compiles. never at runtime.
 */
public final class WriteAppEntry {

    private WriteAppEntry() {
    }

    /** {@code args[0]} the jar, {@code args[1]} the source directory to write into */
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        Path into = Path.of(args[1]).resolve("dev/flagon/app");
        Files.createDirectories(into);

        List<String> mains = Files.isRegularFile(jar) ? runnableIn(jar) : List.of();
        Files.writeString(into.resolve("AppEntry.java"), source(mains));
        System.out.println(mains.isEmpty()
                ? "nothing runnable in " + jar + ", so the app bundle will say so"
                : "app programs: " + mains);
    }

    /** what the jar says it runs, and failing that, everything in it that could be run */
    private static List<String> runnableIn(Path jar) throws IOException {
        try (JarFile opened = new JarFile(jar.toFile())) {
            Manifest manifest = opened.getManifest();
            if (manifest != null) {
                String declared = manifest.getMainAttributes().getValue("Main-Class");
                if (declared != null && !declared.isBlank()) {
                    return List.of(declared.trim());
                }
            }
            return search(jar, opened);
        }
    }

    private static List<String> search(Path jar, JarFile opened) throws IOException {
        List<String> found = new ArrayList<>();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, null)) {
            Enumeration<JarEntry> entries = opened.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.contains("$")) {
                    continue;
                }
                String type = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                if (hasMain(loader, type)) {
                    found.add(type);
                }
            }
        }
        return found;
    }

    /** loaded without initializing it: this is someone else's code, and it is not being run */
    private static boolean hasMain(ClassLoader loader, String type) {
        try {
            var found = Class.forName(type, false, loader).getDeclaredMethod("main", String[].class);
            return Modifier.isStatic(found.getModifiers());
        } catch (ReflectiveOperationException | LinkageError absent) {
            return false;
        }
    }

    /** the simple name, unless two classes share one, in which case the whole name earns its keep */
    private static List<String> idsFor(List<String> mains) {
        Set<String> simple = new LinkedHashSet<>();
        boolean clash = false;
        for (String main : mains) {
            clash |= !simple.add(simpleOf(main));
        }
        List<String> ids = new ArrayList<>();
        for (String main : mains) {
            ids.add(clash ? main.replace('.', '-').toLowerCase(Locale.ROOT) : simpleOf(main));
        }
        return ids;
    }

    private static String simpleOf(String type) {
        int dot = type.lastIndexOf('.');
        return (dot < 0 ? type : type.substring(dot + 1)).toLowerCase(Locale.ROOT);
    }

    private static String source(List<String> mains) {
        List<String> ids = idsFor(mains);
        StringBuilder registrations = new StringBuilder();
        StringBuilder calls = new StringBuilder();

        for (int at = 0; at < mains.size(); at++) {
            registrations.append("                .register(new MainProgram(\"").append(ids.get(at))
                    .append("\", \"").append(mains.get(at))
                    .append("\", AppEntry::run").append(at).append("))\n");
            calls.append("""

                        private static void run%d(String[] arguments) {
                            // a main may declare a checked exception; Program.run may not
                            try {
                                %s.main(arguments);
                            } catch (RuntimeException | Error thrown) {
                                throw thrown;
                            } catch (Exception failed) {
                                throw new IllegalStateException(failed);
                            }
                        }
                    """.formatted(at, mains.get(at)));
        }

        if (mains.isEmpty()) {
            registrations.append("                .register(new MainProgram(\"app\", "
                    + "\"no jar in vendor/\", AppEntry::none))\n");
            calls.append("""

                        private static void none(String[] arguments) {
                            throw new IllegalStateException(
                                    "no jar in vendor/ to run. put one there and build again");
                        }
                    """);
        }

        return """
                package dev.flagon.app;

                import dev.flagon.runtime.MainProgram;
                import dev.flagon.runtime.ProgramRegistry;

                /** what the jar in vendor/ runs. written by the build, not by hand. */
                final class AppEntry {

                    private AppEntry() {
                    }

                    static ProgramRegistry registry() {
                        return new ProgramRegistry()
                %s                ;
                    }
                %s}
                """.formatted(registrations, calls);
    }
}
