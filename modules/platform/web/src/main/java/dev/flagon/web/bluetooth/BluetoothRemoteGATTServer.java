package dev.flagon.web.bluetooth;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;

/** https://developer.mozilla.org/docs/Web/API/BluetoothRemoteGATTServer */
public interface BluetoothRemoteGATTServer extends JSObject {
    @JSProperty
    boolean isConnected();

    @JSProperty
    BluetoothDevice getDevice();

    JSPromise<BluetoothRemoteGATTServer> connect();

    void disconnect();

    JSPromise<BluetoothRemoteGATTService> getPrimaryService(String service);

    JSPromise<JSArray<BluetoothRemoteGATTService>> getPrimaryServices();
}
