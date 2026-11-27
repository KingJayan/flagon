package dev.flagon.web.codecs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

/** https://developer.mozilla.org/docs/Web/API/AudioEncoder */
@JSClass
public class AudioEncoder implements JSObject {

    public AudioEncoder(AudioEncoderInit init) {
    }

    @JSBody(script = "return typeof AudioEncoder !== 'undefined';")
    public static native boolean isSupported();

    public static native JSPromise<CodecSupport<AudioEncoderConfig>> isConfigSupported(
            AudioEncoderConfig config);

    /** unconfigured, configured or closed */
    @JSProperty
    public native String getState();

    @JSProperty
    public native int getEncodeQueueSize();

    public native void configure(AudioEncoderConfig config);

    public native void encode(AudioData input);

    /** settles once every queued input has come out */
    public native JSPromise<JSUndefined> flush();

    public native void reset();

    public native void close();
}
