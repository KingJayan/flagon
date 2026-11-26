package dev.flagon.platform.browser.net;

import dev.flagon.platform.net.Connection;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.NetworkException;
import dev.flagon.platform.browser.js.JsArrays;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * connections carried by wss to proxy
 */
public final class ProxyNetwork implements Network {

    private ProxyNetwork() {
    }

    public static ProxyNetwork createIfAvailable() {
        return Tunnel.isAvailable() && Tunnel.isInstalled() ? new ProxyNetwork() : null;
    }

    @Override
    public String name() {
        return "websocket proxy";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Connection connect(String host, int port) {
        return open(host, port, false);
    }

    @Override
    public Connection connectSecure(String host, int port) {
        return open(host, port, true);
    }

    private static Connection open(String host, int port, boolean secure) {
        if (host == null || host.isEmpty()) {
            throw new NetworkException("no host to connect to");
        }
        if (port <= 0 || port > 65_535) {
            throw new NetworkException("port " + port + " is not a port");
        }
        return new TunnelConnection(host, port,
                Tunnel.open(Tunnel.endpoint(host, port, Tunnel.PATH, secure)));
    }


    private static final class TunnelConnection implements Connection {

        private final String host;
        private final int port;
        private final Tunnel.Handle handle;
        private byte[] chunk;
        private int taken;
        private boolean ended;
        private boolean closed;

        private TunnelConnection(String host, int port, Tunnel.Handle handle) {
            this.host = host;
            this.port = port;
            this.handle = handle;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public boolean isOpen() {
            return !closed && (held() > 0 || (!ended && Tunnel.isOpen(handle)));
        }

        @Override
        public int available() {
            return held() + (closed ? 0 : Tunnel.available(handle));
        }

        @Override
        public int read(byte[] into, int offset, int length) {
            requireRange(into, offset, length);
            requireOpen();
            if (length == 0) {
                return 0;
            }
            if (held() == 0 && !refill()) {
                return -1;
            }
            int count = Math.min(length, held());
            System.arraycopy(chunk, taken, into, offset, count);
            taken += count;
            return count;
        }

        @Override
        public void write(byte[] data, int offset, int length) {
            requireRange(data, offset, length);
            requireOpen();
            if (length == 0) {
                return;
            }
            byte[] slice = new byte[length];
            System.arraycopy(data, offset, slice, 0, length);
            Tunnel.send(handle, Int8Array.copyFromJavaArray(slice));
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                chunk = null;
                Tunnel.close(handle);
            }
        }

        private boolean refill() {
            Int8Array arrived = Tunnel.receive(handle);
            taken = 0;
            if (arrived == null) {
                chunk = null;
                ended = true;
                return false;
            }
            chunk = JsArrays.bytesOf(arrived);
            return true;
        }

        private int held() {
            return chunk == null ? 0 : chunk.length - taken;
        }

        private void requireOpen() {
            if (closed) {
                throw new NetworkException("the connection to " + host + ":" + port + " is closed");
            }
        }

        private static void requireRange(byte[] data, int offset, int length) {
            if (data == null) {
                throw new IllegalArgumentException("buffer must not be null");
            }
            if (offset < 0 || length < 0 || offset + length > data.length) {
                throw new IndexOutOfBoundsException(
                        "offset " + offset + " length " + length + " of " + data.length);
            }
        }
    }
}
