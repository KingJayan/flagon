package dev.flagon.appentry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.JavaCompiler;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WriteAppEntryTest {

    @TempDir
    Path directory;

    @Test
    void takesTheClassTheManifestNames() throws Exception {
        Path jar = jar("real.Entry", "real.Entry");

        assertTrue(entryFor(jar).contains("real.Entry.main(arguments)"));
    }

    @Test
    void givesMinecraftWhatItsLauncherWouldWhenTheRunHasNoArguments() throws Exception {
        Path built = jar("net.minecraft.client.main.Main", "net.minecraft.client.main.Main");
        Path jar = Files.move(built, built.resolveSibling("1.8.9.jar"));

        String defaults = "{\"--accessToken\", \"0\", \"--version\", \"1.8.9\"}";
        assertTrue(entryFor(jar).contains("net.minecraft.client.main.Main.main("
                + "arguments.length == 0 ? new String[] " + defaults + " : arguments)"));
    }

    @Test
    void pointsMinecraftAtTheAssetIndexBesideItsJars() throws Exception {
        Path built = jar("net.minecraft.client.main.Main", "net.minecraft.client.main.Main");
        Files.move(built, built.resolveSibling("1.8.9.jar"));
        Files.createDirectories(directory.resolve("assets/indexes"));
        Files.writeString(directory.resolve("assets/indexes/1.8.json"), "{}");

        assertTrue(entryFor(directory).contains("\"--version\", \"1.8.9\", \"--assetIndex\", "
                + "\"1.8\"}"));
    }

    @Test
    void findsTheRealMainWhenTheManifestNamesAClassTheJarLacks() throws Exception {
        Path jar = jar("real.deep.Entry", "real.Entry");

        String written = entryFor(jar);
        assertTrue(written.contains("real.deep.Entry.main(arguments)"));
        assertFalse(written.contains("real.Entry.main(arguments)"));
    }

    @Test
    void saysSoWhenTheJarRunsNothing() throws Exception {
        Path jar = jar(null, null);

        assertTrue(entryFor(jar).contains("no jar in vendor/"));
    }

    @Test
    void runsTheJarThatNamesAMainClassAndNotItsLibraries() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", "real.Entry"), vendor.resolve("app.jar"));
        Files.move(jar("library.Tool", null), vendor.resolve("library.jar"));

        String written = entryFor(vendor);
        assertTrue(written.contains("real.Entry.main(arguments)"));
        assertFalse(written.contains("library.Tool"));
    }

    @Test
    void namesTheClassesTwoJarsBothCarry() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("shared.Thing", null), vendor.resolve("a-library.jar"));
        Files.move(jar("shared.Thing", "shared.Thing"), vendor.resolve("z-app.jar"));

        Map<String, String> shadowed = WriteAppEntry.shadowedIn(List.of(
                vendor.resolve("a-library.jar"), vendor.resolve("z-app.jar")));

        assertEquals(Set.of("shared.Thing"), shadowed.keySet());
        assertTrue(shadowed.get("shared.Thing").contains("a-library.jar wins over z-app.jar"),
                shadowed.toString());
    }

    @Test
    void saysNothingWhenTheJarsShareNoClass() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", "real.Entry"), vendor.resolve("app.jar"));
        Files.move(jar("library.Tool", null), vendor.resolve("library.jar"));

        assertEquals(Map.of(), WriteAppEntry.shadowedIn(List.of(
                vendor.resolve("app.jar"), vendor.resolve("library.jar"))));
    }

    @Test
    void searchesEveryJarWhenNoneNamesAMainClass() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", null), vendor.resolve("app.jar"));

        assertTrue(entryFor(vendor).contains("real.Entry.main(arguments)"));
    }

    /**
     * reflection constructs a class it has a Class object of and no instance: one named by a
     * literal or a string, one a signature names, and the types gson fills their fields with
     */
    @Test
    void listsTheClassesTheJarsCanConstructByName() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        sources(vendor.resolve("app.jar"), Map.of(
                "app.Entry", """
                        public class Entry {
                            static Object made = Made.class;
                            static Object token = new Token();
                            public static void main(String[] args) throws Exception {
                                switch (args.length) {
                                    case 0, 1, 2, 3 -> new Unnamed();
                                    default -> { }
                                }
                                switch (args.length) {
                                    case 1, 1000, 100000 -> Class.forName("app.Named");
                                    default -> { }
                                }
                                System.out.println(Late.class);
                            }
                        }""",
                "app.Made", "public class Made { Filled filled; java.util.List<Listed> listed; }",
                "app.Filled", "public class Filled { }",
                "app.Listed", "public class Listed { }",
                "app.Named", "public class Named { }",
                "app.Unnamed", "public class Unnamed { Filled filled; }",
                "app.Base", "public class Base<T> { }",
                "app.Token", "public class Token extends Base<Read> { }",
                "app.Read", "public class Read { }",
                "app.Late", "public class Late { }"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString()});

        assertEquals(List.of("app.Filled", "app.Late", "app.Listed", "app.Made", "app.Named",
                "app.Read"),
                Files.readAllLines(listed).stream().sorted().toList());
    }

    @Test
    void keepsOnlyTheClassesAGlobNames() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        sources(vendor.resolve("app.jar"), naming("real.Made", "library.Tool"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString(), "real.**"});

        assertEquals(List.of("real.Made"), Files.readAllLines(listed));
    }

    @Test
    void takesSeveralGlobsAndStopsAPackageGlobAtItsBoundary() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        sources(vendor.resolve("app.jar"), naming("real.deep.Made", "library.Tool"));
        Path shallow = directory.resolve("listed/shallow.txt");
        Path both = directory.resolve("listed/both.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), shallow.toString(), "real.*"});
        assertEquals(List.of(), Files.readAllLines(shallow));

        WriteAppEntry.main(new String[] {vendor.toString(), directory.resolve("out").toString(),
            both.toString(), "real.**, library.*"});
        assertEquals(List.of("library.Tool", "real.deep.Made"),
                Files.readAllLines(both).stream().sorted().toList());
    }

    @Test
    void listsEverythingWhenGivenNoGlobs() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        sources(vendor.resolve("app.jar"), naming("real.Made"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString(), "  "});

        assertEquals(List.of("real.Made"), Files.readAllLines(listed));
    }

    @Test
    void listsNothingWhenTheBuildAsksForNoList() throws Exception {
        Path jar = jar("real.Entry", "real.Entry");

        WriteAppEntry.main(new String[] {
            jar.toString(), directory.resolve("out").toString(), null});

        assertFalse(Files.exists(directory.resolve("listed")));
    }

    @Test
    void loadsAClassThatOnlyAResourceNames() throws Exception {
        Path jar = jarNaming("real.Entry", "LoggerFactory = real.Entry\n");

        String written = entryFor(jar);
        assertTrue(written.contains("real.Entry.class,"), written);
        assertTrue(written.contains("loadNamedClasses();"), written);
    }

    @Test
    void ignoresANameNoJarCarries() throws Exception {
        Path jar = jarNaming("real.Entry", "LoggerFactory = other.Missing\n");

        assertEquals(List.of(), WriteAppEntry.namedByResources(List.of(jar)));
    }

    @Test
    void loadsNothingWhenNoResourceNamesAClass() throws Exception {
        Path jar = jar("real.Entry", "real.Entry");

        assertFalse(entryFor(jar).contains("loadNamedClasses"));
    }

    private Path jarNaming(String type, String resource) throws Exception {
        Path classes = compile(type);
        Path jar = directory.resolve("naming.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            String name = type.replace('.', '/') + ".class";
            out.putNextEntry(new JarEntry(name));
            Files.copy(classes.resolve(name), (OutputStream) out);
            out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/provider.properties"));
            out.write(resource.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private String entryFor(Path jar) throws Exception {
        Path into = Files.createDirectories(directory.resolve("out"));
        WriteAppEntry.main(new String[] {jar.toString(), into.toString()});
        return Files.readString(into.resolve("dev/flagon/app/AppEntry.java"));
    }

    private Path jar(String type, String declared) throws Exception {
        Path classes = compile(type);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (declared != null) {
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, declared);
        }

        Path jar = directory.resolve(Math.abs(String.valueOf(type).hashCode()) + ".jar");
        try (JarOutputStream out =
                new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            if (type != null) {
                String name = type.replace('.', '/') + ".class";
                out.putNextEntry(new JarEntry(name));
                Files.copy(classes.resolve(name), (OutputStream) out);
                out.closeEntry();
            }
        }
        return jar;
    }

    /** an entry class that names each of {@code named} by a class literal, and those classes */
    private static Map<String, String> naming(String... named) {
        Map<String, String> sources = new LinkedHashMap<>();
        StringBuilder literals = new StringBuilder();
        for (String type : named) {
            literals.append(type).append(".class, ");
            sources.put(type, "public class " + type.substring(type.lastIndexOf('.') + 1)
                    + " { }");
        }
        sources.put("entry.Entry", "public class Entry { static Object[] named = {" + literals
                + "}; public static void main(String[] args) { } }");
        return sources;
    }

    /** compiles each source, keyed by its class, into one jar */
    private void sources(Path jar, Map<String, String> sources) throws IOException {
        Path root = Files.createTempDirectory(directory, "sources");
        Path classes = Files.createDirectories(root.resolve("classes"));
        List<String> arguments = new ArrayList<>(List.of("-d", classes.toString()));
        for (Map.Entry<String, String> source : sources.entrySet()) {
            String type = source.getKey();
            int dot = type.lastIndexOf('.');
            Path file = Files.createDirectories(root.resolve(type.substring(0, dot)))
                    .resolve(type.substring(dot + 1) + ".java");
            Files.writeString(file, "package " + type.substring(0, dot) + ";\n"
                    + source.getValue());
            arguments.add(file.toString());
        }
        int failed = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                arguments.toArray(new String[0]));
        assertTrue(failed == 0, "test fixture did not compile");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar));
             var tree = Files.walk(classes)) {
            for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(file).toString()
                        .replace('\\', '/')));
                Files.copy(file, (OutputStream) out);
                out.closeEntry();
            }
        }
    }

    private Path compile(String type) throws IOException {
        Path classes = Files.createDirectories(directory.resolve("classes"));
        if (type == null) {
            return classes;
        }
        int dot = type.lastIndexOf('.');
        Path source = directory.resolve(type.substring(dot + 1) + ".java");
        Files.writeString(source, """
                package %s;

                public class %s {
                    public static void main(String[] args) {
                    }
                }
                """.formatted(type.substring(0, dot), type.substring(dot + 1)),
                StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int failed = compiler.run(null, null, null,
                "-d", classes.toString(), source.toString());
        assertTrue(failed == 0, "test fixture did not compile");
        return classes;
    }
}
