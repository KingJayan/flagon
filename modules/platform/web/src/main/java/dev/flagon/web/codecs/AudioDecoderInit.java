package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** where the audio decoder sends its output and its errors */
public interface AudioDecoderInit extends JSObject {
    static AudioDecoderInit create(AudioDataOutputCallback output,
            WebCodecsErrorCallback error) {
        AudioDecoderInit init = JSObjects.create();
        init.setOutput(output);
        init.setError(error);
        return init;
    }

    @JSProperty
    void setOutput(AudioDataOutputCallback output);

    @JSProperty
    void setError(WebCodecsErrorCallback error);
}
