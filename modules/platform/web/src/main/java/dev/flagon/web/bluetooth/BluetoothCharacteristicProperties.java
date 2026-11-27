package dev.flagon.web.bluetooth;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/** https://developer.mozilla.org/docs/Web/API/BluetoothCharacteristicProperties */
public interface BluetoothCharacteristicProperties extends JSObject {
    @JSProperty
    boolean isRead();

    @JSProperty
    boolean isWrite();

    @JSProperty
    boolean isWriteWithoutResponse();

    @JSProperty
    boolean isNotify();

    @JSProperty
    boolean isIndicate();
}
