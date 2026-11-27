package dev.flagon.web.bluetooth;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.Registration;

/** https://developer.mozilla.org/docs/Web/API/BluetoothDevice */
public interface BluetoothDevice extends EventTarget {
    @JSProperty
    String getId();

    @JSProperty
    String getName();

    @JSProperty
    BluetoothRemoteGATTServer getGatt();

    default Registration onGattServerDisconnected(EventListener<Event> listener) {
        return onEvent("gattserverdisconnected", listener);
    }
}
