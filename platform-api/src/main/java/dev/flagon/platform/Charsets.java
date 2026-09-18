package dev.flagon.platform;

import java.nio.charset.Charset;

public final class Charsets {

    private Charsets() {
    }

    public static boolean isSupported(String name) {
        try {
            return Charset.forName(name) != null;
        } catch (RuntimeException unknown) {
            return false;
        }
    }
}
