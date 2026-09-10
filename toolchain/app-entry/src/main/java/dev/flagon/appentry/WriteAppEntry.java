package dev.flagon.appentry;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * writes the entry points for whatever jar was dropped in
 */
public final class WriteAppEntry {

    private WriteAppEntry() {
    }

    /** naming every class costs compile time and bundle size, so say when the list gets big */
    private static final int WARN_ABOVE = 10_000;

    /** past this the list is doing more harm than the reflection it enables */
    private static final int LIMIT = 50_000;

    /**
     * {@code args[0]} a jar or a directory of them, {@code args[1]} the source directory to
     * write into, {@code args[2]} where to list the classes they carry, if anywhere,
     * {@code args[3]} comma-separated globs narrowing that list, if any
     */
    public static void main(String[] args) throws Exception {
        Path given = Path.of(args[0]);
        Path into = Path.of(args[1]).resolve("dev/flagon/app");
        Files.createDirectories(into);

        List<Path> jars = jarsIn(given);
        List<String> mains = runnableIn(jars);
        Files.writeString(into.resolve("AppEntry.java"), source(mains));
        System.out.println(mains.isEmpty()
                ? "nothing runnable in " + given + ", so the app bundle will say so"
                : "app programs: " + mains);

        // the build may pass the argument through empty, which asks for no list at all
        if (args.length > 2 && args[2] != null && !args[2].isBlank()) {
            String narrowing = args.length > 3 && args[3] != null ? args[3] : "";
            List<String> classes = narrow(classesIn(jars), narrowing);
            if (classes.size() > LIMIT) {
                throw new IllegalStateException(classes.size() + " classes is more than the "
                        + LIMIT + " this will name for reflection. narrow it with"
                        + " -Dflagon.app.reflect=your.game.**,your.other.pack.**");
            }
            Path listed = Path.of(args[2]).toAbsolutePath();
            Files.createDirectories(listed.getParent());
            Files.write(listed, classes);
            System.out.println(classes.size() + " classes may be constructed by reflection");
            if (classes.size() > WARN_ABOVE) {
                System.out.println("that is a lot to keep constructors for, and it costs compile"
                        + " time and bundle size. if the game only reflects over its own packages,"
                        + " say so with -Dflagon.app.reflect=your.game.**");
            }
        }
    }

