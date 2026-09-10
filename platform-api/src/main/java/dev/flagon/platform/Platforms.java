package dev.flagon.platform;

public final class Platforms {

    private static Platform current;

    private Platforms() {
    }

    public static void install(Platform platform) {
        if (platform == null) {
            throw new IllegalArgumentException("platform must not be null");
        }
        if (current != null) {
            throw new IllegalStateException("platform already installed: " + current.name());
        }
        current = platform;
    }

    public static Platform get() {
        if (current == null) {
            throw new IllegalStateException(
                    "no platform installed; the runtime must call Platforms.install() during boot");
        }
        return current;
    }

    public static boolean isInstalled() {
        return current != null;
    }

    public static void reset() {
        current = null;
    }
}
