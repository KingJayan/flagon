package dev.flagon.pack;

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
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WriteResourcePack {

    static final byte[] MAGIC = "FLGNPK02".getBytes(StandardCharsets.US_ASCII);

    static final int STORED = 0;
    static final int DEFLATED = 1;

    /** where one resource comes from; {@code entry} is null when the source is a plain file */
    record Item(Path source, String entry) {
    }

    private WriteResourcePack() {
    }

    public static void main(String[] args) throws IOException {
        Path given = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        Map<String, Item> files = collect(given);
        Files.createDirectories(pack.getParent());
        try (OutputStream out = Files.newOutputStream(pack)) {
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

    private static boolean wanted(String name) {
        if (name.endsWith(".class")) {
            return false;
        }
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        return !upper.equals("META-INF/MANIFEST.MF")
                && !upper.endsWith(".SF")
                && !upper.endsWith(".DSA")
                && !upper.endsWith(".RSA")
                && !upper.endsWith(".EC");
    }

    static void write(Map<String, Item> files, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.write(MAGIC);
        data.writeInt(files.size());
        try (Jars open = new Jars()) {
            for (Map.Entry<String, Item> file : files.entrySet()) {
                writeOne(data, file.getKey(), open.read(file.getValue()));
            }
        }
        data.flush();
    }

    /** holds one jar open, because the resources of a jar are written one after the other */
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

    private static void writeOne(DataOutputStream data, String name, byte[] raw)
            throws IOException {
        byte[] label = name.getBytes(StandardCharsets.UTF_8);
        byte[] packed = deflate(raw);
        boolean smaller = packed.length < raw.length;
        data.writeInt(label.length);
        data.write(label);
        data.writeInt(raw.length);
        data.writeInt(smaller ? packed.length : raw.length);
        data.writeByte(smaller ? DEFLATED : STORED);
        data.write(smaller ? packed : raw);
    }

    private static byte[] deflate(byte[] raw) throws IOException {
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (DeflaterOutputStream out = new DeflaterOutputStream(packed, deflater)) {
            out.write(raw);
        } finally {
            deflater.end();
        }
        return packed.toByteArray();
    }
}
