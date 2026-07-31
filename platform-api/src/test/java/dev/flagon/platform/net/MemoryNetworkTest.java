package dev.flagon.platform.net;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryNetworkTest {

    private MemoryNetwork network;

    @BeforeEach
    void setUp() {
        network = new MemoryNetwork();
    }

    @Test
    void refusesAnAddressNothingServes() {
        NetworkException refused =
                assertThrows(NetworkException.class, () -> network.connect("example.com", 80));

        assertTrue(refused.getMessage().contains("example.com:80"));
    }

    @Test
    void carriesARequestToTheServiceAndTheAnswerBack() {
        network.serve("host", 7, request -> reverse(request));

        Connection connection = network.connect("host", 7);
        write(connection, "abc");

        assertEquals("cba", readAll(connection));
    }

    @Test
    void tellsTheAddressItWasOpenedFor() {
        network.serve("host", 7, request -> request);

        Connection connection = network.connect("host", 7);

        assertEquals("host", connection.host());
        assertEquals(7, connection.port());
    }

    @Test
    void readsAsMuchAsTheCallerAsksFor() {
        network.serve("host", 7, request -> "abcdef".getBytes(StandardCharsets.UTF_8));

        Connection connection = network.connect("host", 7);
        write(connection, "?");
        byte[] into = new byte[4];

        assertEquals(4, connection.read(into, 0, 4));
        assertEquals("abcd", new String(into, 0, 4, StandardCharsets.UTF_8));
        assertEquals(2, connection.read(into, 0, 4));
        assertEquals(-1, connection.read(into, 0, 4));
    }

    @Test
    void countsWhatIsReadableWithoutBlocking() {
        network.serve("host", 7, request -> new byte[] {1, 2, 3});

        Connection connection = network.connect("host", 7);
        write(connection, "?");
        connection.read(new byte[1], 0, 1);

        assertEquals(2, connection.available());
    }

    @Test
    void isOpenUntilTheAnswerRunsOut() {
        network.serve("host", 7, request -> new byte[] {1});

        Connection connection = network.connect("host", 7);
        assertTrue(connection.isOpen());
        write(connection, "?");
        connection.read(new byte[1], 0, 1);

        assertFalse(connection.isOpen());
    }

    @Test
    void sendsEverythingWrittenBeforeTheFirstRead() {
        network.serve("host", 7, request -> request);

        Connection connection = network.connect("host", 7);
        write(connection, "one ");
        write(connection, "two");

        assertEquals("one two", readAll(connection));
    }

    @Test
    void refusesToWriteAfterTheServiceHasAnswered() {
        network.serve("host", 7, request -> new byte[] {1});

        Connection connection = network.connect("host", 7);
        write(connection, "?");
        connection.read(new byte[1], 0, 1);

        assertThrows(NetworkException.class, () -> write(connection, "more"));
    }

    @Test
    void refusesToReadOnceClosed() {
        network.serve("host", 7, request -> new byte[] {1});

        Connection connection = network.connect("host", 7);
        connection.close();

        assertFalse(connection.isOpen());
        assertThrows(NetworkException.class, () -> connection.read(new byte[1], 0, 1));
    }

    @Test
    void closingTwiceIsNotAnError() {
        network.serve("host", 7, request -> new byte[0]);

        Connection connection = network.connect("host", 7);
        connection.close();
        connection.close();

        assertFalse(connection.isOpen());
    }

    @Test
    void refusesARangeOutsideTheBuffer() {
        network.serve("host", 7, request -> new byte[0]);
        Connection connection = network.connect("host", 7);

        assertThrows(IndexOutOfBoundsException.class,
                () -> connection.write(new byte[2], 1, 4));
        assertThrows(IndexOutOfBoundsException.class,
                () -> connection.read(new byte[2], -1, 1));
    }

    @Test
    void stopsServingAnAddressItWasToldToRefuse() {
        network.serve("host", 7, request -> new byte[0]);
        network.refuse("host", 7);

        assertThrows(NetworkException.class, () -> network.connect("host", 7));
    }

    @Test
    void answersNothingWhereAServiceAnswersNull() {
        network.serve("host", 7, request -> null);

        Connection connection = network.connect("host", 7);
        write(connection, "?");

        assertEquals("", readAll(connection));
    }

    @Test
    void aHostWithoutOneRefusesEveryAddressByName() {
        Network none = new UnavailableNetwork("this host has no way out");

        assertFalse(none.isAvailable());
        NetworkException refused =
                assertThrows(NetworkException.class, () -> none.connect("example.com", 80));
        assertTrue(refused.getMessage().contains("example.com:80"));
        assertTrue(refused.getMessage().contains("no way out"));
    }

    @Test
    void servesEachAddressSeparately() {
        network.serve("host", 7, request -> "seven".getBytes(StandardCharsets.UTF_8));
        network.serve("host", 8, request -> "eight".getBytes(StandardCharsets.UTF_8));

        Connection seven = network.connect("host", 7);
        write(seven, "?");
        Connection eight = network.connect("host", 8);
        write(eight, "?");

        assertEquals("seven", readAll(seven));
        assertEquals("eight", readAll(eight));
    }

    @Test
    void carriesBytesThatAreNotText() {
        byte[] raw = {0, -1, 127, -128};
        network.serve("host", 7, request -> request);

        Connection connection = network.connect("host", 7);
        connection.write(raw, 0, raw.length);
        byte[] into = new byte[raw.length];
        connection.read(into, 0, into.length);

        assertArrayEquals(raw, into);
    }

    private static void write(Connection connection, String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        connection.write(data, 0, data.length);
    }

    private static String readAll(Connection connection) {
        StringBuilder collected = new StringBuilder();
        byte[] chunk = new byte[8];
        int count = connection.read(chunk, 0, chunk.length);
        while (count >= 0) {
            collected.append(new String(chunk, 0, count, StandardCharsets.UTF_8));
            count = connection.read(chunk, 0, chunk.length);
        }
        return collected.toString();
    }

    private static byte[] reverse(byte[] data) {
        byte[] reversed = new byte[data.length];
        for (int at = 0; at < data.length; at++) {
            reversed[at] = data[data.length - 1 - at];
        }
        return reversed;
    }
}
