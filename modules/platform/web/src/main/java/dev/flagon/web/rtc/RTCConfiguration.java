package dev.flagon.web.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/RTCPeerConnection/RTCPeerConnection#configuration */
public interface RTCConfiguration extends JSObject {
    static RTCConfiguration create(RTCIceServer... iceServers) {
        RTCConfiguration configuration = JSObjects.create();
        configuration.setIceServers(iceServers);
        return configuration;
    }

    @JSProperty
    void setIceServers(RTCIceServer[] iceServers);

    /** all, or relay to use only turn servers */
    @JSProperty
    void setIceTransportPolicy(String policy);
}
