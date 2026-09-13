package javax.imageio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

final class Png {

    private static final byte[] SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n',
    };

    private static final int GREY = 0;
    private static final int RGB = 2;
    private static final int PALETTE = 3;
    private static final int GREY_ALPHA = 4;
    private static final int RGBA = 6;

    private Png() {
    }

    static BufferedImage read(InputStream from) throws IOException {
        byte[] file = readAll(from);
        requireSignature(file);

        int width = 0;
        int height = 0;
        int depth = 0;
        int colorType = 0;
        byte[] palette = null;
        byte[] paletteAlpha = null;
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        int at = SIGNATURE.length;
        while (at + 8 <= file.length) {
            int length = intAt(file, at);
            String kind = new String(file, at + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int body = at + 8;
            if (length < 0 || body + length > file.length) {
                throw new IOException("a png chunk says it is " + length + " bytes, which is "
                        + "more than the file has left");
            }
            switch (kind) {
                case "IHDR" -> {
                    width = intAt(file, body);
                    height = intAt(file, body + 4);
                    depth = file[body + 8] & 0xFF;
                    colorType = file[body + 9] & 0xFF;
                    if (file[body + 12] != 0) {
                        throw new IOException("an interlaced png is not read here");
                    }
                }
                case "PLTE" -> {
                    palette = new byte[length];
                    System.arraycopy(file, body, palette, 0, length);
                }
                case "tRNS" -> {
                    paletteAlpha = new byte[length];
                    System.arraycopy(file, body, paletteAlpha, 0, length);
                }
                case "IDAT" -> compressed.write(file, body, length);
                default -> {
                }
            }
            if ("IEND".equals(kind)) {
                break;
            }
            at = body + length + 4;
        }

        if (width <= 0 || height <= 0) {
            throw new IOException("this is not a png: it carries no header");
        }
        if (depth != 8) {
            throw new IOException("a png of " + depth + " bits a channel is not read here; "
                    + "eight is what a game writes");
        }
        byte[] raw = inflate(compressed.toByteArray());
        return unfilter(raw, width, height, colorType, palette, paletteAlpha);
    }

    static void write(BufferedImage image, OutputStream to) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] rows = new byte[height * (1 + width * 4)];
        int out = 0;
        for (int y = 0; y < height; y++) {
            rows[out++] = 0;
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                rows[out++] = (byte) (argb >> 16);
                rows[out++] = (byte) (argb >> 8);
                rows[out++] = (byte) argb;
                rows[out++] = (byte) (argb >>> 24);
            }
        }

        to.write(SIGNATURE);
        byte[] header = new byte[13];
        putInt(header, 0, width);
        putInt(header, 4, height);
        header[8] = 8;
        header[9] = (byte) RGBA;
        writeChunk(to, "IHDR", header);
        writeChunk(to, "IDAT", deflate(rows));
        writeChunk(to, "IEND", new byte[0]);
        to.flush();
    }

    private static BufferedImage unfilter(byte[] raw, int width, int height, int colorType,
                                          byte[] palette, byte[] paletteAlpha)
            throws IOException {
        int channels = switch (colorType) {
            case GREY -> 1;
            case RGB -> 3;
            case PALETTE -> 1;
            case GREY_ALPHA -> 2;
            case RGBA -> 4;
            default -> throw new IOException("a png colour type of " + colorType + " is unknown");
        };
        int stride = width * channels;
        if (raw.length < height * (stride + 1)) {
            throw new IOException("a png says it is " + width + "x" + height
                    + ", which is more than its pixels fill");
        }
        byte[] line = new byte[stride];
        byte[] above = new byte[stride];
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int at = 0;
        for (int y = 0; y < height; y++) {
            int filter = raw[at++] & 0xFF;
            System.arraycopy(raw, at, line, 0, stride);
            at += stride;
            undo(filter, line, above, channels);
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, pixel(line, x * channels, colorType, palette, paletteAlpha));
            }
            byte[] swap = above;
            above = line;
            line = swap;
        }
        return image;
    }

    private static int pixel(byte[] line, int at, int colorType, byte[] palette,
                             byte[] paletteAlpha) {
        return switch (colorType) {
            case GREY -> {
                int grey = line[at] & 0xFF;
                yield 0xFF000000 | grey << 16 | grey << 8 | grey;
            }
            case GREY_ALPHA -> {
                int grey = line[at] & 0xFF;
                yield (line[at + 1] & 0xFF) << 24 | grey << 16 | grey << 8 | grey;
            }
            case RGB -> 0xFF000000 | (line[at] & 0xFF) << 16
                    | (line[at + 1] & 0xFF) << 8 | (line[at + 2] & 0xFF);
            case RGBA -> (line[at + 3] & 0xFF) << 24 | (line[at] & 0xFF) << 16
                    | (line[at + 1] & 0xFF) << 8 | (line[at + 2] & 0xFF);
            default -> {
                int index = line[at] & 0xFF;
                int alpha = paletteAlpha != null && index < paletteAlpha.length
                        ? paletteAlpha[index] & 0xFF : 0xFF;
                yield alpha << 24 | (palette[index * 3] & 0xFF) << 16
                        | (palette[index * 3 + 1] & 0xFF) << 8
                        | (palette[index * 3 + 2] & 0xFF);
            }
        };
    }

    private static void undo(int filter, byte[] line, byte[] above, int channels) {
        switch (filter) {
            case 0 -> {
            }
            case 1 -> {
                for (int at = channels; at < line.length; at++) {
                    line[at] += line[at - channels];
                }
            }
            case 2 -> {
                for (int at = 0; at < line.length; at++) {
                    line[at] += above[at];
                }
            }
            case 3 -> {
                for (int at = 0; at < line.length; at++) {
                    int left = at >= channels ? line[at - channels] & 0xFF : 0;
                    line[at] += (byte) ((left + (above[at] & 0xFF)) / 2);
                }
            }
            case 4 -> {
                for (int at = 0; at < line.length; at++) {
                    int left = at >= channels ? line[at - channels] & 0xFF : 0;
                    int up = above[at] & 0xFF;
                    int corner = at >= channels ? above[at - channels] & 0xFF : 0;
                    line[at] += (byte) nearest(left, up, corner);
                }
            }
            default -> throw new IllegalArgumentException("a png row filter of " + filter
                    + " is unknown");
        }
    }

    private static int nearest(int left, int up, int corner) {
        int guess = left + up - corner;
        int toLeft = Math.abs(guess - left);
        int toUp = Math.abs(guess - up);
        int toCorner = Math.abs(guess - corner);
        if (toLeft <= toUp && toLeft <= toCorner) {
            return left;
        }
        return toUp <= toCorner ? up : corner;
    }

    private static byte[] inflate(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        byte[] chunk = new byte[16384];
        try {
            while (!inflater.finished()) {
                int read = inflater.inflate(chunk);
                if (read == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                out.write(chunk, 0, read);
            }
        } catch (java.util.zip.DataFormatException broken) {
            throw new IOException("the png pixels do not unpack: " + broken.getMessage(), broken);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 2 + 64);
        byte[] chunk = new byte[16384];
        while (!deflater.finished()) {
            out.write(chunk, 0, deflater.deflate(chunk));
        }
        deflater.end();
        return out.toByteArray();
    }

    private static void writeChunk(OutputStream to, String kind, byte[] body) throws IOException {
        byte[] name = kind.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] length = new byte[4];
        putInt(length, 0, body.length);
        to.write(length);
        to.write(name);
        to.write(body);

        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(body);
        byte[] check = new byte[4];
        putInt(check, 0, (int) crc.getValue());
        to.write(check);
    }

    private static void requireSignature(byte[] file) throws IOException {
        if (file.length < SIGNATURE.length) {
            throw new IOException("this file is too short to be a png");
        }
        for (int at = 0; at < SIGNATURE.length; at++) {
            if (file[at] != SIGNATURE[at]) {
                throw new IOException("this file is not a png: only png is read here");
            }
        }
    }

    private static byte[] readAll(InputStream from) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[16384];
        int read;
        while ((read = from.read(chunk)) > 0) {
            out.write(chunk, 0, read);
        }
        return out.toByteArray();
    }

    private static int intAt(byte[] bytes, int at) {
        return (bytes[at] & 0xFF) << 24 | (bytes[at + 1] & 0xFF) << 16
                | (bytes[at + 2] & 0xFF) << 8 | (bytes[at + 3] & 0xFF);
    }

    private static void putInt(byte[] bytes, int at, int value) {
        bytes[at] = (byte) (value >>> 24);
        bytes[at + 1] = (byte) (value >>> 16);
        bytes[at + 2] = (byte) (value >>> 8);
        bytes[at + 3] = (byte) value;
    }
}
