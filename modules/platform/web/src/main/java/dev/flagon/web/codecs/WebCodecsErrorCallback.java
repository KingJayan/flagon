package dev.flagon.web.codecs;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** takes the DOMException that closed a coder */
@JSFunctor
public interface WebCodecsErrorCallback extends JSObject {
    void call(JSObject error);
}
