package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.net.Connection;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.NetworkException;
import dev.flagon.runtime.Program;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * a tcp connection from a page that has no sockets, spoken to by hand
 */
public final class NetDemoProgram implements Program {

    private static final String DEFAULT_ADDRESS = "localhost:5273";
    private static final String PROBE = "/flagon-net-probe.txt";
    private static final String BULK_PROBE = "/flagon-net-bulk.bin";
    private static final String BULK = "/flagon-net-flood.bin";
    private static final int FLOOD_SIZE = 32 * 1024 * 1024;
    private static final int CREEP = 64 * 1024;
    private static final int SENT = 8 * 1024 * 1024;

    @Override
    public String id() {
        return "net";
    }

    @Override
    public String description() {
        return "fetches a file over a proxied socket (argument: host:port)";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        Network network = Platforms.get().network();
        console.info("network: " + network.name());
        console.info("available: " + network.isAvailable());

        String address = arguments.length > 0 ? arguments[0] : DEFAULT_ADDRESS;
        String host = address.substring(0, address.lastIndexOf(':'));
        int port = Integer.parseInt(address.substring(address.lastIndexOf(':') + 1));

        fetch(console, network, host, port);
        bulk(console, network, host, port);
        fetchByUrl(console, "http://" + address + PROBE);
        paced(console, network, host, port);
        pacedWrite(console, network, host, port);
        refused(console, network, host);
        disallowed(console, network);
        console.info("net: done");
    }

    /** http by hand, because what is being proved is that the bytes went both ways */
    private void fetch(ConsoleSink console, Network network, String host, int port) {
        Connection connection = network.connect(host, port);
        console.info("connected to " + connection.host() + ":" + connection.port());
        try {
            int sent = get(connection, PROBE, host, port);
            console.info("sent " + sent + " bytes");

            String response = new String(readAll(connection), StandardCharsets.UTF_8);
            int end = response.indexOf("\r\n");
            console.info("status: " + (end < 0 ? response : response.substring(0, end)));

            int body = response.indexOf("\r\n\r\n");
            if (body < 0) {
                throw new IllegalStateException("no headers ended in " + response.length() + " bytes");
            }
            console.info("body: " + response.substring(body + 4).trim());
        } finally {
            connection.close();
        }
        console.info("closed: " + !connection.isOpen());
    }

    private void bulk(ConsoleSink console, Network network, String host, int port) {
        Connection connection = network.connect(host, port);
        try {
            get(connection, BULK_PROBE, host, port);

            byte[] response = readAll(connection);
            int head = headEnd(response);
            int length = response.length - head;
            for (int at = 0; at < length; at++) {
                byte expected = (byte) (at % 251);
                if (response[head + at] != expected) {
                    throw new IllegalStateException("byte " + at + " is " + response[head + at]
                            + ", not " + expected);
                }
            }
            console.info("bulk: " + length + " bytes in order");
        } finally {
            connection.close();
        }
    }

    /** the same file again, this time through code that has never heard of a proxy */
    private void fetchByUrl(ConsoleSink console, String address) throws Exception {
        URLConnection connection = new URL(address).openConnection();
        console.info("url type: " + connection.getContentType());
        try (InputStream body = connection.getInputStream()) {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = new byte[256];
            int count = body.read(chunk);
            while (count >= 0) {
                collected.write(chunk, 0, count);
                count = body.read(chunk);
            }
            console.info("url body: "
                    + new String(collected.toByteArray(), StandardCharsets.UTF_8).trim());
        }
    }

    private void paced(ConsoleSink console, Network network, String host, int port) {
        Connection connection = network.connect(host, port);
        try {
            get(connection, BULK, host, port);

            Thread.sleep(1000);
            int held = connection.available();
            boolean capped = false;
            for (int tries = 0; tries < 80 && !capped; tries++) {
                Thread.sleep(250);
                int later = connection.available();
                capped = later - held <= CREEP && later > 0;
                held = later;
            }
            // a host that buffers the whole flood settles above it, having never had to hold back
            console.info("paced: queue held at " + held + " bytes, capped: " + capped
                    + ", held back: " + (held < FLOOD_SIZE));

            drain(connection, held);
            boolean resumed = false;
            for (int tries = 0; tries < 20 && !resumed; tries++) {
                Thread.sleep(200);
                resumed = connection.available() > 0;
            }
            console.info("paced: sending resumed: " + resumed);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            connection.close();
        }
    }

    /** a body larger than the browser will hold for a socket, which the write has to wait out */
    private void pacedWrite(ConsoleSink console, Network network, String host, int port) {
        Connection connection = network.connect(host, port);
        try {
            String request = "POST " + BULK + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Content-Length: " + SENT + "\r\n"
                    + "Connection: close\r\n\r\n";
            byte[] head = request.getBytes(StandardCharsets.US_ASCII);
            connection.write(head, 0, head.length);

            byte[] chunk = new byte[64 * 1024];
            for (int at = 0; at < SENT; at += chunk.length) {
                connection.write(chunk, 0, chunk.length);
            }

            String response = new String(readAll(connection), StandardCharsets.UTF_8);
            int taken = Integer.parseInt(response.substring(
                    response.lastIndexOf("taken ") + "taken ".length()).trim());
            console.info("paced: whole write arrived: " + (taken == SENT));
        } finally {
            connection.close();
        }
    }

    /** reads what is already here and nothing more, so the queue falls back under the low mark */
    private void drain(Connection connection, int waiting) {
        byte[] chunk = new byte[8192];
        int left = waiting;
        while (left > 0) {
            int count = connection.read(chunk, 0, Math.min(chunk.length, left));
            if (count < 0) {
                return;
            }
            left -= count;
        }
    }

    /** nothing is listening on port 1, and a connection that cannot be made has to say so */
    private void refused(ConsoleSink console, Network network, String host) {
        try {
            network.connect(host, 1).close();
            console.warn("something answered on port 1");
        } catch (NetworkException refused) {
            console.info("refused: " + refused.getMessage());
        }
    }

    /** the proxy decides who may be reached, and it was told only about this host */
    private void disallowed(ConsoleSink console, Network network) {
        try {
            network.connect("example.com", 80).close();
            console.warn("example.com was reachable");
        } catch (NetworkException blocked) {
            console.info("not allowed: " + blocked.getMessage());
        }
    }

    /** @return how many bytes the request took */
    private int get(Connection connection, String path, String host, int port) {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Connection: close\r\n\r\n";
        byte[] sent = request.getBytes(StandardCharsets.US_ASCII);
        connection.write(sent, 0, sent.length);
        return sent.length;
    }

    private byte[] readAll(Connection connection) {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int filled = 0;
        int count = connection.read(chunk, filled, chunk.length - filled);
        while (count >= 0) {
            filled += count;
            if (filled == chunk.length) {
                collected.write(chunk, 0, filled);
                filled = 0;
            }
            count = connection.read(chunk, filled, chunk.length - filled);
        }
        collected.write(chunk, 0, filled);
        return collected.toByteArray();
    }

    /** @return the offset just past the blank line that ends the response head */
    private static int headEnd(byte[] response) {
        for (int at = 0; at + 3 < response.length; at++) {
            if (response[at] == '\r' && response[at + 1] == '\n'
                    && response[at + 2] == '\r' && response[at + 3] == '\n') {
                return at + 4;
            }
        }
        throw new IllegalStateException("no headers ended in " + response.length + " bytes");
    }
}
