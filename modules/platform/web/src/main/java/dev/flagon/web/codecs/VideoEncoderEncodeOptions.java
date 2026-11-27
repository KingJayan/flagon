package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/VideoEncoder/encode#options */
public interface VideoEncoderEncodeOptions extends JSObject {
    static VideoEncoderEncodeOptions keyFrame() {
        VideoEncoderEncodeOptions options = JSObjects.create();
        options.setKeyFrame(true);
        return options;
    }

    @JSProperty
    void setKeyFrame(boolean keyFrame);
}
