package dev.flagon.web.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/RTCPeerConnection/createDataChannel#options */
public interface RTCDataChannelInit extends JSObject {
    static RTCDataChannelInit create() {
        return JSObjects.create();
    }

    @JSProperty
    void setOrdered(boolean ordered);

    @JSProperty
    void setMaxRetransmits(int maxRetransmits);

    @JSProperty
    void setMaxPacketLifeTime(int milliseconds);

    @JSProperty
    void setProtocol(String protocol);

    /** both peers make the channel with the same id, and no datachannel event fires */
    @JSProperty
    void setNegotiated(boolean negotiated);

    @JSProperty
    void setId(int id);
}
