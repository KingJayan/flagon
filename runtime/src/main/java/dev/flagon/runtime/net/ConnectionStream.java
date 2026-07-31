package dev.flagon.runtime.net;

import dev.flagon.platform.net.Connection;

import java.io.IOException;
import java.io.InputStream;

/** a {@link Connection} read as a stream, so java.io code can be pointed at it */
final class ConnectionStream extends InputStream {

    private final Connection connection;
    private boolean ended;

    ConnectionStream(Connection connection) {
        this.connection = connection;
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
        try {
            int count = connection.read(into, offset, length);
            if (count < 0) {
                ended = true;
                return -1;
            }
            return count;
        } catch (RuntimeException failed) {
            throw new IOException(failed.getMessage(), failed);
        }
    }

    @Override
    public int available() {
        return ended ? 0 : connection.available();
    }

    @Override
    public void close() {
        ended = true;
        connection.close();
    }
}
