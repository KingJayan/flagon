package dev.flagon.platform.browser;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.browser.display.CanvasSurface;
import dev.flagon.platform.browser.fs.OpfsFileStore;
import dev.flagon.platform.browser.fs.StoreFileSystem;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.MemoryFileStore;

public final class BrowserPlatform implements Platform {

    private final Clock clock;
    private final ConsoleSink console;
    private final RuntimeHost host;
    private final FileStore files;
    private DisplaySurface surface;

    private BrowserPlatform(Clock clock, ConsoleSink console, RuntimeHost host, FileStore files) {
        this.clock = clock;
        this.console = console;
        this.host = host;
        this.files = files;
    }

    /** @param arguments the entry point's own arguments, as the page passed them to main */
    public static BrowserPlatform create(String[] arguments) {
        HostBridge bridge = HostBridges.installed();
        BrowserConsole console = new BrowserConsole(bridge);
        if (bridge == null) {
            console.warn("no window.__flagonHost installed; reporting to the console only");
        }
        RuntimeHost host = new BrowserRuntimeHost(bridge, console, copyOf(arguments));
        FileStore files = openStorage(console);
        // installed before anything can open a file, because java.io resolves the filesystem once
        StoreFileSystem.install(files);
        return new BrowserPlatform(new BrowserClock(), console, host, files);
    }

    /**
     * the origin private file system where the browser has one. where it does not, files live in
     * the heap and are lost on reload — said out loud, because an application that saves its work
     * would otherwise appear to succeed.
     */
    private static FileStore openStorage(ConsoleSink console) {
        FileStore opfs = OpfsFileStore.createIfAvailable();
        if (opfs != null) {
            return opfs;
        }
        console.warn("no origin private file system here; files are kept in memory and lost on reload");
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
    public boolean hasDisplay() {
        return true;
    }

    @Override
    public DisplaySurface display(int width, int height, String title) {
        if (surface == null) {
            surface = CanvasSurface.open(width, height, title, clock);
        }
        return surface;
    }
}
