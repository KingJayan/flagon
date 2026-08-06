package dev.flagon.platform.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

/** what a page installs as {@code window.__flagonHost} to observe the runtime. */
public interface HostBridge extends JSObject {

    void log(String level, String text);

    void catalog(JSArray<JSString> ids, JSArray<JSString> descriptions);

    void status(String phase, String detail);

    void ready();

    void exited(int code);

    void fatal(String phase, String message, String stack);
}
