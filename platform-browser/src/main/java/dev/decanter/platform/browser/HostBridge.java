package dev.decanter.platform.browser;

import org.teavm.jso.JSObject;

/** what a page installs as {@code window.__decanterHost} to observe the runtime. */
public interface HostBridge extends JSObject {

    void log(String level, String text);

    void status(String phase, String detail);

    void ready();

    void exited(int code);

    void fatal(String phase, String message, String stack);
}
