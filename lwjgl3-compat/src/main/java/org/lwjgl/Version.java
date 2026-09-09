package org.lwjgl;

/** what a program prints when it wants to say which lwjgl it is talking to. */
public final class Version {

    public static final int VERSION_MAJOR = 3;
    public static final int VERSION_MINOR = 3;
    public static final int VERSION_REVISION = 3;

    private static final String BUILD = "flagon";

    private Version() {
    }

    public static String getVersion() {
        return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_REVISION + " build " + BUILD;
    }

    public static String getVersion(String prefix) {
        return prefix + " " + getVersion();
    }
}
