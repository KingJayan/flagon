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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * packs a bundle's resources: {@code <resources> <pack> [<cache>] [--vorbis-quality=N]
 * [--apart-from=BYTES] [--apart=GLOB,...]}, where the cache keeps transcoded audio between builds.
 * resources at least {@code --apart-from} bytes, or named by an {@code --apart} glob, go beside
 * the pack in {@code <name>.files/}, named by their sha-256, and the page fetches them after the
 * program starts. a glob also matches a launcher asset object by its names in the asset index.
 */
public final class WriteResourcePack {

    static final byte[] MAGIC = "FLGNPK04".getBytes(StandardCharsets.US_ASCII);

    static final int INSIDE = 0;
    static final int APART = 1;
    /** added to the place: the file sits beside the jars, so java.io.File finds it as well */
    static final int SHIPPED = 2;

    private static final int BUFFER = 1 << 16;

    /** where one resource comes from; a shipped one sat in a folder beside the jars */
    record Item(Path source, String entry, boolean shipped) {
    }

    /** how to pack; a null quality leaves audio as it is, and an apartFrom of 0 keeps all in */
    record Options(Integer vorbisQuality, int apartFrom, List<Pattern> apart, Path cache) {

        static Options parse(String[] args) {
            Integer quality = null;
            int apartFrom = 0;
            List<Pattern> apart = new ArrayList<>();
            Path cache = null;
            for (String arg : args) {
                String value = arg.substring(arg.indexOf('=') + 1);
                if (arg.startsWith("--vorbis-quality=")) {
                    quality = Integer.valueOf(value);
                } else if (arg.startsWith("--apart-from=")) {
                    apartFrom = Integer.parseInt(value);
                } else if (arg.startsWith("--apart=")) {
                    for (String glob : value.split(",")) {
                        if (!glob.isBlank()) {
                            apart.add(glob(glob.trim()));
                        }
                    }
                } else if (!arg.startsWith("--")) {
                    cache = Path.of(arg);
                } else {
                    throw new IllegalArgumentException("unknown option " + arg);
                }
            }
            if (quality != null && cache == null) {
                throw new IllegalArgumentException("--vorbis-quality needs a cache folder");
            }
            return new Options(quality, apartFrom, List.copyOf(apart), cache);
        }
    }

