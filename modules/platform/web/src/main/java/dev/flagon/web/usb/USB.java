package dev.flagon.web.usb;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.Registration;

/** https://developer.mozilla.org/docs/Web/API/USB */
public abstract class USB implements EventTarget {

    public static boolean isSupported() {
        return hasNavigator() && hasUSB();
    }

    /** @return navigator.usb, or null without it */
    public static USB get() {
        return isSupported() ? usb() : null;
    }

    /** shows the device chooser, which needs a click or key press to start it */
    public abstract JSPromise<USBDevice> requestDevice(USBDeviceRequestOptions options);

    /** @return the devices the page may already use */
    public abstract JSPromise<JSArray<USBDevice>> getDevices();

    public final Registration onConnect(EventListener<USBConnectionEvent> listener) {
        return onEvent("connect", listener);
    }

    public final Registration onDisconnect(EventListener<USBConnectionEvent> listener) {
        return onEvent("disconnect", listener);
    }

    @JSBody(script = "return typeof navigator !== 'undefined';")
    private static native boolean hasNavigator();

    @JSBody(script = "return navigator.usb != null;")
    private static native boolean hasUSB();

    @JSBody(script = "return navigator.usb;")
    private static native USB usb();
}
