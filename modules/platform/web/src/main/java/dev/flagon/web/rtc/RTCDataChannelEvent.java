package dev.flagon.web.rtc;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

/** https://developer.mozilla.org/docs/Web/API/RTCDataChannelEvent */
public interface RTCDataChannelEvent extends Event {
    @JSProperty
    RTCDataChannel getChannel();
}
