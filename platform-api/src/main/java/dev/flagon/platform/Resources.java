package dev.flagon.platform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Resources {

    private static final byte[] MAGIC = "FLGNPK01".getBytes(StandardCharsets.US_ASCII);

    private static byte[] pack = new byte[0];
    private static Map<String, int[]> index = Map.of();

    private Resources() {
    }

    public static void install(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            pack = new byte[0];
            index = Map.of();
            return;
        }
        Map<String, int[]> read = readIndex(bytes);
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
        int[] at = index.get(clean(name));
        if (at == null) {
            return null;
        }
        byte[] bytes = new byte[at[1]];
        System.arraycopy(pack, at[0], bytes, 0, at[1]);
        return bytes;
    }

    public static InputStream open(String name) {
        int[] at = index.get(clean(name));
        return at == null ? null : new ByteArrayInputStream(pack, at[0], at[1]);
    }

    public static int sizeOf(String name) {
        int[] at = index.get(clean(name));
        return at == null ? -1 : at[1];
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

    private static Map<String, int[]> readIndex(byte[] bytes) {
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
        Map<String, int[]> read = new LinkedHashMap<>();
        String[] names = new String[count];
        int[] lengths = new int[count];
        for (int at = 0; at < count; at++) {
            int nameLength = cursor.readInt();
            names[at] = new String(cursor.readBytes(nameLength), StandardCharsets.UTF_8);
            lengths[at] = cursor.readInt();
        }
        int offset = cursor.at();
        for (int at = 0; at < count; at++) {
            if (lengths[at] < 0 || offset + lengths[at] > bytes.length) {
                throw new IllegalArgumentException("the pack ends inside " + names[at]);
            }
            read.put(names[at], new int[] {offset, lengths[at]});
            offset += lengths[at];
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
