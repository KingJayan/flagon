package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/** https://developer.mozilla.org/docs/Web/API/USBOutTransferResult */
public interface USBOutTransferResult extends JSObject {
    @JSProperty
    int getBytesWritten();

    /** ok or stall */
    @JSProperty
    String getStatus();
}
