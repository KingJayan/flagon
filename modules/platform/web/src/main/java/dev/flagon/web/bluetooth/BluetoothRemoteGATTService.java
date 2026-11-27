package dev.flagon.web.bluetooth;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;

/** https://developer.mozilla.org/docs/Web/API/BluetoothRemoteGATTService */
public interface BluetoothRemoteGATTService extends JSObject {
    @JSProperty
    String getUuid();

    @JSProperty
    boolean isPrimary();

    @JSProperty
    BluetoothDevice getDevice();

    JSPromise<BluetoothRemoteGATTCharacteristic> getCharacteristic(String characteristic);

    JSPromise<JSArray<BluetoothRemoteGATTCharacteristic>> getCharacteristics();
}
