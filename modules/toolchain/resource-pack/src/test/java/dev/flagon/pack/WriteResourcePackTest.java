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
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertFalse(packed.get("assets/lang.txt").shipped(), "a lone tree is the classpath");
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
        assertTrue(packed.get("assets/sounds/click.ogg").shipped());
        assertFalse(packed.get("assets/pack.mcmeta").shipped());
    }

    @Test
    void marksAShippedFileInThePack(@TempDir Path work) throws IOException {
        jar(work.resolve("game.jar"), Map.of("inside.txt", "jar"));
        Files.createDirectories(work.resolve("assets"));
        Files.writeString(work.resolve("assets/beside.txt"), "loose");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        Map<String, Integer> places = places(bytes.toByteArray());
        assertEquals(WriteResourcePack.INSIDE, places.get("inside.txt"));
        assertEquals(WriteResourcePack.INSIDE | WriteResourcePack.SHIPPED,
                places.get("assets/beside.txt"));
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
        assertEquals(WriteResourcePack.INSIDE, read.get());
        read.get(contents);
        assertEquals("a.txt", new String(name, StandardCharsets.UTF_8));
        assertEquals("hello", new String(contents, StandardCharsets.UTF_8));
        assertEquals(0, read.remaining());
    }

    /** the browser unzips the whole pack as it downloads, so the file on disk is gzip */
    @Test
    void gzipsThePackItWrites(@TempDir Path work) throws Exception {
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

    @Test
    void putsLargeResourcesApartUnderTheirHash(@TempDir Path work) throws IOException {
        Path resources = Files.createDirectory(work.resolve("in"));
        Files.writeString(resources.resolve("big.ogg"), "0123456789");
        Files.writeString(resources.resolve("small.txt"), "hi");
        Path apart = work.resolve("app.files");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(resources), Map.of(),
                new WriteResourcePack.Options(null, 10, List.of(), null), apart, bytes);

        ByteBuffer read = ByteBuffer.wrap(bytes.toByteArray());
        read.position(WriteResourcePack.MAGIC.length + 4);
        skipName(read);
        assertEquals(2, read.getInt());
        assertEquals(WriteResourcePack.INSIDE, read.get());
        read.position(read.position() + 2);
        skipName(read);
        assertEquals(10, read.getInt());
        assertEquals(WriteResourcePack.APART, read.get());
        byte[] hash = new byte[32];
        read.get(hash);
        Path file = apart.resolve(HexFormat.of().formatHex(hash));
        assertEquals("0123456789", Files.readString(file));
        assertArrayEquals(hash, Hashes.sha256(Files.readAllBytes(file)));
    }

    @Test
    void putsResourcesApartByNameOrByTheirAssetIndexName(@TempDir Path work) throws IOException {
        Path resources = Files.createDirectory(work.resolve("in"));
        Files.createDirectories(resources.resolve("assets/indexes"));
        Files.createDirectories(resources.resolve("assets/objects/ab"));
        String hash = "ab" + "0".repeat(38);
        Files.writeString(resources.resolve("assets/indexes/1.8.json"), "{\"objects\": {"
                + "\"minecraft/lang/de_DE.lang\": {\"hash\": \"" + hash + "\", \"size\": 2}}}");
        Files.writeString(resources.resolve("assets/objects/ab/" + hash), "de");
        Files.createDirectories(resources.resolve("presets"));
        Files.writeString(resources.resolve("presets/a.png"), "a");
        Files.writeString(resources.resolve("en_US.lang"), "en");
        var options = WriteResourcePack.Options.parse(new String[] {
            "--apart=minecraft/lang/*.lang, presets/**"});

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(resources), Map.of(), options,
                work.resolve("app.files"), bytes);

        Map<String, Integer> places = places(bytes.toByteArray());
        assertEquals(WriteResourcePack.APART, places.get("assets/objects/ab/" + hash));
        assertEquals(WriteResourcePack.APART, places.get("presets/a.png"));
        assertEquals(WriteResourcePack.INSIDE, places.get("en_US.lang"));
        assertEquals(WriteResourcePack.INSIDE, places.get("assets/indexes/1.8.json"));
    }

    /** gzip looks only 32 KiB back, so vorbis sits with vorbis and png with png */
    @Test
    void groupsResourcesOfOneKind(@TempDir Path work) throws IOException {
        Files.write(work.resolve("a.ogg"), vorbis(40));
        Files.writeString(work.resolve("b.txt"), "b");
        Files.write(work.resolve("c.png"), "\u0089PNG".getBytes(StandardCharsets.ISO_8859_1));
        Files.write(work.resolve("d.ogg"), vorbis(40));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), bytes);

        assertEquals(List.of("a.ogg", "d.ogg", "c.png", "b.txt"),
                List.copyOf(places(bytes.toByteArray()).keySet()));
    }

    /** the page fetches the files kept apart in pack order */
    @Test
    void putsTheResourcesApartLastAndSmallestFirst(@TempDir Path work) throws IOException {
        Files.writeString(work.resolve("a.txt"), "long enough");
        Files.writeString(work.resolve("b.txt"), "short!");
        Files.writeString(work.resolve("c.txt"), "s");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WriteResourcePack.write(WriteResourcePack.collect(work), Map.of(),
                new WriteResourcePack.Options(null, 5, List.of(), null), work.resolve("apart"),
                bytes);

        assertEquals(List.of("c.txt", "b.txt", "a.txt"),
                List.copyOf(places(bytes.toByteArray()).keySet()));
    }

    @Test
    void namesTheFolderOfResourcesApartAfterThePack() {
        assertEquals(Path.of("out/app.files"), WriteResourcePack.apartOf(Path.of("out/app.pack")));
    }

    @Test
    void reencodesOnlyVorbisThatComesOutSmallEnough(@TempDir Path work) throws Exception {
        Path resources = Files.createDirectory(work.resolve("in"));
        Files.write(resources.resolve("loud.ogg"), vorbis(100));
        Files.write(resources.resolve("quiet.ogg"), vorbis(45));
        Files.write(resources.resolve("opus.ogg"), ogg("\u0013OpusHead", 100));
        Files.createDirectories(resources.resolve("objects/ab"));
        Files.write(resources.resolve("objects/ab/abcd"), vorbis(90));
        Files.write(resources.resolve("loud.txt"), "not audio".repeat(20).getBytes(
                StandardCharsets.US_ASCII));
        int[] runs = {0};
        // cuts a file to 40 bytes, which pays only for the large one
        Vorbis.Encoder halving = (from, to) -> {
            runs[0]++;
            Files.write(to, Arrays.copyOf(Files.readAllBytes(from), 40));
        };

        Map<String, byte[]> shrunk = WriteResourcePack.shrink(
                WriteResourcePack.collect(resources), new Vorbis(halving, work.resolve("cache")),
                "q0");

        assertEquals(Set.of("loud.ogg", "objects/ab/abcd"), shrunk.keySet());
        assertEquals(40, shrunk.get("loud.ogg").length);
        assertEquals(3, runs[0]);

        WriteResourcePack.shrink(WriteResourcePack.collect(resources),
                new Vorbis(halving, work.resolve("cache")), "q0");
        assertEquals(3, runs[0], "a second pack reads the cache");
    }

    @Test
    void tellsVorbisFromOtherOgg() {
        assertTrue(Vorbis.is(vorbis(64)));
        assertFalse(Vorbis.is(ogg("\u0013OpusHead", 64)));
        assertFalse(Vorbis.is("not ogg at all, but long enough to read".getBytes(
                StandardCharsets.US_ASCII)));
        assertFalse(Vorbis.is(new byte[3]));
    }

    @Test
    void readsItsOptions() {
        var options = WriteResourcePack.Options.parse(new String[] {
            "--vorbis-quality=0", "--apart-from=262144", "--apart=a/*,b/**", "build/audio"});

        assertEquals(0, options.vorbisQuality());
        assertEquals(262144, options.apartFrom());
        assertEquals(2, options.apart().size());
        assertTrue(options.apart().get(0).matcher("a/x.lang").matches());
        assertFalse(options.apart().get(0).matcher("a/b/x.lang").matches());
        assertTrue(options.apart().get(1).matcher("b/c/x.png").matches());
        assertEquals(Path.of("build/audio"), options.cache());
        assertThrows(IllegalArgumentException.class,
                () -> WriteResourcePack.Options.parse(new String[] {"--vorbis-quality=0"}));
        assertThrows(IllegalArgumentException.class,
                () -> WriteResourcePack.Options.parse(new String[] {"--fast"}));
    }

    /** each resource's place in the pack, in pack order */
    private static Map<String, Integer> places(byte[] pack) {
        ByteBuffer read = ByteBuffer.wrap(pack);
        read.position(WriteResourcePack.MAGIC.length);
        Map<String, Integer> places = new LinkedHashMap<>();
        for (int count = read.getInt(); count > 0; count--) {
            byte[] name = new byte[read.getInt()];
            read.get(name);
            int length = read.getInt();
            int place = read.get();
            places.put(new String(name, StandardCharsets.UTF_8), place);
            read.position(read.position() + (place == WriteResourcePack.APART ? 32 : length));
        }
        return places;
    }

    private static void skipName(ByteBuffer read) {
        int length = read.getInt();
        read.position(read.position() + length);
    }

    private static byte[] vorbis(int length) {
        return ogg("\u0001vorbis", length);
    }

    /** one ogg page whose first packet opens with {@code header}, padded to {@code length} */
    private static byte[] ogg(String header, int length) {
        byte[] file = new byte[length];
        System.arraycopy("OggS".getBytes(StandardCharsets.US_ASCII), 0, file, 0, 4);
        file[26] = 1;
        byte[] packet = header.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(packet, 0, file, 28, packet.length);
        return file;
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
