package dev.flagon.web.rtc;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/RTCDataChannel */
public interface RTCDataChannel extends EventTarget {
    @JSProperty
    String getLabel();

    @JSProperty
    int getId();

    /** connecting, open, closing or closed */
    @JSProperty
    String getReadyState();

    @JSProperty
    int getBufferedAmount();

    /** blob or arraybuffer */
    @JSProperty
    void setBinaryType(String binaryType);

    void send(String data);

    void send(ArrayBuffer data);

    void send(ArrayBufferView data);

    void close();

    default Registration onOpen(EventListener<Event> listener) {
        return onEvent("open", listener);
    }

    default Registration onMessage(EventListener<MessageEvent> listener) {
        return onEvent("message", listener);
    }

    default Registration onClose(EventListener<Event> listener) {
        return onEvent("close", listener);
    }

    default Registration onError(EventListener<Event> listener) {
        return onEvent("error", listener);
    }
}
