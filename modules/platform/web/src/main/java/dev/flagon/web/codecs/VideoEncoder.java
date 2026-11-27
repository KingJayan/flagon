package dev.flagon.web.codecs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

/** https://developer.mozilla.org/docs/Web/API/VideoEncoder */
@JSClass
public class VideoEncoder implements JSObject {

    public VideoEncoder(VideoEncoderInit init) {
    }

    @JSBody(script = "return typeof VideoEncoder !== 'undefined';")
    public static native boolean isSupported();

    public static native JSPromise<CodecSupport<VideoEncoderConfig>> isConfigSupported(
            VideoEncoderConfig config);

    /** unconfigured, configured or closed */
    @JSProperty
    public native String getState();

    @JSProperty
    public native int getEncodeQueueSize();

    public native void configure(VideoEncoderConfig config);

    public native void encode(VideoFrame input);

    public native void encode(VideoFrame frame, VideoEncoderEncodeOptions options);

    /** settles once every queued input has come out */
    public native JSPromise<JSUndefined> flush();

    public native void reset();

    public native void close();
}
