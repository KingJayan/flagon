package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/VideoFrame/VideoFrame#options */
public interface VideoFrameInit extends JSObject {
    /** @param timestamp in microseconds */
    static VideoFrameInit create(double timestamp) {
        VideoFrameInit init = JSObjects.create();
        init.setTimestamp(timestamp);
        return init;
    }

    @JSProperty
    void setTimestamp(double timestamp);

    @JSProperty
    void setDuration(double duration);
}
