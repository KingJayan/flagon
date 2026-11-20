package dev.flagon.pack;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.IntStream;

public final class WriteResourcePack {

    static final byte[] MAGIC = "FLGNPK02".getBytes(StandardCharsets.US_ASCII);

    static final int STORED = 0;
    static final int DEFLATED = 1;

    /** how many entries one batch holds */
    private static final int BATCH = 64;

    private static final int BUFFER = 1 << 16;

    /** already compressed, so deflating only costs time */
    private static final Set<String> ALREADY_PACKED = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "avif", "ico",
            "ogg", "oga", "mp3", "wav", "opus", "webm", "mp4",
            "zip", "jar", "gz", "xz", "zst", "woff", "woff2");

    private static final ThreadLocal<Deflater> DEFLATERS =
            ThreadLocal.withInitial(() -> new Deflater(Deflater.BEST_COMPRESSION));

    /** where one resource comes from */
    record Item(Path source, String entry) {
    }

    private WriteResourcePack() {
    }

    public static void main(String[] args) throws IOException {
        Path given = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        Map<String, Item> files = collect(given);
        Files.createDirectories(pack.getParent());
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(pack), BUFFER)) {
            write(files, out);
        }
        System.out.println(files.isEmpty()
                ? "no resources in " + given + ", so the bundle carries an empty pack"
                : "packed " + files.size() + " resources from " + given + " into " + pack);
    }

    static Map<String, Item> collect(Path given) throws IOException {
        Map<String, Item> files = new LinkedHashMap<>();
        for (Path source : sourcesIn(given)) {
            if (Files.isDirectory(source)) {
                readTree(source, files);
            } else {
                readJar(source, files);
            }
        }
        return files;
    }

    private static List<Path> sourcesIn(Path given) throws IOException {
        if (!Files.exists(given)) {
            return List.of();
        }
        if (!Files.isDirectory(given)) {
            return List.of(given);
        }
        List<Path> jars = new ArrayList<>();
        try (var listing = Files.list(given)) {
            listing.filter(entry -> entry.toString().endsWith(".jar")).sorted().forEach(jars::add);
        }
        return jars.isEmpty() ? List.of(given) : jars;
    }

    private static void readJar(Path jar, Map<String, Item> files) throws IOException {
        try (ZipFile open = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : open.stream().sorted(
                    (left, right) -> left.getName().compareTo(right.getName())).toList()) {
                if (entry.isDirectory() || !wanted(entry.getName())) {
                    continue;
                }
                files.putIfAbsent(entry.getName(), new Item(jar, entry.getName()));
            }
        }
    }

    private static void readTree(Path root, Map<String, Item> files) throws IOException {
        try (var tree = Files.walk(root)) {
            for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
                String name = root.relativize(file).toString().replace('\\', '/');
                if (wanted(name)) {
                    files.putIfAbsent(name, new Item(file, null));
                }
            }
        }
    }

    /** native libraries, signatures, editor files and license texts no program reads */
    private static final Set<String> UNWANTED = Set.of(
            "class", "so", "dll", "dylib", "jnilib", "a", "lib", "pdb", "sha1", "md5",
            "sf", "dsa", "rsa", "ec", "swp", "swo", "bak", "orig", "html", "htm");

    static boolean wanted(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int slash = lower.lastIndexOf('/');
        String file = lower.substring(slash + 1);
        int dot = file.lastIndexOf('.');
        String base = dot > 0 ? file.substring(0, dot) : file;
        return !UNWANTED.contains(dot >= 0 ? file.substring(dot + 1) : "")
                && !file.endsWith("~")
                && !file.equals(".ds_store")
                && !lower.equals("meta-inf/manifest.mf")
                && !lower.startsWith("meta-inf/flagon/")
                && !lower.startsWith("meta-inf/maven/")
                && !base.equals("license") && !base.equals("notice")
                && !base.startsWith("license-") && !base.startsWith("notice-");
    }

    static void write(Map<String, Item> files, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.write(MAGIC);
        data.writeInt(files.size());
        List<String> names = new ArrayList<>(files.keySet());
        byte[][] raw = new byte[BATCH][];
        byte[][] packed = new byte[BATCH][];
        try (Jars open = new Jars()) {
            for (int first = 0; first < names.size(); first += BATCH) {
                int count = Math.min(BATCH, names.size() - first);
                for (int at = 0; at < count; at++) {
                    raw[at] = open.read(files.get(names.get(first + at)));
                }
                deflateBatch(count, names.subList(first, first + count), raw, packed);
                for (int at = 0; at < count; at++) {
                    writeOne(data, names.get(first + at), raw[at], packed[at]);
                }
            }
        }
        data.flush();
    }

    private static void deflateBatch(int count, List<String> names, byte[][] raw,
            byte[][] packed) {
        IntStream.range(0, count).parallel().forEach(at -> {
            packed[at] = alreadyPacked(names.get(at)) ? null : deflate(raw[at]);
        });
    }

    private static boolean alreadyPacked(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0
                && ALREADY_PACKED.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** holds one jar open across its resources */
    private static final class Jars implements AutoCloseable {

        private Path at;
        private ZipFile open;

        byte[] read(Item item) throws IOException {
            if (item.entry() == null) {
                return Files.readAllBytes(item.source());
            }
            if (!item.source().equals(at)) {
                close();
                open = new ZipFile(item.source().toFile());
                at = item.source();
            }
            try (InputStream bytes = open.getInputStream(open.getEntry(item.entry()))) {
                return bytes.readAllBytes();
            }
        }

        @Override
        public void close() throws IOException {
            if (open != null) {
                open.close();
                open = null;
                at = null;
            }
        }
    }

    private static void writeOne(DataOutputStream data, String name, byte[] raw, byte[] packed)
            throws IOException {
        byte[] label = name.getBytes(StandardCharsets.UTF_8);
        boolean smaller = packed != null && packed.length < raw.length;
        data.writeInt(label.length);
        data.write(label);
        data.writeInt(raw.length);
        data.writeInt(smaller ? packed.length : raw.length);
        data.writeByte(smaller ? DEFLATED : STORED);
        data.write(smaller ? packed : raw);
    }

    /** one deflater per thread, reset between resources */
    private static byte[] deflate(byte[] raw) {
        Deflater deflater = DEFLATERS.get();
        deflater.reset();
        ByteArrayOutputStream packed = new ByteArrayOutputStream(raw.length);
        try (DeflaterOutputStream out = new DeflaterOutputStream(packed, deflater)) {
            out.write(raw);
        } catch (IOException impossible) {
            throw new IllegalStateException("a resource would not deflate", impossible);
        }
        return packed.toByteArray();
    }
}
