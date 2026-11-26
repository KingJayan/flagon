package dev.flagon.platform.wasi;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.SilentAudioDevice;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.MemoryFileStore;
import dev.flagon.platform.fs.StoreFileSystem;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.UnavailableNetwork;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

/** the headless platform of a wasi component: a console, a clock, args, an exit status and files */
public final class WasiPlatform implements Platform {

    private final Clock clock = new WasiClock();
    private final ConsoleSink console = new WasiConsole();
    private final RuntimeHost host;
    private final FileStore files;
    private final AudioDevice audio = new SilentAudioDevice(clock);
    private final Network network = new UnavailableNetwork("a wasi component has no sockets");

    private WasiPlatform(String[] arguments) {
        host = new WasiRuntimeHost(arguments == null ? new String[0] : arguments.clone());
        // with no directory to read and write, files live as long as the component runs
        FileStore preopened = WasiFileStore.createIfAvailable();
        files = preopened == null ? new MemoryFileStore() : preopened;
        // wasi file calls return when they are done, so writes need no thread of their own
        VirtualFileSystemProvider.setInstance(new StoreFileSystem(files));
    }

    /** {@code java.io} works below the first directory the host preopened, as {@code /} */
    public static WasiPlatform create(String[] arguments) {
        return new WasiPlatform(arguments);
    }

    @Override
    public String name() {
        return "wasi";
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public ConsoleSink console() {
        return console;
    }

    @Override
    public RuntimeHost host() {
        return host;
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
        return false;
    }

    @Override
    public DisplaySurface display(int width, int height, String title) {
        throw new UnsupportedCapabilityException("a wasi component has no window to open");
    }
}
