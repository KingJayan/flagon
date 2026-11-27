package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** where the video encoder sends its output and its errors */
public interface VideoEncoderInit extends JSObject {
    static VideoEncoderInit create(EncodedVideoChunkOutputCallback output,
            WebCodecsErrorCallback error) {
        VideoEncoderInit init = JSObjects.create();
        init.setOutput(output);
        init.setError(error);
        return init;
    }

    @JSProperty
    void setOutput(EncodedVideoChunkOutputCallback output);

    @JSProperty
    void setError(WebCodecsErrorCallback error);
}
