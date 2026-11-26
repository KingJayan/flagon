package dev.flagon.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;

/** re-encodes ogg vorbis at a lower quality, through ffmpeg, keeping each result on disk */
final class Vorbis {

    /** a result that saves less than this share of the original is not worth its loss */
    static final double KEEP_BELOW = 0.85;

    /** turns one vorbis file into another */
    interface Encoder {
        void encode(Path from, Path to) throws IOException, InterruptedException;
    }

    private final Encoder encoder;
    private final Path cache;

    Vorbis(Encoder encoder, Path cache) {
        this.encoder = encoder;
        this.cache = cache;
    }

    static Encoder ffmpeg(int quality) {
        return (from, to) -> {
            Process run;
            try {
                run = new ProcessBuilder("ffmpeg", "-nostdin", "-v", "error", "-y",
                        "-i", from.toString(), "-map", "0:a", "-map_metadata", "-1",
                        "-c:a", "libvorbis", "-q:a", Integer.toString(quality),
                        "-f", "ogg", to.toString())
                        .redirectErrorStream(true).start();
            } catch (IOException missing) {
                throw new IOException("ffmpeg is missing; install it to transcode audio", missing);
            }
            String said = new String(run.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (run.waitFor() != 0) {
                throw new IOException("ffmpeg could not transcode " + from + ": " + said.strip());
            }
        };
    }

    /** whether these bytes open an ogg stream whose first packet is a vorbis header */
    static boolean is(byte[] file) {
        if (file.length < 27 || file[0] != 'O' || file[1] != 'g' || file[2] != 'g'
                || file[3] != 'S') {
            return false;
        }
        int packet = 27 + (file[26] & 0xff);
        if (file.length < packet + 7 || file[packet] != 1) {
            return false;
        }
        return new String(file, packet + 1, 6, StandardCharsets.US_ASCII).equals("vorbis");
    }

    /**
     * @param key names the encoder's settings, so a change of quality misses the cache
     * @return the smaller file, or the original when the saving is too small
     */
    byte[] shrink(byte[] original, String key) throws IOException, InterruptedException {
        Path done = cache.resolve(HexFormat.of().formatHex(Hashes.sha256(original)) + "-" + key
                + ".ogg");
        if (!Files.exists(done)) {
            Files.createDirectories(cache);
            Path from = Files.createTempFile(cache, "in", ".ogg");
            Path to = Files.createTempFile(cache, "out", ".ogg");
            try {
                Files.write(from, original);
                encoder.encode(from, to);
                Files.move(to, done, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(from);
                Files.deleteIfExists(to);
            }
        }
        byte[] smaller = Files.readAllBytes(done);
        return smaller.length > 0 && smaller.length < original.length * KEEP_BELOW
                ? smaller : original;
    }
}
