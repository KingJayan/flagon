package dev.flagon.runtime.net;

import java.io.IOException;
import java.io.InputStream;

/** a body as long as its {@code Content-Length} says, and not one byte longer */
final class BoundedStream extends InputStream {

    private final InputStream source;
    private long left;

    BoundedStream(InputStream source, long length) {
        this.source = source;
        this.left = length;
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int count = read(one, 0, 1);
        return count < 0 ? -1 : one[0] & 0xFF;
    }

    @Override
    public int read(byte[] into, int offset, int length) throws IOException {
        if (left <= 0) {
            return -1;
        }
        if (length == 0) {
            return 0;
        }
        int count = source.read(into, offset, (int) Math.min(length, left));
        if (count < 0) {
            left = 0;
            return -1;
        }
        left -= count;
        return count;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(left, source.available());
    }

    @Override
    public void close() throws IOException {
        left = 0;
        source.close();
    }
}
