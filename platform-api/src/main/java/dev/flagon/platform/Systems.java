package dev.flagon.platform;

public final class Systems {

    private Systems() {
    }

    public static void exit(int status) {
        ShutdownHooks.exit(Runtime.getRuntime(), status);
    }

    public static void loadLibrary(String name) {
        throw new UnsupportedCapabilityException(
                "there is no native library to load in a page: '" + name + "' would have to be"
                        + " compiled into the bundle. see docs/COMPATIBILITY.md");
    }

    public static void load(String path) {
        loadLibrary(path);
    }

    public static String mapLibraryName(String name) {
        return name;
    }
}
