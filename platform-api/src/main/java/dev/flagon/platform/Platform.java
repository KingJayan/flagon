package dev.flagon.platform;

import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;

/**
 * services a host must provide for desktop java programs to run
 */
public interface Platform {

    String name();

    Clock clock();

    ConsoleSink console();

    RuntimeHost host();

    /** @return the storage {@code java.io} reads and writes through */
    FileStore files();

    /** @return the mixer openal plays through; silent rather than absent where a host has no output */
    AudioDevice audio();

    /** @return whether this host can open a window at all. */
    boolean hasDisplay();

    /**
     * opens the window, or returns the one already open.
     *
     * @throws UnsupportedCapabilityException on a headless host
     */
    DisplaySurface display(int width, int height, String title);
}
