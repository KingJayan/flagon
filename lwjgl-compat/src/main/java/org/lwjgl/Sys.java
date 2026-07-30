package org.lwjgl;

import dev.flagon.platform.Platforms;

public final class Sys {

    private static final long TIMER_RESOLUTION = 1_000_000_000L;

    private Sys() {
    }

    public static long getTime() {
        return Platforms.get().clock().nanoTime();
    }

    public static long getTimerResolution() {
        return TIMER_RESOLUTION;
    }

    public static String getVersion() {
        return "2.9.4-flagon";
    }

    public static void initialize() {
        // the platform is already installed by the time any hosted program runs
    }

    /** desktop lwjgl opens a browser here; in a browser the page is already open. */
    public static boolean openURL(String url) {
        return false;
    }
}