    /** every class the jars carry, for the compiler to keep the constructors of */
    private static List<String> classesIn(List<Path> jars) throws IOException {
        List<String> classes = new ArrayList<>();
        for (Path jar : jars) {
            try (JarFile opened = new JarFile(jar.toFile())) {
                Enumeration<JarEntry> entries = opened.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.endsWith(".class")) {
                        classes.add(name.substring(0, name.length() - ".class".length())
                                .replace('/', '.'));
                    }
                }
            }
        }
        return classes;
    }

    /**
     * keeps the classes a glob names. {@code *} stops at a package boundary and {@code **} does
     * not, so {@code net.minecraft.*} is one package and {@code net.minecraft.**} is the tree.
     */
    private static List<String> narrow(List<String> classes, String globs) {
        List<Pattern> wanted = new ArrayList<>();
        for (String glob : globs.split(",")) {
            String trimmed = glob.trim();
            if (!trimmed.isEmpty()) {
                wanted.add(Pattern.compile(regexOf(trimmed)));
            }
        }
        if (wanted.isEmpty()) {
            return classes;
        }
        return classes.stream()
                .filter(name -> wanted.stream().anyMatch(one -> one.matcher(name).matches()))
                .toList();
    }

    private static String regexOf(String glob) {
        StringBuilder pattern = new StringBuilder();
        int at = 0;
        while (at < glob.length()) {
            char letter = glob.charAt(at);
            if (letter == '*') {
                boolean deep = at + 1 < glob.length() && glob.charAt(at + 1) == '*';
                pattern.append(deep ? ".*" : "[^.]*");
                at += deep ? 2 : 1;
            } else {
                pattern.append(Pattern.quote(String.valueOf(letter)));
                at++;
            }
        }
        return pattern.toString();
    }

    /** the jars to consider, a directory holding an application's libraries beside it */
    private static List<Path> jarsIn(Path given) throws IOException {
        if (Files.isRegularFile(given)) {
            return List.of(given);
        }
        if (!Files.isDirectory(given)) {
            return List.of();
        }
        try (Stream<Path> listed = Files.list(given)) {
            return listed.filter(found -> found.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        }
    }

    /**
     * what the application runs
     *
     * <p>a library beside it is on the classpath and nothing more, so a jar that declares a main
     * class is taken to be the application, and only when none does is every jar searched.
     */
    private static List<String> runnableIn(List<Path> jars) throws IOException {
        List<Path> declaring = new ArrayList<>();
        for (Path jar : jars) {
            try (JarFile opened = new JarFile(jar.toFile())) {
                if (declaredBy(opened) != null) {
                    declaring.add(jar);
                }
            }
        }

        List<String> mains = new ArrayList<>();
        for (Path jar : declaring.isEmpty() ? jars : declaring) {
            mains.addAll(runnableIn(jar));
        }
        return mains;
    }

    /** what the jar says it runs, and failing that, everything in it that could be run */
    private static List<String> runnableIn(Path jar) throws IOException {
        try (JarFile opened = new JarFile(jar.toFile())) {
            String declared = declaredBy(opened);
            if (declared != null && hasMain(opened, declared.replace('.', '/') + ".class")) {
                return List.of(declared);
            }
            if (declared != null) {
                System.out.println("manifest names " + declared
                        + ", which the jar has no runnable class for. searching instead");
            }
            return search(opened);
        }
    }

    private static String declaredBy(JarFile opened) throws IOException {
        Manifest manifest = opened.getManifest();
        if (manifest == null) {
            return null;
        }
        String declared = manifest.getMainAttributes().getValue("Main-Class");
        return declared == null || declared.isBlank() ? null : declared.trim();
    }

    private static List<String> search(JarFile opened) {
        List<String> found = new ArrayList<>();
        Enumeration<JarEntry> entries = opened.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (!name.endsWith(".class") || name.contains("$")) {
                continue;
            }
            if (hasMain(opened, name)) {
                found.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
            }
        }
        return found;
    }

    /**
     * whether the class file declares {@code public static void main(String[])}
     */
    private static boolean hasMain(JarFile opened, String entry) {
        JarEntry found = opened.getJarEntry(entry);
        if (found == null) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(opened.getInputStream(found)))) {
            if (in.readInt() != 0xCAFEBABE) {
                return false;
            }
            in.skipBytes(4);
            String[] constants = constantPool(in);
            in.skipBytes(6);
            in.skipBytes(2 * in.readUnsignedShort());
            skipMembers(in);
            return declaresMain(in, constants);
        } catch (IOException | RuntimeException unreadable) {
            return false;
        }
    }

    /** only the utf8 entries are kept; nothing else here names a method */
    private static String[] constantPool(DataInputStream in) throws IOException {
        String[] constants = new String[in.readUnsignedShort()];
        for (int at = 1; at < constants.length; at++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> constants[at] = in.readUTF();
                case 7, 8, 16, 19, 20 -> in.skipBytes(2);
                case 15 -> in.skipBytes(3);
                case 5, 6 -> {
                    in.skipBytes(8);
                    at++;
                }
                default -> in.skipBytes(4);
            }
        }
        return constants;
    }

    private static boolean declaresMain(DataInputStream in, String[] constants) throws IOException {
        int methods = in.readUnsignedShort();
        for (int at = 0; at < methods; at++) {
            int access = in.readUnsignedShort();
            String name = constants[in.readUnsignedShort()];
            String descriptor = constants[in.readUnsignedShort()];
            skipAttributes(in);
            if ((access & (Modifier.PUBLIC | Modifier.STATIC))
                    == (Modifier.PUBLIC | Modifier.STATIC)
                    && "main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    private static void skipMembers(DataInputStream in) throws IOException {
        int members = in.readUnsignedShort();
        for (int at = 0; at < members; at++) {
            in.skipBytes(6);
            skipAttributes(in);
        }
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int attributes = in.readUnsignedShort();
        for (int at = 0; at < attributes; at++) {
            in.skipBytes(2);
            in.skipBytes(in.readInt());
        }
    }

    /**
     * the simple name, unless two classes share one, in which case the whole name earns its keep
     */
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
