package dev.flagon.web.codecs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

/** https://developer.mozilla.org/docs/Web/API/AudioDecoder */
@JSClass
public class AudioDecoder implements JSObject {

    public AudioDecoder(AudioDecoderInit init) {
    }

    @JSBody(script = "return typeof AudioDecoder !== 'undefined';")
    public static native boolean isSupported();

    public static native JSPromise<CodecSupport<AudioDecoderConfig>> isConfigSupported(
            AudioDecoderConfig config);

    /** unconfigured, configured or closed */
    @JSProperty
    public native String getState();

    @JSProperty
    public native int getDecodeQueueSize();

    public native void configure(AudioDecoderConfig config);

    public native void decode(EncodedAudioChunk input);

    /** settles once every queued input has come out */
    public native JSPromise<JSUndefined> flush();

    public native void reset();

    public native void close();
}
