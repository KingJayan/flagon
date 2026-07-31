package dev.flagon.platform.net;

import java.util.HashMap;
import java.util.Map;

/**
 * a network whose servers are objects in the heap
 */
public final class MemoryNetwork implements Network {

    /** a server: one request in, one response out */
    public interface Service {
        byte[] answer(byte[] request);
    }

    private final Map<String, Service> served = new HashMap<>();

    public void serve(String host, int port, Service service) {
        served.put(address(host, port), service);
    }

    public void refuse(String host, int port) {
        served.remove(address(host, port));
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Connection connect(String host, int port) {
        Service service = served.get(address(host, port));
        if (service == null) {
            throw new NetworkException("connection refused by " + host + ":" + port);
        }
        return new HeapConnection(host, port, service);
    }

    private static String address(String host, int port) {
        return host + ":" + port;
    }

    private static final class HeapConnection implements Connection {

        private final String host;
        private final int port;
        private final Service service;
        private byte[] sent = new byte[0];
        private byte[] received;
        private int read;
        private boolean open = true;

        private HeapConnection(String host, int port, Service service) {
            this.host = host;
            this.port = port;
            this.service = service;
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
            return open && (received == null || read < received.length);
        }

        @Override
        public int available() {
            return received == null ? 0 : received.length - read;
        }

        @Override
        public int read(byte[] into, int offset, int length) {
            requireOpen();
            requireRange(into, offset, length);
            answer();
            if (read >= received.length) {
                return -1;
            }
            int count = Math.min(length, received.length - read);
            System.arraycopy(received, read, into, offset, count);
            read += count;
            return count;
        }

        @Override
        public void write(byte[] data, int offset, int length) {
            requireOpen();
            requireRange(data, offset, length);
            if (received != null) {
                throw new NetworkException("this service has already answered " + host + ":" + port);
            }
            byte[] grown = new byte[sent.length + length];
            System.arraycopy(sent, 0, grown, 0, sent.length);
            System.arraycopy(data, offset, grown, sent.length, length);
            sent = grown;
        }

        @Override
        public void close() {
            open = false;
        }

        /** the request is whatever was written before the first read, as a server sees it */
        private void answer() {
            if (received == null) {
                byte[] answered = service.answer(sent);
                received = answered == null ? new byte[0] : answered;
            }
        }

        private void requireOpen() {
            if (!open) {
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
