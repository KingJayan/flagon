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
    void listsNothingWhenTheBuildAsksForNoList() throws Exception {
        Path jar = jar("real.Entry", "real.Entry");

        WriteAppEntry.main(new String[] {
            jar.toString(), directory.resolve("out").toString(), null});

        assertFalse(Files.exists(directory.resolve("listed")));
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
