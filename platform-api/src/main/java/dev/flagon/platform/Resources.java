package dev.flagon.platform;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class Resources {

    private static final byte[] MAGIC = "FLGNPK04".getBytes(StandardCharsets.US_ASCII);

    /**
     * the pack's bytes where they already are: a browser keeps them in the buffer they arrived
     * in, so only a resource that is read is copied
     */
    public interface Source {

        int length();

        byte get(int at);

        void copy(int from, byte[] into, int to, int count);
    }

    /**
     * hands over a resource kept beside the pack, by the hex sha-256 it is named by. it must not
     * wait: a read can hold a lock another thread enters without suspending
     */
    public interface Fetcher {

        /** @return the file, or null while it has not arrived */
        byte[] fetch(String hash);
    }

    private static final int INSIDE = 0;
    private static final int APART = 1;
    private static final int HASH = 32;

    /** where one resource sits: at an offset in the pack, or apart under a hash */
    private record At(int offset, int length, String apart) {
    }

    private static final Source EMPTY = of(new byte[0]);
    private static final Fetcher NOWHERE = hash -> null;

    private static Source pack = EMPTY;
    private static Fetcher fetcher = NOWHERE;
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
        fetcher = NOWHERE;
        index = Map.of();
    }

    public static void install(byte[] bytes) {
        install(of(bytes));
    }

    public static void install(Source source) {
        install(source, NOWHERE);
    }

    public static void install(Source source, Fetcher apart) {
        if (source.length() == 0) {
            clear();
            return;
        }
        Map<String, At> read = readIndex(source);
        pack = source;
        fetcher = apart;
        index = read;
    }

    /** the hashes of the resources kept beside the pack */
    public static Set<String> apart() {
        Set<String> hashes = new LinkedHashSet<>();
        for (At at : index.values()) {
            if (at.apart() != null) {
                hashes.add(at.apart());
            }
        }
        return hashes;
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
        return at == null ? null : bytesOf(at);
    }

    private static byte[] bytesOf(At at) {
        if (at.apart() != null) {
            byte[] fetched = fetcher.fetch(at.apart());
            return fetched != null && fetched.length == at.length() ? fetched : null;
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
        if (at.apart() == null) {
            pack.copy(at.offset(), into, 0, at.length());
            return at.length();
        }
        byte[] fetched = bytesOf(at);
        if (fetched == null) {
            return -1;
        }
        System.arraycopy(fetched, 0, into, 0, fetched.length);
        return fetched.length;
    }

    /** a resource kept apart is fetched on the first read, so opening one only checks it */
    public static InputStream open(String name) {
        String clean = clean(name);
        At at = index.get(clean);
        if (at == null) {
            return null;
        }
        return at.apart() == null ? new ByteArrayInputStream(bytesOf(at)) : new Apart(clean, at);
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
            int place = cursor.readByte();
            if (length < 0 || place != INSIDE && place != APART) {
                throw new IllegalArgumentException("the pack mislabels " + name);
            }
            if (place == APART) {
                read.put(name, new At(0, length, hex(cursor.readBytes(HASH))));
                continue;
            }
            int offset = cursor.at();
            if (offset + length > source.length()) {
                throw new IllegalArgumentException("the pack ends inside " + name);
            }
            read.put(name, new At(offset, length, null));
            cursor.skip(length);
        }
        return read;
    }

    private static final class Apart extends InputStream {

        private final String name;
        private final At at;
        private ByteArrayInputStream bytes;

        Apart(String name, At at) {
            this.name = name;
            this.at = at;
        }

        private ByteArrayInputStream bytes() throws IOException {
            if (bytes == null) {
                byte[] fetched = bytesOf(at);
                if (fetched == null) {
                    throw new IOException(name + " has not loaded yet");
                }
                bytes = new ByteArrayInputStream(fetched);
            }
            return bytes;
        }

        @Override
        public int read() throws IOException {
            return bytes().read();
        }

        @Override
        public int read(byte[] into, int offset, int length) throws IOException {
            return bytes().read(into, offset, length);
        }

        @Override
        public long skip(long count) throws IOException {
            return bytes().skip(count);
        }

        @Override
        public int available() throws IOException {
            return bytes().available();
        }
    }

    private static String hex(byte[] bytes) {
        char[] digits = new char[bytes.length * 2];
        for (int at = 0; at < bytes.length; at++) {
            digits[at * 2] = Character.forDigit((bytes[at] >> 4) & 0xf, 16);
            digits[at * 2 + 1] = Character.forDigit(bytes[at] & 0xf, 16);
        }
        return new String(digits);
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
