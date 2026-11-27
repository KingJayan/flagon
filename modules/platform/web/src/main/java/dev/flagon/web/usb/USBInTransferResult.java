package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.DataView;

/** https://developer.mozilla.org/docs/Web/API/USBInTransferResult */
public interface USBInTransferResult extends JSObject {
    @JSProperty
    DataView getData();

    /** ok, stall or babble */
    @JSProperty
    String getStatus();
}
