package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** where the audio encoder sends its output and its errors */
public interface AudioEncoderInit extends JSObject {
    static AudioEncoderInit create(EncodedAudioChunkOutputCallback output,
            WebCodecsErrorCallback error) {
        AudioEncoderInit init = JSObjects.create();
        init.setOutput(output);
        init.setError(error);
        return init;
    }

    @JSProperty
    void setOutput(EncodedAudioChunkOutputCallback output);

    @JSProperty
    void setError(WebCodecsErrorCallback error);
}
