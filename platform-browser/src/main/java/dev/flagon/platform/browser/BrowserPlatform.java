package dev.flagon.platform.browser;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.browser.display.CanvasSurface;
import dev.flagon.platform.display.DisplaySurface;

public final class BrowserPlatform implements Platform {

    private final Clock clock;
    private final ConsoleSink console;
    private final RuntimeHost host;
    private DisplaySurface surface;

    private BrowserPlatform(Clock clock, ConsoleSink console, RuntimeHost host) {
        this.clock = clock;
        this.console = console;
        this.host = host;
    }

    /** @param arguments the entry point's own arguments, as the page passed them to main */
    public static BrowserPlatform create(String[] arguments) {
        HostBridge bridge = HostBridges.installed();
        BrowserConsole console = new BrowserConsole(bridge);
        if (bridge == null) {
            console.warn("no window.__flagonHost installed; reporting to the console only");
        }
        RuntimeHost host = new BrowserRuntimeHost(bridge, console, copyOf(arguments));
        return new BrowserPlatform(new BrowserClock(), console, host);
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
    public boolean hasDisplay() {
        return true;
    }

    @Override
    public DisplaySurface display(int width, int height, String title) {
        if (surface == null) {
            surface = CanvasSurface.open(width, height, title);
        }
        return surface;
    }
}
