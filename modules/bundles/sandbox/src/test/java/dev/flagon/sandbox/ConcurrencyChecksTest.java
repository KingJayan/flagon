package dev.flagon.sandbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrencyChecksTest {

    /** the browser test reads this file too */
    private static final Path FIXTURE = Path.of("src/test/resources/concurrency.txt");

    /** run with -Dflagon.concurrency.update=true to write the fixture again */
    @Test
    void theFixtureIsWhatTheJdkSays() throws IOException {
        List<String> lines = ConcurrencyChecks.run();
        if (Boolean.getBoolean("flagon.concurrency.update")) {
            Files.write(FIXTURE, lines);
        }
        assertEquals(Files.readAllLines(FIXTURE), lines);
    }
}
