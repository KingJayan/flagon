package dev.flagon.appentry;

import dev.flagon.classfile.Loadable;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * writes entry points for vendor jar
 */
public final class WriteAppEntry {

    private WriteAppEntry() {
    }

    private static final int WARN_ABOVE = 10_000;

    private static final int LIMIT = 50_000;

    private static final int SHADOWS_SHOWN = 10;

    private static final int RESOURCE_LIMIT = 1 << 20;

    private static final Pattern TYPE_NAME = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");

    public static void main(String[] args) throws Exception {
        Path given = Path.of(args[0]);
        Path into = Path.of(args[1]).resolve("dev/flagon/app");
        Files.createDirectories(into);

        try (OpenJars jars = new OpenJars(jarsIn(given))) {
            write(args, given, into, jars);
        }
    }

    private static void write(String[] args, Path given, Path into, OpenJars jars)
            throws IOException {
        List<String> mains = runnableIn(jars);
        List<String> named = namedByResources(jars);
        Files.writeString(into.resolve("AppEntry.java"), source(mains, named));
        if (!named.isEmpty()) {
            System.out.println("loading " + named.size()
                    + " classes that only a resource names: " + named);
        }
        System.out.println(mains.isEmpty()
                ? "nothing runnable in " + given + ", so the app bundle will say so"
                : "app programs: " + mains);
        reportShadowing(jars);

        if (args.length > 2 && args[2] != null && !args[2].isBlank()) {
            String narrowing = args.length > 3 && args[3] != null ? args[3] : "";

            List<String> classes = narrow(classesIn(jars), narrowing);

            if (classes.size() > LIMIT) {
                throw new IllegalStateException(classes.size() + " classes is more than the "
                        + LIMIT + " this will name for reflection. narrow it with"
                        + " -Pflagon.app.reflect=your.game.**,your.other.pack.**");
            }

            Path listed = Path.of(args[2]).toAbsolutePath();
            Files.createDirectories(listed.getParent());
            Files.write(listed, classes);

            System.out.println(classes.size() + " classes may be constructed by reflection");

            if (classes.size() > WARN_ABOVE) {
                System.out.println("that is a lot to keep constructors for, and it costs compile"
                        + " time and bundle size. if the game only reflects over its own"
                        + " packages, say so with -Pflagon.app.reflect=your.game.**");
            }
        }
    }

    static List<String> namedByResources(List<Path> paths) throws IOException {
        try (OpenJars jars = new OpenJars(paths)) {
            return namedByResources(jars);
        }
    }

    private static List<String> namedByResources(OpenJars jars) throws IOException {
        Map<String, JarClasses.Where> carriers = carriersIn(jars);
        Set<String> carried = carriers.keySet();
        Set<String> named = new LinkedHashSet<>();
        for (OpenJars.Opened opened : jars.all()) {
            for (JarEntry entry : opened.entries()) {
                if (!describes(entry)) {
                    continue;
                }
                Matcher found = TYPE_NAME.matcher(textOf(opened.jar(), entry));
                while (found.find()) {
                    if (carried.contains(found.group())) {
                        named.add(found.group());
                    }
                }
            }
        }
        Loadable loadable = new Loadable(new JarClasses(carriers));
        return named.stream().sorted().filter(loadable::test).toList();
    }

    private static Map<String, JarClasses.Where> carriersIn(OpenJars jars) {
        Map<String, JarClasses.Where> carriers = new LinkedHashMap<>();
        for (OpenJars.Opened opened : jars.all()) {
            for (JarEntry found : opened.entries()) {
                String entry = found.getName();
                if (entry.endsWith(".class")) {
                    carriers.putIfAbsent(
                            entry.substring(0, entry.length() - ".class".length())
                                    .replace('/', '.'),
                            new JarClasses.Where(opened.jar(), entry));
                }
            }
        }
        return carriers;
    }

    private static boolean describes(JarEntry entry) {
        String name = entry.getName();
        return !entry.isDirectory() && name.startsWith("META-INF/")
                && !name.equals("META-INF/MANIFEST.MF")
                && !name.startsWith("META-INF/maven/")
                && entry.getSize() <= RESOURCE_LIMIT;
    }

