package dev.flagon.web.codecs;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** takes each chunk a video encoder makes; metadata carries the decoder config on key frames */
@JSFunctor
public interface EncodedVideoChunkOutputCallback extends JSObject {
    void call(EncodedVideoChunk chunk, JSObject metadata);
}
