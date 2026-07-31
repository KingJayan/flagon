package dev.flagon.platform;

import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;

/**
 * services a host must provide for desktop java programs to run.
 * grows one capability per stage; every method here is really implemented by every host.
 */
public interface Platform {

    String name();

    Clock clock();

    ConsoleSink console();

    RuntimeHost host();

    /** @return the storage {@code java.io} reads and writes through */
    FileStore files();

    /** @return whether this host can open a window at all. */
    boolean hasDisplay();

    /**
     * opens the window, or returns the one already open.
     *
     * @throws UnsupportedCapabilityException on a headless host
     */
    DisplaySurface display(int width, int height, String title);
}
