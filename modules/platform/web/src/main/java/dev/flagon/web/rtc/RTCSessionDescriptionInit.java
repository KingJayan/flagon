package dev.flagon.web.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/RTCSessionDescription */
public interface RTCSessionDescriptionInit extends JSObject {
    /** @param type offer, answer, pranswer or rollback */
    static RTCSessionDescriptionInit create(String type, String sdp) {
        RTCSessionDescriptionInit init = JSObjects.create();
        init.setType(type);
        init.setSdp(sdp);
        return init;
    }

    @JSProperty
    String getType();

    @JSProperty
    void setType(String type);

    @JSProperty
    String getSdp();

    @JSProperty
    void setSdp(String sdp);
}
