package dev.flagon.pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
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
    void takesTheFoldersBesideTheJarsAfterThem(@TempDir Path work) throws IOException {
        jar(work.resolve("game.jar"), Map.of("assets/pack.mcmeta", "jar"));
        Files.createDirectories(work.resolve("assets/sounds"));
        Files.writeString(work.resolve("assets/sounds/click.ogg"), "ogg");
        Files.writeString(work.resolve("assets/pack.mcmeta"), "loose");
        Files.writeString(work.resolve("preserve.txt"), "org.**");

        var packed = WriteResourcePack.collect(work);

        assertEquals(Set.of("assets/pack.mcmeta", "assets/sounds/click.ogg"),
                packed.keySet());
        assertEquals(work.resolve("game.jar"), packed.get("assets/pack.mcmeta").source());
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
    void laysEachResourceOutAsItIs(@TempDir Path work) throws IOException {
        Files.writeString(work.resolve("a.txt"), "hello");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        ByteBuffer read = ByteBuffer.wrap(bytes.toByteArray());
        read.position(WriteResourcePack.MAGIC.length);
        assertEquals(1, read.getInt());
        byte[] name = new byte[read.getInt()];
        read.get(name);
        byte[] contents = new byte[read.getInt()];
        read.get(contents);
        assertEquals("a.txt", new String(name, StandardCharsets.UTF_8));
        assertEquals("hello", new String(contents, StandardCharsets.UTF_8));
        assertEquals(0, read.remaining());
    }

    /** the browser unzips the whole pack as it downloads, so the file on disk is gzip */
    @Test
    void gzipsThePackItWrites(@TempDir Path work) throws IOException {
        Path resources = Files.createDirectory(work.resolve("in"));
        Files.writeString(resources.resolve("long.txt"), "abcdefgh".repeat(400));
        Path pack = work.resolve("out/app.pack");

        WriteResourcePack.main(new String[] {resources.toString(), pack.toString()});

        assertTrue(Files.size(pack) < 400, "the pack is " + Files.size(pack) + " bytes");
        try (InputStream unzipped = new GZIPInputStream(Files.newInputStream(pack))) {
            byte[] magic = unzipped.readNBytes(WriteResourcePack.MAGIC.length);
            assertArrayEquals(WriteResourcePack.MAGIC, magic);
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

    @Test
    void leavesOutNativesAndFilesNoProgramReads() {
        for (String junk : new String[] {"linux/x64/liblwjgl.so", "windows/lwjgl.dll",
                "macos/liblwjgl.jnilib", "Thing.class", "META-INF/MANIFEST.MF",
                "META-INF/CODESIGN.SF", "META-INF/LICENSE", "META-INF/NOTICE.txt",
                "LICENSE-junit.txt", "about.html", "lang/.en_us.lang.swp",
                "META-INF/flagon/constructed.txt", "META-INF/maven/a/b/pom.xml"}) {
            assertFalse(WriteResourcePack.wanted(junk), junk);
        }
        for (String kept : new String[] {"assets/minecraft/lang/en_us.lang", "pack.png",
                "META-INF/services/java.nio.file.spi.FileSystemProvider", "licensed.txt"}) {
            assertTrue(WriteResourcePack.wanted(kept), kept);
        }
    }
}
