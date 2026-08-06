package dev.flagon.platform.browser;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.RuntimeHost;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

public final class BrowserRuntimeHost implements RuntimeHost {

    private final HostBridge bridge;
    private final ConsoleSink console;
    private final String[] arguments;

    BrowserRuntimeHost(HostBridge bridge, ConsoleSink console, String[] arguments) {
        this.bridge = bridge;
        this.console = console;
        this.arguments = arguments;
    }

    @Override
    public String[] arguments() {
        return arguments.clone();
    }

    @Override
    public void catalog(String[] ids, String[] descriptions) {
        if (bridge == null || !HostBridges.offersCatalog(bridge)) {
            return;
        }
        bridge.catalog(strings(ids), strings(descriptions));
    }

    private static JSArray<JSString> strings(String[] values) {
        JSArray<JSString> array = JSArray.of();
        for (String value : values) {
            array.push(JSString.valueOf(value));
        }
        return array;
    }

    @Override
    public void status(String phase, String detail) {
        console.debug("phase '" + phase + "': " + detail);
        if (bridge != null) {
            bridge.status(phase, detail);
        }
    }

    @Override
    public void ready() {
        console.info("runtime ready");
        if (bridge != null) {
            bridge.ready();
        }
    }

    @Override
    public void exited(int code) {
        console.info("program exited with code " + code);
        if (bridge != null) {
            bridge.exited(code);
        }
    }

    @Override
    public void fatal(String phase, Throwable error) {
        // the console copy was already written by FlagonRuntime before it called us
        if (bridge == null) {
            return;
        }
        String message = error == null ? "unknown error" : String.valueOf(error.getMessage());
        String stack = error == null ? "" : BrowserConsole.describe(error);
        bridge.fatal(phase, message, stack);
    }
}
