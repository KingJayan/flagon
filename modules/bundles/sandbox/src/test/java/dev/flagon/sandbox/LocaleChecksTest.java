package dev.flagon.sandbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocaleChecksTest {

    /** the browser test reads this file too */
    private static final Path FIXTURE = Path.of("src/test/resources/locale.txt");

    /** run with -Dflagon.locale.update=true to write the fixture again */
    @Test
    void theFixtureIsWhatTheJdkSays() throws IOException {
        List<String> lines = LocaleChecks.run();
        if (Boolean.getBoolean("flagon.locale.update")) {
            Files.write(FIXTURE, lines);
        }
        assertEquals(Files.readAllLines(FIXTURE), lines);
    }
}
