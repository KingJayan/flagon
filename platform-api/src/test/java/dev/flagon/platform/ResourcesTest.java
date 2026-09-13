package dev.flagon.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcesTest {

    @AfterEach
    void forget() {
        Resources.install(null);
    }

    @Test
    void readsEveryPackedFile() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one", "b/two.txt", "two")));

        assertEquals(2, Resources.count());
        assertArrayEquals("one".getBytes(StandardCharsets.UTF_8), Resources.read("a/one.txt"));
        assertArrayEquals("two".getBytes(StandardCharsets.UTF_8), Resources.read("b/two.txt"));
    }

    @Test
    void readsTheSameFileThroughALeadingSlash() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        assertTrue(Resources.has("/a/one.txt"));
        assertArrayEquals(Resources.read("a/one.txt"), Resources.read("/a/one.txt"));
    }

    @Test
    void opensAStreamOverOneFileOnly() throws IOException {
        Resources.install(pack(names("first", "second")));

        try (InputStream stream = Resources.open("first")) {
            assertEquals("first", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void answersNothingForAFileNobodyPacked() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        assertFalse(Resources.has("missing.txt"));
        assertNull(Resources.read("missing.txt"));
        assertNull(Resources.open("missing.txt"));
    }

    @Test
    void answersNothingWithNoPackAtAll() {
        Resources.install(null);

        assertEquals(0, Resources.count());
        assertNull(Resources.open("a/one.txt"));
    }

    @Test
    void refusesBytesThatAreNotAPack() {
        assertThrows(IllegalArgumentException.class,
                () -> Resources.install("not a pack at all".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void refusesAPackThatEndsEarly() throws IOException {
        byte[] whole = pack(Map.of("a/one.txt", "one"));
        byte[] cut = new byte[whole.length - 1];
        System.arraycopy(whole, 0, cut, 0, cut.length);

        assertThrows(IllegalArgumentException.class, () -> Resources.install(cut));
    }

    private static Map<String, String> names(String... files) {
        Map<String, String> packed = new LinkedHashMap<>();
        for (String file : files) {
            packed.put(file, file);
        }
        return packed;
    }

    private static byte[] pack(Map<String, String> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write("FLGNPK01".getBytes(StandardCharsets.US_ASCII));
        out.writeInt(files.size());
        for (Map.Entry<String, String> file : files.entrySet()) {
            byte[] name = file.getKey().getBytes(StandardCharsets.UTF_8);
            out.writeInt(name.length);
            out.write(name);
            out.writeInt(file.getValue().getBytes(StandardCharsets.UTF_8).length);
        }
        for (String content : files.values()) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
        return bytes.toByteArray();
    }
}
