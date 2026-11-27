package dev.flagon.web.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/RTCIceCandidate/RTCIceCandidate#candidateinfo */
public interface RTCIceCandidateInit extends JSObject {
    static RTCIceCandidateInit create(String candidate, String sdpMid) {
        RTCIceCandidateInit init = JSObjects.create();
        init.setCandidate(candidate);
        init.setSdpMid(sdpMid);
        return init;
    }

    @JSProperty
    String getCandidate();

    @JSProperty
    void setCandidate(String candidate);

    @JSProperty
    String getSdpMid();

    @JSProperty
    void setSdpMid(String sdpMid);

    @JSProperty
    int getSdpMLineIndex();

    @JSProperty
    void setSdpMLineIndex(int sdpMLineIndex);
}
