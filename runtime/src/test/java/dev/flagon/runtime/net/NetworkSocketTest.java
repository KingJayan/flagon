package dev.flagon.runtime.net;

import dev.flagon.platform.net.MemoryNetwork;
import dev.flagon.platform.net.UnavailableNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSocketTest {

    private MemoryNetwork network;
    private AtomicReference<String> heard;

    @BeforeEach
    void setUp() {
        network = new MemoryNetwork();
        heard = new AtomicReference<>("");
    }

    private void serve(String answer) {
        network.serve("host", 25_565, request -> {
            heard.set(new String(request, StandardCharsets.UTF_8));
            return answer.getBytes(StandardCharsets.UTF_8);
        });
    }

    private NetworkSocket connected() throws IOException {
        NetworkSocket socket = new NetworkSocket(network);
        socket.connect("host", 25_565, 0);
        return socket;
    }

    private static String readAll(InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void carriesBytesBothWays() throws Exception {
        serve("pong");
        NetworkSocket socket = connected();

        OutputStream out = socket.outputStream();
        out.write("ping".getBytes(StandardCharsets.UTF_8));

        assertEquals("pong", readAll(socket.inputStream()));
        assertEquals("ping", heard.get());
    }

    @Test
    void aSocketKnowsWhereItWent() throws Exception {
        serve("");
        NetworkSocket socket = connected();

        assertEquals("host", socket.host());
        assertEquals(25_565, socket.port());
        assertTrue(socket.isConnected());
        assertFalse(socket.isClosed());
    }

    @Test
    void aRefusedAddressFailsAsAnIoException() {
        NetworkSocket socket = new NetworkSocket(network);

        IOException refused = assertThrows(IOException.class,
                () -> socket.connect("host", 25_565, 0));
        assertTrue(refused.getMessage().contains("refused"), refused.getMessage());
    }

    @Test
    void aHostWithNoNetworkSaysSoRatherThanHanging() {
        NetworkSocket socket = new NetworkSocket(new UnavailableNetwork("this page has no proxy"));

        IOException failed = assertThrows(IOException.class,
                () -> socket.connect("host", 25_565, 0));
        assertTrue(failed.getMessage().contains("no network"), failed.getMessage());
    }

    @Test
    void aPortOutsideTheRangeIsNotAPort() {
        NetworkSocket socket = new NetworkSocket(network);

        assertThrows(IOException.class, () -> socket.connect("host", 0, 0));
        assertThrows(IOException.class, () -> socket.connect("host", 70_000, 0));
    }

    @Test
    void connectingTwiceIsRefused() throws Exception {
        serve("");
        NetworkSocket socket = connected();

        IOException again = assertThrows(IOException.class,
                () -> socket.connect("host", 25_565, 0));
        assertTrue(again.getMessage().contains("already connected"), again.getMessage());
    }

    @Test
    void streamsOnAnUnconnectedSocketAreRefused() {
        NetworkSocket socket = new NetworkSocket(network);

        assertThrows(IOException.class, socket::inputStream);
        assertThrows(IOException.class, socket::outputStream);
    }

    @Test
    void closingTheSocketClosesWhatReadsThroughIt() throws Exception {
        serve("data");
        NetworkSocket socket = connected();
        InputStream in = socket.inputStream();

        socket.close();

        assertTrue(socket.isClosed());
        assertThrows(IOException.class, in::read);
    }

    @Test
    void closingAStreamClosesTheSocketBehindIt() throws Exception {
        serve("data");
        NetworkSocket socket = connected();

        socket.inputStream().close();

        assertTrue(socket.isClosed());
    }

    @Test
    void closingTwiceIsHarmless() throws Exception {
        serve("");
        NetworkSocket socket = connected();

        socket.close();
        socket.close();

        assertTrue(socket.isClosed());
    }

    @Test
    void theEndOfTheStreamReadsAsMinusOne() throws Exception {
        serve("hi");
        NetworkSocket socket = connected();
        InputStream in = socket.inputStream();

        assertEquals('h', in.read());
        assertEquals('i', in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    void shuttingTheReadingHalfDownEndsTheStream() throws Exception {
        serve("data");
        NetworkSocket socket = connected();
        InputStream in = socket.inputStream();

        socket.shutdownInput();

        assertTrue(socket.isInputShutdown());
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
    }

    @Test
    void shuttingTheWritingHalfDownRefusesLaterWrites() throws Exception {
        serve("");
        NetworkSocket socket = connected();
        OutputStream out = socket.outputStream();

        socket.shutdownOutput();

        assertTrue(socket.isOutputShutdown());
        IOException failed = assertThrows(IOException.class, () -> out.write(1));
        assertTrue(failed.getMessage().contains("shut down"), failed.getMessage());
    }

    @Test
    void aReadTimeoutIsKeptForWhoeverAsksForItBack() throws Exception {
        serve("");
        NetworkSocket socket = connected();

        socket.setSoTimeout(3_000);

        assertEquals(3_000, socket.getSoTimeout());
    }

    @Test
    void aNegativeTimeoutIsNotAWait() throws Exception {
        serve("");
        NetworkSocket socket = connected();

        assertThrows(IllegalArgumentException.class, () -> socket.setSoTimeout(-1));
    }

    @Test
    void aRangeOutsideTheBufferIsRefused() throws Exception {
        serve("data");
        NetworkSocket socket = connected();

        assertThrows(IndexOutOfBoundsException.class,
                () -> socket.inputStream().read(new byte[2], 0, 5));
        assertThrows(IndexOutOfBoundsException.class,
                () -> socket.outputStream().write(new byte[2], 1, 4));
    }

    @Test
    void aWriteOfNothingAsksTheHostForNothing() throws Exception {
        serve("ok");
        NetworkSocket socket = connected();

        socket.outputStream().write(new byte[0], 0, 0);

        assertEquals("", heard.get());
        assertEquals("ok", readAll(socket.inputStream()));
    }
}
