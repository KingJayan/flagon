package dev.flagon.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        var packed = WriteResourcePack.collect(jar);

        assertEquals(2, packed.size());
        assertTrue(packed.containsKey("assets/lang.txt"));
        assertTrue(packed.containsKey("META-INF/services/a.b.C"));
        assertFalse(packed.containsKey("dev/flagon/Thing.class"));
    }

    @Test
    void givesTheFirstJarTheNameTwoJarsShare(@TempDir Path work) throws IOException {
        jar(work.resolve("a.jar"), Map.of("shared.txt", "first"));
        jar(work.resolve("b.jar"), Map.of("shared.txt", "second"));

        var packed = WriteResourcePack.collect(work);

        assertEquals(work.resolve("a.jar"), packed.get("shared.txt").source());
    }

    @Test
    void takesATreeOfFilesAsWellAsAJar(@TempDir Path work) throws IOException {
        Files.createDirectories(work.resolve("assets"));
        Files.writeString(work.resolve("assets/lang.txt"), "hello");
        Files.writeString(work.resolve("Thing.class"), "bytecode");

        var packed = WriteResourcePack.collect(work);

        assertEquals(Map.of("assets/lang.txt", "hello").keySet(), packed.keySet());
    }

    @Test
    void packsNothingFromASourceThatIsNotThere(@TempDir Path work) throws IOException {
        assertTrue(WriteResourcePack.collect(work.resolve("absent")).isEmpty());
    }

    @Test
    void writesTheMagicTheReaderLooksFor(@TempDir Path work) throws IOException {
        Files.writeString(work.resolve("a.txt"), "a");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        byte[] written = bytes.toByteArray();
        for (int at = 0; at < WriteResourcePack.MAGIC.length; at++) {
            assertEquals(WriteResourcePack.MAGIC[at], written[at]);
        }
    }

    @Test
    void packsATextFileSmallerThanItReadsBack(@TempDir Path work) throws IOException {
        Files.writeString(work.resolve("long.txt"), "abcdefgh".repeat(400));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        assertTrue(bytes.size() < 3200, "the pack is " + bytes.size() + " bytes");
    }

    @Test
    void keepsAFileCompressionWouldOnlyGrow(@TempDir Path work) throws IOException {
        byte[] noisy = new byte[512];
        new java.util.Random(7).nextBytes(noisy);
        Files.write(work.resolve("noise.bin"), noisy);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        byte[] written = bytes.toByteArray();
        assertEquals(WriteResourcePack.STORED, written[written.length - noisy.length - 1]);
    }

    @Test
    void storesAFileItsOwnFormatAlreadyCompressed(@TempDir Path work) throws IOException {
        Files.writeString(work.resolve("tile.png"), "abcdefgh".repeat(400));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        assertEquals(WriteResourcePack.STORED, packingOf(bytes.toByteArray()).get("tile.png"));
    }

    @Test
    void tellsTheFormatsApartAcrossEveryBatch(@TempDir Path work) throws IOException {
        for (int at = 0; at < 200; at++) {
            String name = at % 2 == 0 ? "file" + at + ".txt" : "file" + at + ".png";
            Files.writeString(work.resolve(name), "abcdefgh".repeat(40));
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        Map<String, Integer> packing = packingOf(bytes.toByteArray());
        assertEquals(200, packing.size());
        for (Map.Entry<String, Integer> entry : packing.entrySet()) {
            int wanted = entry.getKey().endsWith(".png")
                    ? WriteResourcePack.STORED
                    : WriteResourcePack.DEFLATED;
            assertEquals(wanted, entry.getValue(), entry.getKey());
        }
    }

    /** @return how each resource in the pack was packed, by name */
    private static Map<String, Integer> packingOf(byte[] pack) {
        ByteBuffer read = ByteBuffer.wrap(pack);
        read.position(WriteResourcePack.MAGIC.length);
        Map<String, Integer> packing = new LinkedHashMap<>();
        for (int count = read.getInt(); count > 0; count--) {
            byte[] name = new byte[read.getInt()];
            read.get(name);
            read.getInt();
            int stored = read.getInt();
            packing.put(new String(name, StandardCharsets.UTF_8), (int) read.get());
            read.position(read.position() + stored);
        }
        return packing;
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
