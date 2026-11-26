package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public final class Scanner implements java.io.Closeable {

    private final String text;
    private String delimiter = "\\s+";
    private int at;
    private int skipEnd;

    public Scanner(String source) {
        this.text = source == null ? "" : source;
    }

    public Scanner(Readable source) {
        this.text = readAll(source);
    }

    public Scanner(InputStream source) {
        this(new InputStreamReader(source, StandardCharsets.UTF_8));
    }

    public Scanner useDelimiter(String pattern) {
        delimiter = pattern;
        return this;
    }

    public boolean hasNext() {
        return peek() != null;
    }

    public String next() {
        String token = peek();
        if (token == null) {
            throw new NoSuchElementException();
        }
        at = skipEnd;
        return token;
    }

    public boolean hasNextInt() {
        String token = peek();
        if (token == null) {
            return false;
        }
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    public int nextInt() {
        return Integer.parseInt(next());
    }

    public boolean hasNextLong() {
        String token = peek();
        if (token == null) {
            return false;
        }
        try {
            Long.parseLong(token);
            return true;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    public long nextLong() {
        return Long.parseLong(next());
    }

    public String nextLine() {
        int end = text.indexOf('\n', at);
        String line = end < 0 ? text.substring(at) : text.substring(at, end);
        at = end < 0 ? text.length() : end + 1;
        return line;
    }

    public boolean hasNextLine() {
        return at < text.length();
    }

    @Override
    public void close() {
    }

    private String peek() {
        String rest = text.substring(at);
        String[] parts = rest.split(delimiter, 2);
        int first = 0;
        while (first < parts.length && parts[first].isEmpty()) {
            first++;
        }
        if (first >= parts.length) {
            skipEnd = text.length();
            return null;
        }
        String token = parts[first];
        int found = rest.indexOf(token);
        skipEnd = at + found + token.length();
        return token;
    }

    private static String readAll(Readable source) {
        StringBuilder all = new StringBuilder();
        CharBuffer room = CharBuffer.allocate(4096);
        try {
            while (source.read(room) >= 0) {
                room.flip();
                all.append(room);
                room.clear();
            }
        } catch (IOException stopped) {
            return all.toString();
        }
        return all.toString();
    }
}
