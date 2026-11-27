package dev.flagon.web.bluetooth;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** a device filter of RequestDeviceOptions */
public interface BluetoothLEScanFilter extends JSObject {
    /** @param services uuids or names, such as heart_rate */
    static BluetoothLEScanFilter services(String... services) {
        BluetoothLEScanFilter filter = JSObjects.create();
        filter.setServices(services);
        return filter;
    }

    static BluetoothLEScanFilter namePrefix(String prefix) {
        BluetoothLEScanFilter filter = JSObjects.create();
        filter.setNamePrefix(prefix);
        return filter;
    }

    @JSProperty
    void setServices(String[] services);

    @JSProperty
    void setName(String name);

    @JSProperty
    void setNamePrefix(String namePrefix);
}
