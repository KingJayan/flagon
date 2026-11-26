package dev.flagon.platform;

import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.graphics.Drawing;
import dev.flagon.platform.net.Fetch;
import dev.flagon.platform.net.Network;

/**
  prereq services
 */
public interface Platform {

    String name();

    Clock clock();

    ConsoleSink console();

    RuntimeHost host();

    /** @return the storage {@code java.io} reads and writes through */
    FileStore files();

    /**
     * @return the mixer openal plays through; silent rather than absent where a host has no output
     */
    AudioDevice audio();

    /**
     * @return where outbound connections are opened; refuses every address where there is no way
     *         out
     */
    Network network();

    /** @return how {@code java.net.http} sends requests; refuses them where the host cannot */
    default Fetch fetch() {
        return Fetch.unavailable(name() + " cannot send http requests for java.net.http");
    }

    /** @return the 2d raster {@code java.awt} draws and {@code javax.imageio} decodes with */
    default Drawing drawing() {
        return Drawing.unavailable(name() + " has no 2d canvas for java.awt");
    }

    /** @return whether this host can open a window at all. */
    boolean hasDisplay();

    /**
     * opens the window or returns current.
     * @throws UnsupportedCapabilityException on a headless host
     */
    DisplaySurface display(int width, int height, String title);
}
