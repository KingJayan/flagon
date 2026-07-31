package dev.flagon.platform.fs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePathsTest {

    @Test
    void anAbsolutePathIsLeftAsItIs() {
        assertEquals("/a/b/c", FilePaths.normalize("/a/b/c"));
    }

    @Test
    void repeatedAndTrailingSeparatorsAreRemoved() {
        assertEquals("/a/b", FilePaths.normalize("//a///b/"));
    }

    @Test
    void dotSegmentsAreResolved() {
        assertEquals("/a/c", FilePaths.normalize("/a/b/../c"));
        assertEquals("/a/b", FilePaths.normalize("/a/./b"));
    }

    /** posix stops at the root rather than reporting an error, and so does this. */
    @Test
    void climbingPastTheRootStaysThere() {
        assertEquals("/", FilePaths.normalize("/../.."));
        assertEquals("/a", FilePaths.normalize("/../a"));
    }

    @Test
    void aRelativePathStartsAtTheRoot() {
        assertEquals("/a/b", FilePaths.normalize("a/b"));
    }

    @Test
    void anEmptyPathIsTheRoot() {
        assertEquals("/", FilePaths.normalize(""));
        assertEquals("/", FilePaths.normalize(null));
        assertTrue(FilePaths.isRoot("/./"));
    }

    @Test
    void parentAndNameSplitThePath() {
        assertEquals("/a/b", FilePaths.parent("/a/b/c.txt"));
        assertEquals("c.txt", FilePaths.name("/a/b/c.txt"));
    }

    @Test
    void theRootIsItsOwnParentAndHasNoName() {
        assertEquals("/", FilePaths.parent("/"));
        assertEquals("", FilePaths.name("/"));
        assertEquals("/", FilePaths.parent("/a"));
    }

    @Test
    void joinNormalisesWhatItBuilds() {
        assertEquals("/a/b", FilePaths.join("/a/", "b"));
        assertEquals("/a", FilePaths.join("/a/b", ".."));
    }
}
