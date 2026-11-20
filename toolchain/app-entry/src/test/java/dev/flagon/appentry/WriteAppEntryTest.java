package dev.flagon.appentry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** the compiler is told which classes may be constructed by name, libraries included */
    @Test
    void listsEveryClassTheJarsCarry() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", "real.Entry"), vendor.resolve("app.jar"));
        Files.move(jar("library.Tool", null), vendor.resolve("library.jar"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString()});

        assertEquals(List.of("library.Tool", "real.Entry"),
                Files.readAllLines(listed).stream().sorted().toList());
    }

    @Test
    void keepsOnlyTheClassesAGlobNames() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", "real.Entry"), vendor.resolve("app.jar"));
        Files.move(jar("library.Tool", null), vendor.resolve("library.jar"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString(), "real.**"});

        assertEquals(List.of("real.Entry"), Files.readAllLines(listed));
    }

    @Test
    void takesSeveralGlobsAndStopsAPackageGlobAtItsBoundary() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.deep.Entry", null), vendor.resolve("app.jar"));
        Files.move(jar("library.Tool", null), vendor.resolve("library.jar"));
        Path shallow = directory.resolve("listed/shallow.txt");
        Path both = directory.resolve("listed/both.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), shallow.toString(), "real.*"});
        assertEquals(List.of(), Files.readAllLines(shallow));

        WriteAppEntry.main(new String[] {vendor.toString(), directory.resolve("out").toString(),
            both.toString(), "real.**, library.*"});
        assertEquals(List.of("library.Tool", "real.deep.Entry"),
                Files.readAllLines(both).stream().sorted().toList());
    }

    @Test
    void listsEverythingWhenGivenNoGlobs() throws Exception {
        Path vendor = Files.createDirectories(directory.resolve("vendor"));
        Files.move(jar("real.Entry", "real.Entry"), vendor.resolve("app.jar"));
        Path listed = directory.resolve("listed/classes.txt");

        WriteAppEntry.main(new String[] {
            vendor.toString(), directory.resolve("out").toString(), listed.toString(), "  "});

        assertEquals(List.of("real.Entry"), Files.readAllLines(listed));
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
