package dev.flagon.web.codecs;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/** takes each frame a video decoder makes, which it must close */
@JSFunctor
public interface VideoFrameOutputCallback extends JSObject {
    void call(VideoFrame frame);
}
