package dev.flagon.sandbox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SerialSamplesTest {

    private static final Path FIXTURE = Path.of("src/main/resources/dev/flagon/sandbox/serial.bin");

    /** run with -Dflagon.serial.update=true to write the fixture again */
    @Test
    void theFixtureIsWhatTheJvmWrites() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(SerialSamples.graph());
        }
        if (Boolean.getBoolean("flagon.serial.update")) {
            Files.write(FIXTURE, bytes.toByteArray());
        }
        try (InputStream in = Files.newInputStream(FIXTURE)) {
            assertArrayEquals(in.readAllBytes(), bytes.toByteArray());
        }
    }
}
