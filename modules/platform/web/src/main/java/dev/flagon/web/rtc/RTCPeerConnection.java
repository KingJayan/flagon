package dev.flagon.web.rtc;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.Registration;

/** https://developer.mozilla.org/docs/Web/API/RTCPeerConnection */
@JSClass
public class RTCPeerConnection implements EventTarget {

    public RTCPeerConnection() {
    }

    public RTCPeerConnection(RTCConfiguration configuration) {
    }

    @JSBody(script = "return typeof RTCPeerConnection !== 'undefined';")
    public static native boolean isSupported();

    public native JSPromise<RTCSessionDescriptionInit> createOffer();

    public native JSPromise<RTCSessionDescriptionInit> createAnswer();

    /** describes itself from its own offer or answer */
    public native JSPromise<JSUndefined> setLocalDescription();

    public native JSPromise<JSUndefined> setLocalDescription(RTCSessionDescriptionInit description);

    public native JSPromise<JSUndefined> setRemoteDescription(
            RTCSessionDescriptionInit description);

    public native JSPromise<JSUndefined> addIceCandidate(RTCIceCandidateInit candidate);

    public native RTCDataChannel createDataChannel(String label);

    public native RTCDataChannel createDataChannel(String label, RTCDataChannelInit options);

    @JSProperty
    public native RTCSessionDescriptionInit getLocalDescription();

    @JSProperty
    public native RTCSessionDescriptionInit getRemoteDescription();

    /** new, connecting, connected, disconnected, failed or closed */
    @JSProperty
    public native String getConnectionState();

    @JSProperty
    public native String getIceConnectionState();

    /** new, gathering or complete */
    @JSProperty
    public native String getIceGatheringState();

    @JSProperty
    public native String getSignalingState();

    public native void close();

    /** the candidate is null once gathering ends */
    public final Registration onIceCandidate(EventListener<RTCPeerConnectionIceEvent> listener) {
        return onEvent("icecandidate", listener);
    }

    public final Registration onDataChannel(EventListener<RTCDataChannelEvent> listener) {
        return onEvent("datachannel", listener);
    }

    public final Registration onConnectionStateChange(EventListener<Event> listener) {
        return onEvent("connectionstatechange", listener);
    }

    public final Registration onNegotiationNeeded(EventListener<Event> listener) {
        return onEvent("negotiationneeded", listener);
    }

    @Override
    public native void addEventListener(String type, EventListener<?> listener,
            boolean useCapture);

    @Override
    public native void addEventListener(String type, EventListener<?> listener);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener,
            boolean useCapture);

    @Override
    public native void removeEventListener(String type, EventListener<?> listener);

    @Override
    public native boolean dispatchEvent(Event event);
}
