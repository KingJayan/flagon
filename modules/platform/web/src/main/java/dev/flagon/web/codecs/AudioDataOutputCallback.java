package dev.flagon.web.codecs;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** takes each block of samples an audio decoder makes, which it must close */
@JSFunctor
public interface AudioDataOutputCallback extends JSObject {
    void call(AudioData data);
}
