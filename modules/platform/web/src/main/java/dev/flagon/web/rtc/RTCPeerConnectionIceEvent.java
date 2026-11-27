package dev.flagon.web.rtc;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

/** https://developer.mozilla.org/docs/Web/API/RTCPeerConnectionIceEvent */
public interface RTCPeerConnectionIceEvent extends Event {
    /** @return the candidate, which addIceCandidate takes as it is, or null at the end */
    @JSProperty
    RTCIceCandidateInit getCandidate();
}
