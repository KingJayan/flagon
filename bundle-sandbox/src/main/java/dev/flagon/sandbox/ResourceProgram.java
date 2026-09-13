package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceProgram implements Program {

    private static final String BESIDE = "greeting.txt";
    private static final String ABSOLUTE = "/dev/flagon/sandbox/greeting.txt";

    @Override
    public String id() {
        return "resources";
    }

    @Override
    public String description() {
        return "reads a packed file with getResourceAsStream";
    }

    @Override
    public void run(String[] arguments) throws IOException {
        ConsoleSink console = Platforms.get().console();
        console.info("resources: beside the class says " + read(getClass()
                .getResourceAsStream(BESIDE)));
        console.info("resources: the absolute name says " + read(getClass()
                .getResourceAsStream(ABSOLUTE)));
        console.info("resources: the loader says " + read(getClass().getClassLoader()
                .getResourceAsStream(ABSOLUTE.substring(1))));
        console.info("resources: a file nobody packed reads "
                + read(getClass().getResourceAsStream("/nothing-here.txt")));
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) {
            return "nothing";
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }
}
