package dev.flagon.runtime.net;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/** the line-at-a-time reading http's head is made of */
final class HttpLines {

    private HttpLines() {
    }

    /**
     * reads one crlf-terminated line, a byte at a time on purpose
     */
    static String readLine(InputStream source) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int read = source.read();
        while (read >= 0 && read != '\n') {
            if (read != '\r') {
                line.write(read);
            }
            read = source.read();
        }
        if (read < 0 && line.size() == 0) {
            throw new EOFException("the response ended where a line was due");
        }
        return line.toString("ISO-8859-1");
    }
}
