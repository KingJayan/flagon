package dev.flagon.web.codecs;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** takes each chunk an audio encoder makes */
@JSFunctor
public interface EncodedAudioChunkOutputCallback extends JSObject {
    void call(EncodedAudioChunk chunk, JSObject metadata);
}
