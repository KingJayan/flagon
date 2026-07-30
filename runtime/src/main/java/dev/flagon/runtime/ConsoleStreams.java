package dev.flagon.runtime;

import dev.flagon.platform.ConsoleSink;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * routes {@code System.out} and {@code System.err} into the platform console.
 *
 * <p>an unmodified application reports through the standard streams and has never heard of
 * {@link ConsoleSink}. without this, everything it prints — including a stack trace it caught
 * and printed itself — lands somewhere the host cannot see.
 */
public final class ConsoleStreams {

    private static PrintStream previousOut;
    private static PrintStream previousErr;
    private static LineStream out;
    private static LineStream err;

    private ConsoleStreams() {
    }

    public static void install(ConsoleSink console) {
        if (console == null) {
            throw new IllegalArgumentException("console must not be null");
        }
        if (previousOut != null) {
            return;
        }
        previousOut = System.out;
        previousErr = System.err;
        out = new LineStream(console, false);
        err = new LineStream(console, true);
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    public static void restore() {
        if (previousOut == null) {
            return;
        }
        System.out.flush();
        System.err.flush();
        System.setOut(previousOut);
        System.setErr(previousErr);
        // whatever was printed without a closing newline still has to go somewhere
        out.drain();
        err.drain();
        previousOut = null;
        previousErr = null;
        out = null;
        err = null;
    }

    /**
     * a console takes messages, not bytes, so bytes are held until a line ends — a flush cannot
     * emit a partial line, because there is no such thing as half a console message. decoding
     * waits for the same point, since a multi-byte character can straddle two writes.
     */
    private static final class LineStream extends OutputStream {

        private static final int MAX_LINE = 8192;

        private final ConsoleSink console;
        private final boolean asError;
        private byte[] pending = new byte[128];
        private int length;

        LineStream(ConsoleSink console, boolean asError) {
            this.console = console;
            this.asError = asError;
        }

        @Override
        public void write(int value) {
            byte b = (byte) value;
            if (b == '\n') {
                emit();
                return;
            }
            if (b == '\r') {
                return;
            }
            if (length == pending.length) {
                if (length >= MAX_LINE) {
                    // a line without an end would otherwise grow until the tab runs out of memory
                    emit();
                } else {
                    byte[] grown = new byte[pending.length * 2];
                    System.arraycopy(pending, 0, grown, 0, length);
                    pending = grown;
                }
            }
            pending[length++] = b;
        }

        @Override
        public void write(byte[] bytes, int offset, int count) {
            for (int i = 0; i < count; i++) {
                write(bytes[offset + i]);
            }
        }

        void drain() {
            emit();
        }

        private void emit() {
            if (length == 0) {
                return;
            }
            String line = new String(pending, 0, length, StandardCharsets.UTF_8);
            length = 0;
            if (asError) {
                console.error(line, null);
            } else {
                console.info(line);
            }
        }
    }
}
