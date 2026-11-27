package dev.flagon.web.usb;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

/** https://developer.mozilla.org/docs/Web/API/USBConnectionEvent */
public interface USBConnectionEvent extends Event {
    @JSProperty
    USBDevice getDevice();
}
