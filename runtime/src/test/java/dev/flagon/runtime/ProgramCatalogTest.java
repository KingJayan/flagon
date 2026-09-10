package dev.flagon.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramCatalogTest {

    @Test
    void writesEveryProgramInTheOrderItWasRegistered() {
        String json = ProgramCatalog.json(new ProgramRegistry()
                .register(program("hello", "prints a greeting"))
                .register(program("net", "fetches a file")));

        assertEquals("[{\"id\":\"hello\",\"description\":\"prints a greeting\"},"
                + "{\"id\":\"net\",\"description\":\"fetches a file\"}]", json);
    }

    /** a description is prose, and prose has quotes in it */
    @Test
    void escapesWhatWouldOtherwiseBreakTheJson() {
        String json = ProgramCatalog.json(new ProgramRegistry()
                .register(program("odd", "a \"quoted\" \\ path\nand a newline")));

        assertEquals("[{\"id\":\"odd\",\"description\":"
                + "\"a \\\"quoted\\\" \\\\ path\\nand a newline\"}]", json);
    }

    @Test
    void anEmptyBundleIsAnEmptyList() {
        assertEquals("[]", ProgramCatalog.json(new ProgramRegistry()));
    }

    @Test
    void makesTheDirectoryItWritesInto(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("flagon/sandbox.catalog.json");

        ProgramCatalog.writeTo(new ProgramRegistry().register(program("hello", "hi")),
                file.toString());

        assertEquals("[{\"id\":\"hello\",\"description\":\"hi\"}]", Files.readString(file));
    }

    private static Program program(String id, String description) {
        return new Program() {

            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public void run(String[] arguments) {
            }
        };
    }
}
