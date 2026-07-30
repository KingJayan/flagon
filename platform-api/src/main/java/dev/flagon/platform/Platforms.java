package dev.flagon.platform;

/**
 * holds the platform for this process.
 * static because lwjgl's api is static ({@code Display.create()}, {@code GL11.glClear()}) and
 * can't take a platform argument without breaking the source compatibility that's the point.
 */
public final class Platforms {

    private static Platform current;

    private Platforms() {
    }

    /** called once during boot, before any other thread exists. */
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
