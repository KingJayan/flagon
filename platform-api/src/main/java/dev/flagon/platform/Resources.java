package dev.flagon.platform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Resources {

    private static final byte[] MAGIC = "FLGNPK03".getBytes(StandardCharsets.US_ASCII);

    /**
     * the pack's bytes where they already are: a browser keeps them in the buffer they arrived
     * in, so only a resource that is read is copied
     */
    public interface Source {

        int length();

        byte get(int at);

        void copy(int from, byte[] into, int to, int count);
    }

    /** where one resource sits */
    private record At(int offset, int length) {
    }

    private static final Source EMPTY = of(new byte[0]);

    private static Source pack = EMPTY;
    private static Map<String, At> index = Map.of();

    private Resources() {
    }

    public static Source of(byte[] bytes) {
        return new Source() {
            @Override
            public int length() {
                return bytes.length;
            }

            @Override
            public byte get(int at) {
                return bytes[at];
            }

            @Override
            public void copy(int from, byte[] into, int to, int count) {
                System.arraycopy(bytes, from, into, to, count);
            }
        };
    }

    /** the pack is gone; every lookup finds nothing */
    public static void clear() {
        pack = EMPTY;
        index = Map.of();
    }

    public static void install(byte[] bytes) {
        install(of(bytes));
    }

    public static void install(Source source) {
        if (source.length() == 0) {
            clear();
            return;
        }
        Map<String, At> read = readIndex(source);
        pack = source;
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

    /** @return a copy the reader may change */
    public static byte[] read(String name) {
        At at = index.get(clean(name));
        if (at == null) {
            return null;
        }
        byte[] bytes = new byte[at.length()];
        pack.copy(at.offset(), bytes, 0, at.length());
        return bytes;
    }

    /** @return bytes written into {@code into}, or -1 when the pack has no such resource */
    public static int readInto(String name, byte[] into) {
        String clean = clean(name);
        At at = index.get(clean);
        if (at == null) {
            return -1;
        }
        if (into.length < at.length()) {
            throw new IllegalArgumentException(clean + " is " + at.length() + " bytes, and the "
                    + "array holds " + into.length);
        }
        pack.copy(at.offset(), into, 0, at.length());
        return at.length();
    }

    public static InputStream open(String name) {
        byte[] bytes = read(name);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    public static int sizeOf(String name) {
        At at = index.get(clean(name));
        return at == null ? -1 : at.length();
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

    private static Map<String, At> readIndex(Source source) {
        Cursor cursor = new Cursor(source);
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
            int length = cursor.readInt();
            int offset = cursor.at();
            if (length < 0 || offset + length > source.length()) {
                throw new IllegalArgumentException("the pack ends inside " + name);
            }
            read.put(name, new At(offset, length));
            cursor.skip(length);
        }
        return read;
    }

    private static final class Cursor {

        private final Source source;
        private int at;

        Cursor(Source source) {
            this.source = source;
        }

        int at() {
            return at;
        }

        byte readByte() {
            if (at >= source.length()) {
                throw new IllegalArgumentException("the pack ends before its header does");
            }
            return source.get(at++);
        }

        int readInt() {
            return ((readByte() & 0xff) << 24) | ((readByte() & 0xff) << 16)
                    | ((readByte() & 0xff) << 8) | (readByte() & 0xff);
        }

        void skip(int length) {
            at += length;
        }

        byte[] readBytes(int length) {
            if (length < 0 || at + length > source.length()) {
                throw new IllegalArgumentException("the pack ends inside its header");
            }
            byte[] taken = new byte[length];
            source.copy(at, taken, 0, length);
            at += length;
            return taken;
        }
    }
}
