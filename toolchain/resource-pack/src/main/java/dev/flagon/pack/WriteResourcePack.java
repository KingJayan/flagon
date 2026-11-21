package dev.flagon.pack;

import java.io.BufferedOutputStream;
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
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WriteResourcePack {

    static final byte[] MAGIC = "FLGNPK03".getBytes(StandardCharsets.US_ASCII);

    private static final int BUFFER = 1 << 16;

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
        // the whole pack is gzipped, and the browser unzips it natively as it arrives; each
        // resource then sits in it as is, ready to read
        try (OutputStream out = new GZIPOutputStream(
                new BufferedOutputStream(Files.newOutputStream(pack), BUFFER), BUFFER)) {
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
                readTree(source, given, files);
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
        List<Path> folders = new ArrayList<>();
        try (var listing = Files.list(given)) {
            for (Path entry : listing.sorted().toList()) {
                if (entry.toString().endsWith(".jar")) {
                    jars.add(entry);
                } else if (Files.isDirectory(entry)) {
                    folders.add(entry);
                }
            }
        }
        if (jars.isEmpty()) {
            return List.of(given);
        }
        // loose files beside the jars, such as game assets, after them so a jar keeps its names
        jars.addAll(folders);
        return jars;
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

    private static void readTree(Path root, Path base, Map<String, Item> files)
            throws IOException {
        try (var tree = Files.walk(root)) {
            for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
                String name = base.relativize(file).toString().replace('\\', '/');
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
                // log4j 2.0 loads every class this lists, used or not; without it, log4j scans
                && !lower.equals("org/apache/logging/log4j/core/config/plugins/log4j2plugins.dat")
                && !lower.startsWith("meta-inf/flagon/")
                && !lower.startsWith("meta-inf/maven/")
                && !base.equals("license") && !base.equals("notice")
                && !base.startsWith("license-") && !base.startsWith("notice-");
    }

    static void write(Map<String, Item> files, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.write(MAGIC);
        data.writeInt(files.size());
        try (Jars open = new Jars()) {
            for (Map.Entry<String, Item> file : files.entrySet()) {
                byte[] label = file.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] raw = open.read(file.getValue());
                data.writeInt(label.length);
                data.write(label);
                data.writeInt(raw.length);
                data.write(raw);
            }
        }
        data.flush();
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
}
