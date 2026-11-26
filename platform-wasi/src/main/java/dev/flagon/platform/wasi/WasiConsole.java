package dev.flagon.platform.wasi;

import dev.flagon.platform.ConsoleSink;
import org.teavm.runtime.wasi.Wasi;

import java.nio.charset.StandardCharsets;

/** {@code System.out} and info to stdout, warnings and errors to stderr, debug nowhere */
final class WasiConsole implements ConsoleSink {

    @Override
    public void write(Level level, String message, Throwable error) {
        if (level == Level.DEBUG) {
            return;
        }
        StringBuilder text = new StringBuilder(message);
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            text.append(cause == error ? "\n" : "\nCaused by: ").append(cause);
        }
        byte[] bytes = text.append('\n').toString().getBytes(StandardCharsets.UTF_8);
        int fd = level == Level.INFO ? Wasi.STDOUT : Wasi.STDERR;
        Wasi.flush(fd);
        Wasi.write(fd, bytes, 0, bytes.length);
    }
}
