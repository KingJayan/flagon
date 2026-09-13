package dev.flagon.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteResourcePackTest {

    @Test
    void takesEveryFileAJarCarriesBesideItsClasses(@TempDir Path work) throws IOException {
        Path jar = jar(work.resolve("one.jar"), Map.of(
                "assets/lang.txt", "hello",
                "dev/flagon/Thing.class", "bytecode",
                "META-INF/MANIFEST.MF", "Manifest-Version: 1.0",
                "META-INF/services/a.b.C", "a.b.D"));

        Map<String, byte[]> packed = WriteResourcePack.collect(jar);

        assertEquals(2, packed.size());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), packed.get("assets/lang.txt"));
        assertTrue(packed.containsKey("META-INF/services/a.b.C"));
        assertFalse(packed.containsKey("dev/flagon/Thing.class"));
    }

    @Test
    void givesTheFirstJarTheNameTwoJarsShare(@TempDir Path work) throws IOException {
        jar(work.resolve("a.jar"), Map.of("shared.txt", "first"));
        jar(work.resolve("b.jar"), Map.of("shared.txt", "second"));

        Map<String, byte[]> packed = WriteResourcePack.collect(work);

        assertArrayEquals("first".getBytes(StandardCharsets.UTF_8), packed.get("shared.txt"));
    }

    @Test
    void takesATreeOfFilesAsWellAsAJar(@TempDir Path work) throws IOException {
        Files.createDirectories(work.resolve("assets"));
        Files.writeString(work.resolve("assets/lang.txt"), "hello");
        Files.writeString(work.resolve("Thing.class"), "bytecode");

        Map<String, byte[]> packed = WriteResourcePack.collect(work);

        assertEquals(Map.of("assets/lang.txt", "hello").keySet(), packed.keySet());
    }

    @Test
    void packsNothingFromASourceThatIsNotThere(@TempDir Path work) throws IOException {
        assertTrue(WriteResourcePack.collect(work.resolve("absent")).isEmpty());
    }

    @Test
    void writesTheMagicTheReaderLooksFor() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(Map.of("a.txt", "a".getBytes(StandardCharsets.UTF_8)), bytes);

        byte[] written = bytes.toByteArray();
        for (int at = 0; at < WriteResourcePack.MAGIC.length; at++) {
            assertEquals(WriteResourcePack.MAGIC[at], written[at]);
        }
    }

    private static Path jar(Path at, Map<String, String> files) throws IOException {
        try (OutputStream out = Files.newOutputStream(at); ZipOutputStream zip =
                new ZipOutputStream(out)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return at;
    }
}
