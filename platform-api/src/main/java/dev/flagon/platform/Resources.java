package dev.flagon.platform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class Resources {

    private static final byte[] MAGIC = "FLGNPK02".getBytes(StandardCharsets.US_ASCII);

    private static final int STORED = 0;

    /** where one resource sits */
    private record At(int offset, int stored, int raw, int packing) {
    }

    private static byte[] pack = new byte[0];
    private static Map<String, At> index = Map.of();

    private Resources() {
    }

    public static void install(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            pack = new byte[0];
            index = Map.of();
            return;
        }
        Map<String, At> read = readIndex(bytes);
        pack = bytes;
        index = read;
    }

    public static int count() {
        return index.size();
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(index.keySet());
    }

    public static boolean has(String name) {
        return index.containsKey(clean(name));
    }

    public static byte[] read(String name) {
        At at = index.get(clean(name));
        if (at == null) {
            return null;
        }
        byte[] bytes = new byte[at.raw()];
        if (at.packing() == STORED) {
            System.arraycopy(pack, at.offset(), bytes, 0, at.raw());
            return bytes;
        }
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(pack, at.offset(), at.stored());
            inflater.inflate(bytes);
        } catch (DataFormatException broken) {
            throw new IllegalStateException("the pack cannot unpack " + name, broken);
        } finally {
            inflater.end();
        }
        return bytes;
    }

    public static InputStream open(String name) {
        At at = index.get(clean(name));
        if (at == null) {
            return null;
        }
        InputStream bytes = new ByteArrayInputStream(pack, at.offset(), at.stored());
        return at.packing() == STORED ? bytes : new InflaterInputStream(bytes);
    }

    public static int sizeOf(String name) {
        At at = index.get(clean(name));
        return at == null ? -1 : at.raw();
    }

    static String clean(String name) {
        if (name == null) {
            return "";
        }
        int start = 0;
        while (start < name.length() && name.charAt(start) == '/') {
            start++;
        }
        return name.substring(start);
    }

    private static Map<String, At> readIndex(byte[] bytes) {
        Cursor cursor = new Cursor(bytes);
        for (byte expected : MAGIC) {
            if (cursor.readByte() != expected) {
                throw new IllegalArgumentException("this is not a flagon resource pack");
            }
        }
        int count = cursor.readInt();
        if (count < 0) {
            throw new IllegalArgumentException("a pack cannot carry " + count + " files");
        }
        Map<String, At> read = new LinkedHashMap<>();
        for (int file = 0; file < count; file++) {
            String name = new String(cursor.readBytes(cursor.readInt()), StandardCharsets.UTF_8);
            int raw = cursor.readInt();
            int stored = cursor.readInt();
            int packing = cursor.readByte();
            int offset = cursor.at();
            if (raw < 0 || stored < 0 || offset + stored > bytes.length) {
                throw new IllegalArgumentException("the pack ends inside " + name);
            }
            read.put(name, new At(offset, stored, raw, packing));
            cursor.skip(stored);
        }
        return read;
    }

    private static final class Cursor {

        private final byte[] bytes;
        private int at;

        Cursor(byte[] bytes) {
            this.bytes = bytes;
        }

        int at() {
            return at;
        }

        byte readByte() {
            if (at >= bytes.length) {
                throw new IllegalArgumentException("the pack ends before its header does");
            }
            return bytes[at++];
        }

        int readInt() {
            return ((readByte() & 0xff) << 24) | ((readByte() & 0xff) << 16)
                    | ((readByte() & 0xff) << 8) | (readByte() & 0xff);
        }

        void skip(int length) {
            at += length;
        }

        byte[] readBytes(int length) {
            if (length < 0 || at + length > bytes.length) {
                throw new IllegalArgumentException("the pack ends inside its header");
            }
            byte[] taken = new byte[length];
            System.arraycopy(bytes, at, taken, 0, length);
            at += length;
            return taken;
        }
    }
}
