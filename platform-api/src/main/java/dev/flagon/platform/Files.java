package dev.flagon.platform;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

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
