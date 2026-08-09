package dev.flagon.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * a bundle's programs as json, so a shell can offer them before the bundle has ever run.
 *
 * <p>written on the jvm at build time from the same registry the bundle boots, which is why
 * the list cannot drift from what the bundle actually holds.
 */
public final class ProgramCatalog {

    private ProgramCatalog() {
    }

    public static String json(ProgramRegistry registry) {
        StringBuilder out = new StringBuilder("[");
        for (Program program : registry.all()) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append("{\"id\":").append(quote(program.id()))
                    .append(",\"description\":").append(quote(program.description()))
                    .append('}');
        }
        return out.append(']').toString();
    }

    public static void writeTo(ProgramRegistry registry, String path) throws IOException {
        Path file = Path.of(path);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, json(registry), StandardCharsets.UTF_8);
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int at = 0; at < text.length(); at++) {
            char letter = text.charAt(at);
            switch (letter) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (letter < 0x20) {
                        out.append(String.format("\\u%04x", (int) letter));
                    } else {
                        out.append(letter);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
