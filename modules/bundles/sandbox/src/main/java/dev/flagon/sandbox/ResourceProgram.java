package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
        return "reads a packed file as a stream and as a url";
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
        console.info("resources: the url beside the class opens "
                + read(open(getClass().getResource(BESIDE))));
        console.info("resources: the loader's url opens "
                + read(open(getClass().getClassLoader().getResource(ABSOLUTE.substring(1)))));
        console.info("resources: a file nobody packed has the url "
                + getClass().getResource("/nothing-here.txt"));
        String[] apart = readApart().split("\n");
        console.info("resources: a file kept apart says " + apart[0] + " in " + apart.length
                + " lines");
    }

    /** a file kept apart arrives after the program starts, and reads fail until it does */
    private String readApart() throws IOException {
        for (int tries = 0; ; tries++) {
            try {
                return read(getClass().getResourceAsStream("apart.txt"));
            } catch (IOException early) {
                if (tries == 100) {
                    throw early;
                }
                sleep();
            }
        }
    }

    private static void sleep() throws IOException {
        try {
            Thread.sleep(50);
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
            throw new IOException(stopped);
        }
    }

    private static InputStream open(URL url) throws IOException {
        return url == null ? null : url.openStream();
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
