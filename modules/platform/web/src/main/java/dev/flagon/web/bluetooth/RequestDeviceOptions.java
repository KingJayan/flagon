package dev.flagon.web.bluetooth;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/Bluetooth/requestDevice#options */
public interface RequestDeviceOptions extends JSObject {
    static RequestDeviceOptions create(BluetoothLEScanFilter... filters) {
        RequestDeviceOptions options = JSObjects.create();
        options.setFilters(filters);
        return options;
    }

    /** offers every device, and takes the place of the filters */
    static RequestDeviceOptions acceptAll() {
        RequestDeviceOptions options = JSObjects.create();
        options.setAcceptAllDevices(true);
        return options;
    }

    @JSProperty
    void setFilters(BluetoothLEScanFilter[] filters);

    @JSProperty
    void setAcceptAllDevices(boolean acceptAllDevices);

    /** services the page may use beyond those the filters name */
    @JSProperty
    void setOptionalServices(String[] services);
}
