package dev.flagon.runtime;

import dev.flagon.platform.ConsoleSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleStreamsTest {

    private final List<String> lines = new ArrayList<>();

    private final ConsoleSink console =
            (level, message, error) -> lines.add(level + ": " + message);

    @AfterEach
    void tearDown() {
        ConsoleStreams.restore();
    }

    @Test
    void printlnBecomesOneConsoleLine() {
        ConsoleStreams.install(console);
        System.out.println("hello from the application");

        assertEquals(List.of("INFO: hello from the application"), lines);
    }

    @Test
    void standardErrorIsReportedAsAnError() {
        ConsoleStreams.install(console);
        System.err.println("something went wrong");

        assertEquals(List.of("ERROR: something went wrong"), lines);
    }

    @Test
    void partialWritesAreJoinedUntilTheLineEnds() {
        ConsoleStreams.install(console);
        System.out.print("one ");
        System.out.print("line");
        assertTrue(lines.isEmpty(), "a line without an end was reported early");

        System.out.println();
        assertEquals(List.of("INFO: one line"), lines);
    }

    @Test
    void multipleLinesInOneWriteAreSplit() {
        ConsoleStreams.install(console);
        System.out.println("first\nsecond");

        assertEquals(List.of("INFO: first", "INFO: second"), lines);
    }

    /** a character can straddle two writes, so bytes are decoded only once a line is whole. */
    @Test
    void multiByteCharactersSurvive() {
        ConsoleStreams.install(console);
        System.out.println("héllo ☃");

        assertEquals(List.of("INFO: héllo ☃"), lines);
    }

    @Test
    void aTrailingLineIsFlushedRatherThanLost() {
        ConsoleStreams.install(console);
        System.out.print("no newline here");
        ConsoleStreams.restore();

        assertEquals(List.of("INFO: no newline here"), lines);
    }

    @Test
    void restorePutsTheOriginalStreamsBack() {
        java.io.PrintStream original = System.out;
        ConsoleStreams.install(console);
        assertNotSame(original, System.out);

        ConsoleStreams.restore();
        assertSame(original, System.out);
    }

    @Test
    void installingTwiceDoesNotLoseTheOriginalStream() {
        java.io.PrintStream original = System.out;
        ConsoleStreams.install(console);
        ConsoleStreams.install(console);
        ConsoleStreams.restore();

        assertSame(original, System.out);
    }
}
