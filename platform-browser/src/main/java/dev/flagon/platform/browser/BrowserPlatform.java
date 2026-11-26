package dev.flagon.platform.browser;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.SilentAudioDevice;
import dev.flagon.platform.browser.audio.WebAudioDevice;
import dev.flagon.platform.browser.display.CanvasSurface;
import dev.flagon.platform.browser.fs.OpfsFileStore;
import dev.flagon.platform.browser.graphics.BrowserDrawing;
import dev.flagon.platform.browser.net.BrowserFetch;
import dev.flagon.platform.browser.net.ProxyNetwork;
import dev.flagon.platform.browser.workers.BrowserWorkers;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.MemoryFileStore;
import dev.flagon.platform.fs.StoreFileSystem;
import dev.flagon.platform.graphics.Drawing;
import dev.flagon.platform.net.Fetch;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.UnavailableNetwork;
import dev.flagon.platform.workers.Workers;
import org.teavm.jso.JSBody;

public final class BrowserPlatform implements Platform {

    private final Clock clock;
    private final ConsoleSink console;
    private final RuntimeHost host;
    private final FileStore files;
    private final AudioDevice audio;
    private final Network network;
    private final Fetch fetch = new BrowserFetch();
    private final Drawing drawing = new BrowserDrawing();
    private final Workers workers = new BrowserWorkers();
    private CanvasSurface surface;

    private BrowserPlatform(Clock clock, ConsoleSink console, RuntimeHost host, FileStore files,
                            AudioDevice audio, Network network) {
        this.clock = clock;
        this.console = console;
        this.host = host;
        this.files = files;
        this.audio = audio;
        this.network = network;
    }

    public static BrowserPlatform create(String[] arguments) {
        HostBridge bridge = HostBridges.installed();
        BrowserConsole console = new BrowserConsole(bridge);
        if (bridge == null) {
            console.warn("no self.__flagonHost installed; reporting to the console only");
        }
        RuntimeHost host = new BrowserRuntimeHost(bridge, console, copyOf(arguments));
        ResourcePack.install(console);
        FileStore files = openStorage(console);
        StoreFileSystem.install(files);
        Clock clock = new BrowserClock();
        return new BrowserPlatform(clock, console, host, files, openAudio(console, clock),
                openNetwork(console));
    }

    /**
     web audio - silent mixer if not
     */
    private static AudioDevice openAudio(ConsoleSink console, Clock clock) {
        if (inWorker()) {
            return new SilentAudioDevice(clock);
        }
        WebAudioDevice audio = WebAudioDevice.createIfAvailable(console);
        if (audio != null) {
            return audio;
        }
        console.warn("no web audio here; sound is tracked but never heard");
        return new SilentAudioDevice(clock);
    }

    private static Network openNetwork(ConsoleSink console) {
        ProxyNetwork network = ProxyNetwork.createIfAvailable();
        if (network != null) {
            return network;
        }
        console.warn("no websockets here; nothing can be reached over the network");
        return new UnavailableNetwork("this browser has no websockets to reach a proxy through");
    }

    private static FileStore openStorage(ConsoleSink console) {
        FileStore opfs = OpfsFileStore.createIfAvailable();
        if (opfs != null) {
            return opfs;
        }
        console.warn("no usable origin private file system here; files are kept in memory and"
                + " lost on reload");
        return new MemoryFileStore();
    }

    private static String[] copyOf(String[] arguments) {
        if (arguments == null) {
            return new String[0];
        }
        Object[] raw = arguments;
        String[] copy = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            copy[i] = raw[i] == null ? "" : raw[i].toString();
        }
        return copy;
    }

    @Override
    public String name() {
        return "browser";
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
    public Fetch fetch() {
        return fetch;
    }

    @Override
    public Drawing drawing() {
        return drawing;
    }

    @Override
    public Workers workers() {
        return workers;
    }

    @Override
    public boolean hasDisplay() {
        return !inWorker();
    }

    /** a bundle worker has no document, so no canvas, input or audio output */
    @JSBody(script = "return typeof document === 'undefined';")
    private static native boolean inWorker();

    @Override
    public DisplaySurface display(int width, int height, String title) {
        if (inWorker()) {
            throw new UnsupportedCapabilityException("a bundle worker has no page to open a"
                    + " window on");
        }
        if (surface == null || surface.isDestroyed()) {
            surface = CanvasSurface.open(width, height, title, clock);
        }
        return surface;
    }
}
