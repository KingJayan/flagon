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
        fetchByUrl(console, "http://" + address + PROBE);
        refused(console, network, host);
        disallowed(console, network);
        console.info("net: done");
    }

    /** http by hand, because what is being proved is that the bytes went both ways */
    private void fetch(ConsoleSink console, Network network, String host, int port) {
        Connection connection = network.connect(host, port);
        console.info("connected to " + connection.host() + ":" + connection.port());
        try {
            String request = "GET " + PROBE + " HTTP/1.1\r\n"
                    + "Host: " + host + ":" + port + "\r\n"
                    + "Connection: close\r\n\r\n";
            byte[] sent = request.getBytes(StandardCharsets.US_ASCII);
            connection.write(sent, 0, sent.length);
            console.info("sent " + sent.length + " bytes");

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

    private byte[] readAll(Connection connection) {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int count = connection.read(chunk, 0, chunk.length);
        while (count >= 0) {
            collected.write(chunk, 0, count);
            count = connection.read(chunk, 0, chunk.length);
        }
        return collected.toByteArray();
    }
}