    /** {@code **} matches across folders, {@code *} and {@code ?} within one */
    static Pattern glob(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int at = 0; at < glob.length(); at++) {
            char next = glob.charAt(at);
            if (glob.startsWith("**", at)) {
                regex.append(".*");
                at++;
            } else if (next == '*') {
                regex.append("[^/]*");
            } else if (next == '?') {
                regex.append("[^/]");
            } else {
                regex.append(Pattern.quote(String.valueOf(next)));
            }
        }
        return Pattern.compile(regex.toString());
    }

    private WriteResourcePack() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Path given = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        Options options = Options.parse(List.of(args).subList(2, args.length)
                .toArray(String[]::new));
        Map<String, Item> files = collect(given);
        Files.createDirectories(pack.getParent());
        Map<String, byte[]> shrunk = options.vorbisQuality() == null ? Map.of()
                : shrink(files, new Vorbis(Vorbis.ffmpeg(options.vorbisQuality()),
                        options.cache()), "vorbis-q" + options.vorbisQuality());
        Path apart = apartOf(pack);
        clear(apart);
        // the whole pack is gzipped, and the browser unzips it natively as it arrives; each
        // resource then sits in it as is, ready to read
        try (OutputStream out = new GZIPOutputStream(
                new BufferedOutputStream(Files.newOutputStream(pack), BUFFER), BUFFER)) {
            write(files, shrunk, options, apart, out);
        }
        System.out.println(files.isEmpty()
                ? "no resources in " + given + ", so the bundle carries an empty pack"
                : "packed " + files.size() + " resources from " + given + " into " + pack);
    }

    /** where the resources kept out of the pack go */
    static Path apartOf(Path pack) {
        String name = pack.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return pack.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".files");
    }

    private static void clear(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (var tree = Files.walk(folder)) {
            for (Path file : tree.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        }
    }

    /** each vorbis file that re-encodes smaller, by name */
    static Map<String, byte[]> shrink(Map<String, Item> files, Vorbis vorbis, String key)
            throws IOException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        try {
            Map<String, Future<byte[]>> pending = new LinkedHashMap<>();
            for (Map.Entry<String, Item> file : files.entrySet()) {
                // a loose file's content decides, as a launcher names its assets by hash alone
                Item item = file.getValue();
                if (item.entry() != null
                        && !file.getKey().toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                    continue;
                }
                pending.put(file.getKey(), pool.submit(() -> {
                    byte[] original;
                    try (Jars open = new Jars()) {
                        original = open.read(item);
                    }
                    if (!Vorbis.is(original)) {
                        return null;
                    }
                    byte[] smaller = vorbis.shrink(original, key);
                    return smaller == original ? null : smaller;
                }));
            }
            Map<String, byte[]> shrunk = new LinkedHashMap<>();
            for (Map.Entry<String, Future<byte[]>> result : pending.entrySet()) {
                byte[] smaller = result.getValue().get();
                if (smaller != null) {
                    shrunk.put(result.getKey(), smaller);
                }
            }
            return shrunk;
        } catch (ExecutionException failed) {
            if (failed.getCause() instanceof IOException io) {
                throw io;
            }
            throw new IOException(failed.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    static Map<String, Item> collect(Path given) throws IOException {
        Map<String, Item> files = new LinkedHashMap<>();
        for (Path source : sourcesIn(given)) {
            if (Files.isDirectory(source)) {
                readTree(source, given, !source.equals(given), files);
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
                files.putIfAbsent(entry.getName(), new Item(jar, entry.getName(), false));
            }
        }
    }

    private static void readTree(Path root, Path base, boolean shipped, Map<String, Item> files)
            throws IOException {
        try (var tree = Files.walk(root)) {
            for (Path file : tree.filter(Files::isRegularFile).sorted().toList()) {
                String name = base.relativize(file).toString().replace('\\', '/');
                if (wanted(name)) {
                    files.putIfAbsent(name, new Item(file, null, shipped));
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
        write(files, Map.of(), new Options(null, 0, List.of(), null), null, out);
    }

    /** one resource as it goes into the pack */
    private record Packed(String name, Item item, byte[] raw, boolean apart) {
    }

    /** resources the options keep apart go to {@code apart} */
    static void write(Map<String, Item> files, Map<String, byte[]> replaced, Options options,
                      Path apart, OutputStream out) throws IOException {
        Map<String, byte[]> read = new LinkedHashMap<>();
        try (Jars open = new Jars()) {
            for (Map.Entry<String, Item> file : files.entrySet()) {
                byte[] raw = replaced.get(file.getKey());
                read.put(file.getKey(), raw == null ? open.read(file.getValue()) : raw);
            }
        }
        Map<String, List<String>> indexed = assetNames(read);
        List<Packed> packed = new ArrayList<>();
        for (Map.Entry<String, Item> file : files.entrySet()) {
            byte[] raw = read.get(file.getKey());
            packed.add(new Packed(file.getKey(), file.getValue(), raw,
                    isApart(file.getKey(), raw, indexed, options)));
        }
        // gzip finds repeats only 32 KiB back, so files of one kind go together. the page
        // fetches the ones kept apart in pack order, so the smallest come in first
        packed.sort(Comparator.comparing(Packed::apart)
                .thenComparingInt(file -> file.apart() ? file.raw().length : kindOf(file.raw()))
                .thenComparing(Packed::name));
        DataOutputStream data = new DataOutputStream(out);
        data.write(MAGIC);
        data.writeInt(packed.size());
        for (Packed file : packed) {
            byte[] label = file.name().getBytes(StandardCharsets.UTF_8);
            byte[] raw = file.raw();
            data.writeInt(label.length);
            data.write(label);
            data.writeInt(raw.length);
            int shipped = file.item().shipped() ? SHIPPED : 0;
            if (file.apart()) {
                byte[] hash = Hashes.sha256(raw);
                Files.createDirectories(apart);
                Files.write(apart.resolve(HexFormat.of().formatHex(hash)), raw);
                data.writeByte(APART | shipped);
                data.write(hash);
            } else {
                data.writeByte(INSIDE | shipped);
                data.write(raw);
            }
        }
        data.flush();
    }

    private static boolean isApart(String name, byte[] raw, Map<String, List<String>> indexed,
                                   Options options) {
        if (options.apartFrom() > 0 && raw.length >= options.apartFrom()) {
            return true;
        }
        List<String> names = new ArrayList<>(List.of(name));
        names.addAll(indexed.getOrDefault(name, List.of()));
        return options.apart().stream()
                .anyMatch(glob -> names.stream().anyMatch(one -> glob.matcher(one).matches()));
    }

    private static final Pattern INDEX = Pattern.compile("(?:.*/)?assets/indexes/[^/]+\\.json");
    private static final Pattern OBJECT =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"hash\"\\s*:\\s*\"([0-9a-f]{40})\"");

    /** the names a launcher asset index gives each object under assets/objects/, by entry */
    private static Map<String, List<String>> assetNames(Map<String, byte[]> read) {
        Map<String, List<String>> names = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> file : read.entrySet()) {
            if (!INDEX.matcher(file.getKey()).matches()) {
                continue;
            }
            String objects = file.getKey().substring(0, file.getKey().lastIndexOf("indexes/"))
                    + "objects/";
            Matcher object = OBJECT.matcher(new String(file.getValue(), StandardCharsets.UTF_8));
            while (object.find()) {
                String hash = object.group(2);
                names.computeIfAbsent(objects + hash.substring(0, 2) + "/" + hash,
                        entry -> new ArrayList<>()).add(object.group(1));
            }
        }
        return names;
    }

    /** vorbis, then png, then the rest */
    private static int kindOf(byte[] raw) {
        if (startsWith(raw, "OggS")) {
            return 0;
        }
        return startsWith(raw, "\u0089PNG") ? 1 : 2;
    }

    private static boolean startsWith(byte[] raw, String magic) {
        if (raw.length < magic.length()) {
            return false;
        }
        for (int at = 0; at < magic.length(); at++) {
            if ((raw[at] & 0xff) != magic.charAt(at)) {
                return false;
            }
        }
        return true;
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
