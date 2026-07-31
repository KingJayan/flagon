package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.MemoryFileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;
import org.teavm.runtime.fs.VirtualFileSystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * the adapter is what {@code java.io} calls, so these are the calls {@code File},
 * {@code FileInputStream} and {@code RandomAccessFile} make on it.
 */
class StoreFileSystemTest {

    private MemoryFileStore store;
    private VirtualFileSystem fs;

    @BeforeEach
    void setUp() {
        store = new MemoryFileStore();
        fs = new StoreFileSystem(store);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private String read(String path) throws IOException {
        VirtualFileAccessor accessor = fs.getFile(path).createAccessor(true, false, false);
        assertNotNull(accessor, path + " could not be opened");
        byte[] into = new byte[accessor.size()];
        int read = accessor.read(into, 0, into.length);
        accessor.close();
        return new String(into, 0, Math.max(read, 0), StandardCharsets.UTF_8);
    }

    @Test
    void theUserDirectoryIsTheRoot() {
        assertEquals("/", fs.getUserDir());
        assertFalse(fs.isWindows());
        assertEquals("/a/b", fs.canonicalize("/a/./c/../b"));
    }

    @Test
    void writingThroughAnAccessorReachesTheStore() throws IOException {
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, false);
        accessor.write(bytes("hello"), 0, 5);
        accessor.close();

        assertArrayEquals(bytes("hello"), store.read("/notes.txt"));
    }

    /** a stream holds its contents until it is closed, which is when it closes its accessor. */
    @Test
    void aStreamReachesTheStoreOnFlush() throws IOException {
        store.createFile("/notes.txt");
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(false, true, false);
        accessor.write(bytes("hello"), 0, 5);

        assertEquals(0, store.size("/notes.txt"));

        accessor.flush();
        assertArrayEquals(bytes("hello"), store.read("/notes.txt"));
        accessor.close();
    }

    /**
     * teavm's RandomAccessFile.close() drops its accessor without flushing it, so a file opened
     * both readable and writable has to be written through on every write or the data is lost.
     */
    @Test
    void aReadWriteFileIsWrittenThroughWithoutBeingClosed() throws IOException {
        store.write("/notes.txt", bytes("hello world"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, false);
        accessor.seek(6);
        accessor.write(bytes("there"), 0, 5);

        assertArrayEquals(bytes("hello there"), store.read("/notes.txt"));
    }

    @Test
    void aReadWriteResizeIsWrittenThroughToo() throws IOException {
        store.write("/notes.txt", bytes("hello"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, false);
        accessor.resize(2);

        assertArrayEquals(bytes("he"), store.read("/notes.txt"));
    }

    @Test
    void readingReturnsMinusOneAtTheEnd() throws IOException {
        store.write("/notes.txt", bytes("ab"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, false, false);

        byte[] into = new byte[8];
        assertEquals(2, accessor.read(into, 0, 8));
        assertEquals(-1, accessor.read(into, 0, 8));
        accessor.close();
    }

    @Test
    void seekingOverwritesInPlace() throws IOException {
        store.write("/notes.txt", bytes("hello world"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, false);
        accessor.seek(6);
        accessor.write(bytes("there"), 0, 5);
        accessor.close();

        assertEquals("hello there", read("/notes.txt"));
    }

    @Test
    void appendingStartsAtTheEnd() throws IOException {
        store.write("/notes.txt", bytes("one"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, true);
        assertEquals(3, accessor.tell());
        accessor.write(bytes(" two"), 0, 4);
        accessor.close();

        assertEquals("one two", read("/notes.txt"));
    }

    @Test
    void resizingTruncatesAndZeroFills() throws IOException {
        store.write("/notes.txt", bytes("hello"));

        VirtualFileAccessor shrink = fs.getFile("/notes.txt").createAccessor(true, true, false);
        shrink.resize(2);
        shrink.close();
        assertEquals("he", read("/notes.txt"));

        VirtualFileAccessor grow = fs.getFile("/notes.txt").createAccessor(true, true, false);
        grow.resize(4);
        grow.close();
        assertArrayEquals(new byte[] {'h', 'e', 0, 0}, store.read("/notes.txt"));
    }

    @Test
    void aReadOnlyAccessorRefusesToWrite() throws IOException {
        store.write("/notes.txt", bytes("x"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, false, false);

        IOException thrown = org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> accessor.write(bytes("y"), 0, 1));
        assertTrue(thrown.getMessage().contains("/notes.txt"), thrown.getMessage());
        accessor.close();
    }

    @Test
    void openingAMissingFileForReadingFails() {
        assertNull(fs.getFile("/absent").createAccessor(true, false, false));
    }

    /** what new FileOutputStream("/absent") does: the file appears. */
    @Test
    void openingAMissingFileForWritingCreatesIt() throws IOException {
        VirtualFileAccessor accessor = fs.getFile("/fresh.txt").createAccessor(true, true, false);

        assertNotNull(accessor);
        assertTrue(store.isFile("/fresh.txt"));
        accessor.close();
    }

    @Test
    void aDirectoryCannotBeOpenedAsAFile() {
        store.createDirectory("/saves");

        assertNull(fs.getFile("/saves").createAccessor(true, false, false));
    }

    @Test
    void directoriesAreCreatedListedAndDeleted() throws IOException {
        VirtualFile root = fs.getFile("/");
        assertTrue(root.createDirectory("saves"));
        assertTrue(fs.getFile("/saves").createFile("one.dat"));

        VirtualFile saves = fs.getFile("/saves");
        assertTrue(saves.isDirectory());
        assertArrayEquals(new String[] {"one.dat"}, saves.listFiles());

        assertFalse(saves.delete());
        assertTrue(fs.getFile("/saves/one.dat").delete());
        assertTrue(saves.delete());
    }

    @Test
    void aFileListsAsNothingRatherThanAsEmpty() {
        store.write("/notes.txt", bytes("x"));

        assertNull(fs.getFile("/notes.txt").listFiles());
    }

    @Test
    void renamingAdoptsTheFileIntoAnotherDirectory() {
        store.createDirectory("/saves");
        store.write("/scratch.dat", bytes("data"));

        assertTrue(fs.getFile("/saves").adopt(fs.getFile("/scratch.dat"), "kept.dat"));
        assertFalse(store.exists("/scratch.dat"));
        assertArrayEquals(bytes("data"), store.read("/saves/kept.dat"));
    }

    @Test
    void nameAndLengthComeFromThePath() {
        store.write("/saves.dat", bytes("12345"));
        VirtualFile file = fs.getFile("/saves.dat");

        assertEquals("saves.dat", file.getName());
        assertEquals(5, file.length());
        assertTrue(file.isFile());
        assertTrue(file.canRead());
    }

    @Test
    void aClosedAccessorRefusesEverything() throws IOException {
        store.write("/notes.txt", bytes("x"));
        VirtualFileAccessor accessor = fs.getFile("/notes.txt").createAccessor(true, true, false);
        accessor.close();

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> accessor.read(new byte[1], 0, 1));
        // closing twice is what a try-with-resources does after an explicit close
        accessor.close();
    }

    @Test
    void aFileGrownByWritingKeepsEverythingWritten() throws IOException {
        VirtualFileAccessor accessor = fs.getFile("/big.txt").createAccessor(true, true, false);
        byte[] chunk = new byte[64];
        Arrays.fill(chunk, (byte) 'x');
        for (int at = 0; at < 40; at++) {
            accessor.write(chunk, 0, chunk.length);
        }
        accessor.close();

        assertEquals(2560, store.size("/big.txt"));
    }
}