    // latin-1 accepts any byte
    private static String textOf(JarFile opened, JarEntry entry) throws IOException {
        try (var in = opened.getInputStream(entry)) {
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static List<String> classesIn(OpenJars jars) {
        List<String> classes = new ArrayList<>();
        for (OpenJars.Opened opened : jars.all()) {
            classes.addAll(classesOf(opened));
        }
        return classes;
    }

    private static List<String> classesOf(OpenJars.Opened opened) {
        List<String> classes = new ArrayList<>();
        for (JarEntry entry : opened.entries()) {
            String name = entry.getName();
            if (name.endsWith(".class")) {
                classes.add(name.substring(0, name.length() - ".class".length())
                        .replace('/', '.'));
            }
        }
        return classes;
    }

    static Map<String, String> shadowedIn(List<Path> paths) throws IOException {
        try (OpenJars jars = new OpenJars(paths)) {
            return shadowedIn(jars);
        }
    }

    private static Map<String, String> shadowedIn(OpenJars jars) {
        Map<String, String> firstIn = new HashMap<>();
        Map<String, String> shadowed = new LinkedHashMap<>();
        for (OpenJars.Opened opened : jars.all()) {
            String name = opened.name();
            for (String type : classesOf(opened)) {
                String already = firstIn.putIfAbsent(type, name);
                if (already != null && !already.equals(name)) {
                    shadowed.putIfAbsent(type, already + " wins over " + name);
                }
            }
        }
        return shadowed;
    }

    private static void reportShadowing(OpenJars jars) {
        Map<String, String> shadowed = shadowedIn(jars);
        if (shadowed.isEmpty()) {
            return;
        }
        System.out.println(shadowed.size() + " classes are carried by more than one jar. the fold"
                + " keeps the copy from the jar whose name sorts first and drops the rest, so"
                + " rename a jar if the wrong copy wins:");
        shadowed.entrySet().stream().limit(SHADOWS_SHOWN).forEach(
                entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        if (shadowed.size() > SHADOWS_SHOWN) {
            System.out.println("  and " + (shadowed.size() - SHADOWS_SHOWN) + " more");
        }
    }

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

    private static List<String> runnableIn(OpenJars jars) throws IOException {
        List<OpenJars.Opened> declaring = new ArrayList<>();
        for (OpenJars.Opened opened : jars.all()) {
            if (declaredBy(opened.jar()) != null) {
                declaring.add(opened);
            }
        }

        List<String> mains = new ArrayList<>();
        for (OpenJars.Opened opened : declaring.isEmpty() ? jars.all() : declaring) {
            mains.addAll(runnableIn(opened));
        }
        return mains;
    }

    private static List<String> runnableIn(OpenJars.Opened opened) throws IOException {
        String declared = declaredBy(opened.jar());
        if (declared != null
                && hasMain(opened.jar(), declared.replace('.', '/') + ".class")) {
            return List.of(declared);
        }
        if (declared != null) {
            System.out.println("manifest names " + declared
                    + ", which the jar has no runnable class for. searching instead");
        }
        return search(opened);
    }

    private static String declaredBy(JarFile opened) throws IOException {
        Manifest manifest = opened.getManifest();
        if (manifest == null) {
            return null;
        }
        String declared = manifest.getMainAttributes().getValue("Main-Class");
        return declared == null || declared.isBlank() ? null : declared.trim();
    }

    private static List<String> search(OpenJars.Opened opened) {
        List<String> found = new ArrayList<>();
        for (JarEntry entry : opened.entries()) {
            String name = entry.getName();
            if (!name.endsWith(".class") || name.contains("$")) {
                continue;
            }
            if (hasMain(opened.jar(), name)) {
                found.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
            }
        }
        return found;
    }

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

    private static String loads(List<String> named) {
        if (named.isEmpty()) {
            return "";
        }
        StringBuilder lookups = new StringBuilder();
        for (String type : named) {
            lookups.append("                Class.forName(\"").append(type).append("\");\n");
        }
        return "\n    // no code calls these, so load them or the compiler drops them\n"
                + "    private static void loadNamedClasses() {\n"
                + "        try {\n" + lookups
                + "        } catch (ClassNotFoundException absent) {\n"
                + "            // the jars changed after the build\n"
                + "        }\n"
                + "    }\n";
    }

    private static String source(List<String> mains, List<String> named) {
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
                %s
                    static ProgramRegistry registry() {
                %s        return new ProgramRegistry()
                %s                ;
                    }
                %s}
                """.formatted(loads(named), named.isEmpty() ? "" : "        loadNamedClasses();\n",
                registrations, calls);
    }
}
