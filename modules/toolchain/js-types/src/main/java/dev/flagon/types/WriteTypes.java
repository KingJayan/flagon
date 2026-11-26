package dev.flagon.types;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * writes the typescript declarations of a javascript bundle: {@code <out.d.ts> <main class>}.
 * classes come from this tool's class loader, which holds the bundle's classpath.
 */
public final class WriteTypes {

    private WriteTypes() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args[0]);
        Files.createDirectories(out.getParent());
        Files.writeString(out, new Declarations(WriteTypes.class.getClassLoader()).module(args[1]));
    }
}
