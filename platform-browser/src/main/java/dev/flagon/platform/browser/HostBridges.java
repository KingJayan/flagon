package dev.flagon.platform.browser;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSObjects;

final class HostBridges {

    private HostBridges() {
    }

    private static final String[] CALLS = {"log", "status", "ready", "exited", "fatal"};

    interface Hosting extends JSObject {
        @JSProperty("__flagonHost")
        HostBridge getHost();
    }

    interface Named extends JSObject {
        @JSIndexer
        JSObject get(String name);
    }

    static HostBridge installed() {
        HostBridge host = Window.current().<Hosting>cast().getHost();
        if (host == null || JSObjects.isUndefined(host)) {
            return null;
        }
        Named calls = host.cast();
        for (String call : CALLS) {
            if (!"function".equals(JSObjects.typeOf(calls.get(call)))) {
                warn("[flagon] ignoring window.__flagonHost: missing " + call + "()");
                return null;
            }
        }
        return host;
    }

    @JSBody(params = "text", script = "console.warn(text);")
    private static native void warn(String text);

    @JSBody(params = "host", script = "return typeof host.catalog === 'function';")
    static native boolean offersCatalog(HostBridge host);
}
