package dev.flagon.platform;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/** a page has no file handle behind a stream, so there is no channel to hand back */
public final class Files {

    private Files() {
    }

    public static FileChannel getChannel(FileInputStream stream) {
        throw refuse();
    }

    public static FileChannel getChannel(FileOutputStream stream) {
        throw refuse();
    }

    private static UnsupportedCapabilityException refuse() {
        return new UnsupportedCapabilityException(
                "a page has no file handle to seek over: read the file as a stream instead. see"
                        + " docs/COMPATIBILITY.md");
    }
}
