package dev.flagon.pack;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WriteResourcePack {

    static final byte[] MAGIC = "FLGNPK01".getBytes(StandardCharsets.US_ASCII);

    private WriteResourcePack() {
    }

    public static void main(String[] args) throws IOException {
        Path given = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        Map<String, byte[]> files = collect(given);
        Files.createDirectories(pack.getParent());
        try (OutputStream out = Files.newOutputStream(pack)) {
            write(files, out);
        }
        System.out.println(files.isEmpty()
                ? "no resources in " + given + ", so the bundle carries an empty pack"
                : "packed " + files.size() + " resources from " + given + " into " + pack);
    }

    static Map<String, byte[]> collect(Path given) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
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

    private static void readJar(Path jar, Map<String, byte[]> files) throws IOException {
        try (ZipFile open = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : open.stream().sorted(
                    (left, right) -> left.getName().compareTo(right.getName())).toList()) {
                if (entry.isDirectory() || !wanted(entry.getName())) {
                    continue;
                }
                try (var bytes = open.getInputStream(entry)) {
                    files.putIfAbsent(entry.getName(), bytes.readAllBytes());
                }
            }
        }
    }

    private static void readTree(Path root, Map<String, byte[]> files) throws IOException {
        try (var tree = Files.walk(root)) {
            for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
                String name = root.relativize(file).toString().replace('\\', '/');
                if (wanted(name)) {
                    files.putIfAbsent(name, Files.readAllBytes(file));
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

    static void write(Map<String, byte[]> files, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.write(MAGIC);
        data.writeInt(files.size());
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            byte[] name = file.getKey().getBytes(StandardCharsets.UTF_8);
            data.writeInt(name.length);
            data.write(name);
            data.writeInt(file.getValue().length);
        }
        for (byte[] bytes : files.values()) {
            data.write(bytes);
        }
        data.flush();
    }
}
