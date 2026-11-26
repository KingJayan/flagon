package dev.flagon.runtime.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** a body sent as {@code Transfer-Encoding: chunked}, handed back as the bytes it stands for */
final class ChunkedStream extends InputStream {

    private final InputStream source;
    private int left;
    private boolean ended;

    ChunkedStream(InputStream source) {
        this.source = source;
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int count = read(one, 0, 1);
        return count < 0 ? -1 : one[0] & 0xFF;
    }

    @Override
    public int read(byte[] into, int offset, int length) throws IOException {
        if (ended) {
            return -1;
        }
        if (length == 0) {
            return 0;
        }
        if (left == 0 && !nextChunk()) {
            return -1;
        }
        int count = source.read(into, offset, Math.min(length, left));
        if (count < 0) {
            throw new EOFException("a chunk ended " + left + " bytes early");
        }
        left -= count;
        if (left == 0) {
            skipLineEnd();
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        ended = true;
        source.close();
    }

    /** @return false at the terminating zero-length chunk, trailers and all */
    private boolean nextChunk() throws IOException {
        String header = HttpLines.readLine(source);
        int extension = header.indexOf(';');
        String size = (extension < 0 ? header : header.substring(0, extension)).trim();
        if (size.isEmpty()) {
            throw new IOException("a chunked body had no size where one was due");
        }
        left = Integer.parseInt(size, 16);
        if (left > 0) {
            return true;
        }
        String trailer = HttpLines.readLine(source);
        while (!trailer.isEmpty()) {
            trailer = HttpLines.readLine(source);
        }
        ended = true;
        return false;
    }

    private void skipLineEnd() throws IOException {
        String rest = HttpLines.readLine(source);
        if (!rest.isEmpty()) {
            throw new IOException("a chunk ended with '" + rest + "' rather than a line break");
        }
    }
}
