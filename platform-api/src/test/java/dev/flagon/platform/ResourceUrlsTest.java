package dev.flagon.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceUrlsTest {

    private final ClassLoader loader = ResourceUrlsTest.class.getClassLoader();

    @AfterEach
    void forget() {
        Resources.install(null);
    }

    @Test
    void opensAPackedFileThroughItsUrl() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        URL found = ResourceUrls.of("a/one.txt");

        assertEquals("flagon", found.getProtocol());
        assertEquals("/a/one.txt", found.getPath());
        try (InputStream stream = found.openStream()) {
            assertEquals("one", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void reportsHowLongThePackedFileIs() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        URLConnection connection = ResourceUrls.of("/a/one.txt").openConnection();

        assertEquals(3, connection.getContentLength());
        assertEquals(3L, connection.getContentLengthLong());
    }

    @Test
    void keepsNoUrlForAFileNobodyPacked() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        assertNull(ResourceUrls.of("a/missing.txt"));
        assertNull(ClassLoaders.getResource(loader, "a/missing.txt"));
        assertFalse(ClassLoaders.getResources(loader, "a/missing.txt").hasMoreElements());
    }

    @Test
    void answersTheLoaderAndTheClassWithTheSameFile() throws IOException {
        Resources.install(pack(Map.of("dev/flagon/platform/one.txt", "one")));

        URL loaded = ClassLoaders.getResource(loader, "dev/flagon/platform/one.txt");
        URL beside = Classes.getResource(ResourceUrlsTest.class, "one.txt");

        assertEquals(loaded, beside);
        Enumeration<URL> all = ClassLoaders.getResources(loader, "dev/flagon/platform/one.txt");
        assertEquals(loaded, all.nextElement());
        assertFalse(all.hasMoreElements());
    }

    @Test
    void refusesAUrlWhoseFileLeftThePack() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));
        URL stale = ResourceUrls.of("a/one.txt");
        Resources.install(null);

        assertThrows(IOException.class, stale::openStream);
    }

    @Test
    void carriesTheWholePathInItsText() throws IOException {
        Resources.install(pack(Map.of("a/one.txt", "one")));

        assertTrue(ResourceUrls.of("a/one.txt").toString().endsWith("/a/one.txt"));
    }

    private static byte[] pack(Map<String, String> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write("FLGNPK02".getBytes(StandardCharsets.US_ASCII));
        out.writeInt(files.size());
        for (Map.Entry<String, String> file : files.entrySet()) {
            byte[] name = file.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] content = file.getValue().getBytes(StandardCharsets.UTF_8);
            out.writeInt(name.length);
            out.write(name);
            out.writeInt(content.length);
            out.writeInt(content.length);
            out.writeByte(0);
            out.write(content);
        }
        out.flush();
        return bytes.toByteArray();
    }
}
