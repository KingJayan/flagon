package dev.flagon.runtime.net;

import dev.flagon.platform.net.MemoryNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkHttpConnectionTest {

    private MemoryNetwork network;
    private AtomicReference<String> asked;

    @BeforeEach
    void setUp() {
        network = new MemoryNetwork();
        asked = new AtomicReference<>("");
    }

    @Test
    void sendsARequestLineAndAHostHeader() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nhi");

        read(open("http://host:7/some/where?q=1"));

        assertTrue(asked.get().startsWith("GET /some/where?q=1 HTTP/1.1\r\n"), asked.get());
        assertTrue(asked.get().contains("Host: host:7\r\n"), asked.get());
    }

    @Test
    void leavesThePortOutOfTheHostHeaderWhereItIsTheDefault() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");
        network.serve("host", 80, request -> record(request, "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"));

        read(open("http://host/"));

        assertTrue(asked.get().contains("Host: host\r\n"), asked.get());
    }

    @Test
    void asksForTheRootWhereAUrlHasNoPath() throws Exception {
        network.serve("host", 80, request -> record(request, "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"));

        read(open("http://host"));

        assertTrue(asked.get().startsWith("GET / HTTP/1.1\r\n"), asked.get());
    }

    @Test
    void readsABodyAsLongAsItsContentLength() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello and then some");

        assertEquals("hello", read(open("http://host:7/")));
    }

    @Test
    void readsABodyThatIsChunked() throws Exception {
        answer("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n"
                + "5\r\nhello\r\n2\r\n, \r\n5\r\nworld\r\n0\r\n\r\n");

        assertEquals("hello, world", read(open("http://host:7/")));
    }

    @Test
    void readsABodyThatEndsWithTheConnection() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nas much as was sent");

        assertEquals("as much as was sent", read(open("http://host:7/")));
    }

    @Test
    void reportsTheStatusAndItsMessage() throws Exception {
        answer("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");

        assertEquals(404, connection.getResponseCode());
        assertEquals("Not Found", connection.getResponseMessage());
    }

    @Test
    void handsBackHeadersWithoutCaringHowTheyWereCapitalised() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");

        assertEquals("text/plain", connection.getHeaderField("content-TYPE"));
        assertEquals("text/plain", connection.getContentType());
        assertEquals(0, connection.getContentLength());
    }

    @Test
    void sendsTheHeadersItWasAskedToSend() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.setRequestProperty("User-Agent", "flagon");
        connection.addRequestProperty("Accept", "text/plain");
        read(connection);

        assertTrue(asked.get().contains("User-Agent: flagon\r\n"), asked.get());
        assertTrue(asked.get().contains("Accept: text/plain\r\n"), asked.get());
        assertEquals("flagon", connection.getRequestProperty("User-Agent"));
    }

    @Test
    void decidesTheHostAndLengthHeadersItself() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.setRequestProperty("Host", "somewhere.else");
        read(connection);

        assertTrue(asked.get().contains("Host: host:7\r\n"), asked.get());
        assertTrue(!asked.get().contains("somewhere.else"), asked.get());
    }

    @Test
    void sendsABodyWithItsLength() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        out.write("name=value".getBytes(StandardCharsets.UTF_8));
        read(connection);

        assertTrue(asked.get().startsWith("POST / HTTP/1.1\r\n"), asked.get());
        assertTrue(asked.get().contains("Content-Length: 10\r\n"), asked.get());
        assertTrue(asked.get().endsWith("name=value"), asked.get());
    }

    @Test
    void refusesToWriteOnceTheRequestHasGone() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.setDoOutput(true);
        connection.connect();

        assertThrows(IOException.class, connection::getOutputStream);
    }

    @Test
    void refusesToWriteWhereOutputWasNeverAskedFor() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        assertThrows(IOException.class, () -> open("http://host:7/").getOutputStream());
    }

    @Test
    void sendsNoBodyForAHeadRequest() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.setRequestMethod("HEAD");

        assertEquals("", read(connection));
        assertEquals("12", connection.getHeaderField("Content-Length"));
    }

    @Test
    void treatsAFailureAsOneAndOffersTheReason() throws Exception {
        answer("HTTP/1.1 500 Server Error\r\nContent-Length: 5\r\n\r\noops!");

        HttpURLConnection connection = open("http://host:7/");

        assertThrows(IOException.class, connection::getInputStream);
        assertEquals("oops!", drain(connection.getErrorStream()));
    }

    @Test
    void hasNoErrorStreamWhereNothingFailed() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");
        connection.connect();

        assertNull(connection.getErrorStream());
    }

    @Test
    void reportsAnAddressThatRefusedRatherThanHanging() {
        HttpURLConnection connection = open("http://nowhere:7/");

        IOException failed = assertThrows(IOException.class, connection::getInputStream);
        assertTrue(failed.getMessage().contains("nowhere:7"), failed.getMessage());
    }

    @Test
    void reportsAnAnswerThatIsNotHttp() {
        answer("hello?\r\n\r\n");

        HttpURLConnection connection = open("http://host:7/");

        IOException failed = assertThrows(IOException.class, connection::getInputStream);
        assertTrue(failed.getMessage().contains("not http"), failed.getMessage());
    }

    @Test
    void saysItIsGoingThroughAProxy() {
        assertTrue(open("http://host:7/").usingProxy());
    }

    @Test
    void connectsOnlyOnce() throws Exception {
        answer("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nhi");

        HttpURLConnection connection = open("http://host:7/");
        connection.connect();
        connection.connect();

        assertEquals("hi", drain(connection.getInputStream()));
    }

    private HttpURLConnection open(String address) {
        try {
            return new NetworkHttpConnection(new URL(address), network);
        } catch (IOException malformed) {
            throw new IllegalArgumentException(malformed);
        }
    }

    private void answer(String response) {
        network.serve("host", 7, request -> record(request, response));
    }

    private byte[] record(byte[] request, String response) {
        asked.set(new String(request, StandardCharsets.ISO_8859_1));
        return response.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String read(HttpURLConnection connection) throws IOException {
        try (InputStream body = connection.getInputStream()) {
            return drain(body);
        }
    }

    private static String drain(InputStream body) throws IOException {
        assertNotNull(body);
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk = new byte[16];
        int count = body.read(chunk);
        while (count >= 0) {
            collected.write(chunk, 0, count);
            count = body.read(chunk);
        }
        return collected.toString("UTF-8");
    }
}
