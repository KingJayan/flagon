package dev.flagon.platform.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFileStoreTest {

    private MemoryFileStore store;

    @BeforeEach
    void setUp() {
        store = new MemoryFileStore();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void theRootExistsBeforeAnythingIsWritten() {
        assertTrue(store.exists("/"));
        assertTrue(store.isDirectory("/"));
        assertEquals(0, store.list("/").length);
    }

    @Test
    void aFileIsReadBackAsItWasWritten() {
        store.write("/greeting.txt", bytes("hello"));

        assertTrue(store.isFile("/greeting.txt"));
        assertEquals(5, store.size("/greeting.txt"));
        assertArrayEquals(bytes("hello"), store.read("/greeting.txt"));
    }

    /** a store that handed out its own array would let a reader edit the file by accident. */
    @Test
    void readingAFileDoesNotShareTheStoredArray() {
        store.write("/greeting.txt", bytes("hello"));
        byte[] read = store.read("/greeting.txt");
        read[0] = 'j';

        assertArrayEquals(bytes("hello"), store.read("/greeting.txt"));
    }

    @Test
    void writingIntoADirectoryThatDoesNotExistIsRejected() {
        FileStoreException thrown = assertThrows(FileStoreException.class,
                () -> store.write("/nowhere/file.txt", bytes("x")));

        assertTrue(thrown.getMessage().contains("/nowhere"), thrown.getMessage());
    }

    @Test
    void readingAMissingFileIsRejected() {
        assertThrows(FileStoreException.class, () -> store.read("/absent"));
    }

    @Test
    void directoriesNestAndList() {
        assertTrue(store.createDirectory("/saves"));
        store.write("/saves/one.dat", bytes("1"));
        store.write("/saves/two.dat", bytes("2"));

        String[] names = store.list("/saves");
        Arrays.sort(names);
        assertArrayEquals(new String[] {"one.dat", "two.dat"}, names);
        assertArrayEquals(new String[] {"saves"}, store.list("/"));
    }

    @Test
    void aDirectoryCannotBeCreatedTwiceOrOverAFile() {
        assertTrue(store.createDirectory("/saves"));
        assertFalse(store.createDirectory("/saves"));

        store.write("/notes", bytes("x"));
        assertFalse(store.createDirectory("/notes"));
    }

    @Test
    void aDirectoryWithNoParentIsNotCreated() {
        assertFalse(store.createDirectory("/a/b"));
    }

    @Test
    void createFileLeavesAnExistingOneAlone() {
        store.write("/notes", bytes("keep me"));

        assertFalse(store.createFile("/notes"));
        assertArrayEquals(bytes("keep me"), store.read("/notes"));
    }

    @Test
    void deletingRemovesAFile() {
        store.write("/notes", bytes("x"));

        assertTrue(store.delete("/notes"));
        assertFalse(store.exists("/notes"));
        assertFalse(store.delete("/notes"));
    }

    /** java.io refuses to remove a directory with anything in it, and callers rely on that. */
    @Test
    void deletingANonEmptyDirectoryIsRefused() {
        store.createDirectory("/saves");
        store.write("/saves/one.dat", bytes("1"));

        assertFalse(store.delete("/saves"));

        store.delete("/saves/one.dat");
        assertTrue(store.delete("/saves"));
    }

    @Test
    void theRootCannotBeDeleted() {
        assertFalse(store.delete("/"));
    }

    @Test
    void movingRenamesAFileAndLeavesNothingBehind() {
        store.createDirectory("/saves");
        store.write("/scratch.dat", bytes("data"));

        assertTrue(store.move("/scratch.dat", "/saves/kept.dat"));
        assertFalse(store.exists("/scratch.dat"));
        assertArrayEquals(bytes("data"), store.read("/saves/kept.dat"));
    }

    @Test
    void movingOntoSomethingThatExistsIsRefused() {
        store.write("/one", bytes("1"));
        store.write("/two", bytes("2"));

        assertFalse(store.move("/one", "/two"));
        assertArrayEquals(bytes("2"), store.read("/two"));
    }

    @Test
    void pathsAreResolvedBeforeTheyAreLookedUp() {
        store.createDirectory("/saves");
        store.write("/saves/one.dat", bytes("1"));

        assertTrue(store.exists("/saves/../saves/./one.dat"));
        assertArrayEquals(bytes("1"), store.read("saves/one.dat"));
    }

    @Test
    void aFileIsNotADirectory() {
        store.write("/notes", bytes("x"));

        assertFalse(store.isDirectory("/notes"));
        assertEquals(0, store.list("/notes").length);
    }

    @Test
    void aWrittenFileCarriesAModifiedTime() {
        store.write("/notes", bytes("x"));

        assertTrue(store.lastModified("/notes") > 0L);
    }

    @Test
    void aDeletedFileTakesItsMetadataWithIt() {
        store.write("/notes", bytes("x"));

        assertTrue(store.delete("/notes"));

        assertEquals(0L, store.lastModified("/notes"));
        assertEquals(0, store.size("/notes"));
        assertFalse(store.isFile("/notes"));
    }

    @Test
    void aMovedFileLeavesNothingBehind() {
        store.write("/one", bytes("1"));

        assertTrue(store.move("/one", "/two"));

        assertEquals(0L, store.lastModified("/one"));
        assertFalse(store.exists("/one"));
        assertArrayEquals(bytes("1"), store.read("/two"));
        assertTrue(store.lastModified("/two") > 0L);
    }

    @Test
    void aDirectoryReportsNoBytesAndIsNotAFile() {
        store.createDirectory("/saves");

        assertEquals(0, store.size("/saves"));
        assertFalse(store.isFile("/saves"));
        assertThrows(FileStoreException.class, () -> store.read("/saves"));
    }
}
