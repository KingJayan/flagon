package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/USB/requestDevice#options */
public interface USBDeviceRequestOptions extends JSObject {
    /** @param filters no filters offers every device */
    static USBDeviceRequestOptions create(USBDeviceFilter... filters) {
        USBDeviceRequestOptions options = JSObjects.create();
        options.setFilters(filters);
        return options;
    }

    @JSProperty
    void setFilters(USBDeviceFilter[] filters);
}
