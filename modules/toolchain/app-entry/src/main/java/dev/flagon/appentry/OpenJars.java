package dev.flagon.appentry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** holds each jar open for the whole run */
final class OpenJars implements AutoCloseable {

    record Opened(Path path, JarFile jar, List<JarEntry> entries) {

        String name() {
            return path.getFileName().toString();
        }
    }

    private final List<Opened> jars = new ArrayList<>();

    OpenJars(List<Path> paths) throws IOException {
        for (Path path : paths) {
            JarFile opened = new JarFile(path.toFile());
            List<JarEntry> entries = new ArrayList<>();
            Enumeration<JarEntry> reading = opened.entries();
            while (reading.hasMoreElements()) {
                entries.add(reading.nextElement());
            }
            jars.add(new Opened(path, opened, Collections.unmodifiableList(entries)));
        }
    }

    List<Opened> all() {
        return jars;
    }

    @Override
    public void close() {
        for (Opened opened : jars) {
            try {
                opened.jar().close();
            } catch (IOException done) {
                // the run is over
            }
        }
    }
}
