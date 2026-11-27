package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/** https://developer.mozilla.org/docs/Web/API/USBConfiguration */
public interface USBConfiguration extends JSObject {
    @JSProperty
    int getConfigurationValue();

    @JSProperty
    String getConfigurationName();
}
