package dev.flagon.web.bluetooth;

import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.DataView;

/** https://developer.mozilla.org/docs/Web/API/BluetoothRemoteGATTCharacteristic */
public interface BluetoothRemoteGATTCharacteristic extends EventTarget {
    @JSProperty
    String getUuid();

    @JSProperty
    BluetoothRemoteGATTService getService();

    @JSProperty
    BluetoothCharacteristicProperties getProperties();

    /** @return the value last read or notified, or null before either */
    @JSProperty
    DataView getValue();

    JSPromise<DataView> readValue();

    JSPromise<JSUndefined> writeValueWithResponse(ArrayBufferView value);

    JSPromise<JSUndefined> writeValueWithoutResponse(ArrayBufferView value);

    JSPromise<BluetoothRemoteGATTCharacteristic> startNotifications();

    JSPromise<BluetoothRemoteGATTCharacteristic> stopNotifications();

    /** the new value is in getValue */
    default Registration onCharacteristicValueChanged(EventListener<Event> listener) {
        return onEvent("characteristicvaluechanged", listener);
    }
}
