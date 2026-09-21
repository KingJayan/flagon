package dev.flagon.lwjgl;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.SilentAudioDevice;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.MemoryFileStore;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.input.InputSource;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.UnavailableNetwork;

/**
 * a host whose clock only moves when a frame is presented, so pacing is a matter of counting
 * rather than of waiting.
 */
final class FakeDisplayPlatform implements Platform, Clock, ConsoleSink, RuntimeHost {

    /** how long one of this host's frames takes; 144 Hz unless a test says otherwise */
    long frameNanos = 1_000_000_000L / 144;

    /** how coarse this host's clock reads, the way a browser rounds performance.now() down */
    long clockGrain = 1L;

    private final FileStore files = new MemoryFileStore();
    private final AudioDevice audio = new SilentAudioDevice(this);
    private final Network network = new UnavailableNetwork("no network in this test");
    private final Surface surface = new Surface();

    private long nanos;

    int presented() {
        return surface.presents;
    }

    boolean destroyed() {
        return surface.destroyed;
    }

    int opened() {
        return surface.opens;
    }

    FakeInputSource input() {
        return surface.input;
    }

    void focus(boolean focused) {
        surface.focused = focused;
    }

    void show(boolean visible) {
        surface.visible = visible;
    }

    @Override
    public String name() {
        return "fake-display";
    }

    @Override
    public Clock clock() {
        return this;
    }

    @Override
    public ConsoleSink console() {
        return this;
    }

    @Override
    public RuntimeHost host() {
        return this;
    }

    @Override
    public FileStore files() {
        return files;
    }

    @Override
    public AudioDevice audio() {
        return audio;
    }

    @Override
    public Network network() {
        return network;
    }

    @Override
    public boolean hasDisplay() {
        return true;
    }

    @Override
    public DisplaySurface display(int width, int height, String title) {
        surface.opens++;
        surface.destroyed = false;
        return surface;
    }

    @Override
    public long currentTimeMillis() {
        return nanos / 1_000_000L;
    }

    @Override
    public long nanoTime() {
        return nanos / clockGrain * clockGrain;
    }

    @Override
    public void write(Level level, String message, Throwable error) {
    }

    @Override
    public String[] arguments() {
        return new String[0];
    }

    @Override
    public void catalog(String[] ids, String[] descriptions) {
    }

    @Override
    public void status(String phase, String detail) {
    }

    @Override
    public void ready() {
    }

    @Override
    public void exited(int code) {
    }

    @Override
    public void fatal(String phase, Throwable error) {
    }

    private final class Surface implements DisplaySurface {

        private final RecordingGraphicsDevice graphics = new RecordingGraphicsDevice();
        private final FakeInputSource input = new FakeInputSource();

        int presents;
        int opens;
        boolean destroyed;
        boolean focused = true;
        boolean visible = true;

        @Override
        public int width() {
            return 640;
        }

        @Override
        public int height() {
            return 360;
        }

        @Override
        public void setTitle(String title) {
        }

        @Override
        public void setSize(int width, int height) {
        }

        @Override
        public boolean isFullscreen() {
            return false;
        }

        @Override
        public boolean setFullscreen(boolean fullscreen) {
            return true;
        }

        @Override
        public boolean isFocused() {
            return focused;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public boolean isCloseRequested() {
            return destroyed;
        }

        @Override
        public void requestClose() {
        }

        @Override
        public GraphicsDevice graphics() {
            return graphics;
        }

        @Override
        public InputSource input() {
            return input;
        }

        @Override
        public void present() {
            presents++;
            nanos += frameNanos;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }
    }
}
