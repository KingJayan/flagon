package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** where the video decoder sends its output and its errors */
public interface VideoDecoderInit extends JSObject {
    static VideoDecoderInit create(VideoFrameOutputCallback output,
            WebCodecsErrorCallback error) {
        VideoDecoderInit init = JSObjects.create();
        init.setOutput(output);
        init.setError(error);
        return init;
    }

    @JSProperty
    void setOutput(VideoFrameOutputCallback output);

    @JSProperty
    void setError(WebCodecsErrorCallback error);
}
