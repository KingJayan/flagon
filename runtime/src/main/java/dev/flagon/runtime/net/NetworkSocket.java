package dev.flagon.runtime.net;

import dev.flagon.platform.Platforms;
import dev.flagon.platform.net.Connection;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.NetworkException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * everything {@code java.net.Socket} does, over the platform's {@link Network}
 */
public final class NetworkSocket {

    private final Network network;
    private Connection connection;
    private String host;
    private int port = -1;
    private int timeout;
    private boolean closed;
    private boolean inputShutdown;
    private boolean outputShutdown;

    public NetworkSocket() {
        this(Platforms.get().network());
    }

    public NetworkSocket(Network network) {
        this.network = network;
    }

    /** @throws IOException if the address refused, is not allowed, or cannot be reached */
    public void connect(String host, int port, int timeoutMillis) throws IOException {
        if (closed) {
            throw new IOException("socket is closed");
        }
        if (connection != null) {
            throw new IOException("already connected to " + this.host + ":" + this.port);
        }
        if (host == null || host.isEmpty()) {
            throw new IOException("no host to connect to");
        }
        if (port <= 0 || port > 65_535) {
            throw new IOException("port " + port + " is not a port");
        }
        if (!network.isAvailable()) {
            throw new IOException("this host has no network; " + network.name());
        }
        // the connect timeout is the host's to honor: connect blocks, and there is no way to
        // hand it a deadline through the network abstraction
        try {
            connection = network.connect(host, port);
        } catch (NetworkException refused) {
            throw new IOException(refused.getMessage(), refused);
        }
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isInputShutdown() {
        return inputShutdown;
    }

    public boolean isOutputShutdown() {
        return outputShutdown;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int getSoTimeout() {
        return timeout;
    }

    /**
     * records the read timeout, which reads here cannot act on
     */
    public void setSoTimeout(int millis) throws IOException {
        if (millis < 0) {
            throw new IllegalArgumentException("a timeout of " + millis + " ms is not a wait");
        }
        timeout = millis;
    }

    public InputStream inputStream() throws IOException {
        return new SocketInput(requireConnected());
    }

    public OutputStream outputStream() throws IOException {
        return new SocketOutput(requireConnected());
    }

    public void shutdownInput() throws IOException {
        requireConnected();
        inputShutdown = true;
    }

    public void shutdownOutput() throws IOException {
        requireConnected();
        outputShutdown = true;
    }

    public void close() {
        if (!closed) {
            closed = true;
            if (connection != null) {
                connection.close();
            }
        }
    }

    private Connection requireConnected() throws IOException {
        if (closed) {
            throw new IOException("socket is closed");
        }
        if (connection == null) {
            throw new IOException("socket is not connected");
        }
        return connection;
    }

    private final class SocketInput extends InputStream {

        private final Connection connection;
        private boolean ended;

        private SocketInput(Connection connection) {
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
            if (into == null) {
                throw new NullPointerException("buffer must not be null");
            }
            if (offset < 0 || length < 0 || offset + length > into.length) {
                throw new IndexOutOfBoundsException(
                        "offset " + offset + " length " + length + " of " + into.length);
            }
            if (closed) {
                throw new IOException("socket is closed");
            }
            if (ended || inputShutdown) {
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
        public int available() throws IOException {
            if (closed) {
                throw new IOException("socket is closed");
            }
            return ended || inputShutdown ? 0 : connection.available();
        }

        @Override
        public void close() {
            NetworkSocket.this.close();
        }
    }

    private final class SocketOutput extends OutputStream {

        private final Connection connection;

        private SocketOutput(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void write(int value) throws IOException {
            write(new byte[] {(byte) value}, 0, 1);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            if (data == null) {
                throw new NullPointerException("buffer must not be null");
            }
            if (offset < 0 || length < 0 || offset + length > data.length) {
                throw new IndexOutOfBoundsException(
                        "offset " + offset + " length " + length + " of " + data.length);
            }
            if (closed) {
                throw new IOException("socket is closed");
            }
            if (outputShutdown) {
                throw new IOException("the writing half of this socket is shut down");
            }
            try {
                connection.write(data, offset, length);
            } catch (RuntimeException failed) {
                throw new IOException(failed.getMessage(), failed);
            }
        }

        @Override
        public void close() {
            NetworkSocket.this.close();
        }
    }
}
