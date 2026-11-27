package dev.flagon.web.codecs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/VideoFrame */
@JSClass
public class VideoFrame implements JSObject {

    /** @param image a canvas, image, video, ImageBitmap or another frame */
    public VideoFrame(JSObject image, VideoFrameInit init) {
    }

    public VideoFrame(ArrayBufferView data, VideoFrameBufferInit init) {
    }

    @JSBody(script = "return typeof VideoFrame !== 'undefined';")
    public static native boolean isSupported();

    /** I420, NV12, RGBA, BGRA and others, or null when the pixels are opaque */
    @JSProperty
    public native String getFormat();

    @JSProperty
    public native int getCodedWidth();

    @JSProperty
    public native int getCodedHeight();

    @JSProperty
    public native int getDisplayWidth();

    @JSProperty
    public native int getDisplayHeight();

    /** in microseconds */
    @JSProperty
    public native double getTimestamp();

    /** in microseconds */
    @JSProperty
    public native double getDuration();

    /** @return the bytes copyTo writes */
    public native int allocationSize();

    /** @return a promise of the plane layout */
    public native JSPromise<JSObject> copyTo(ArrayBufferView destination);

    public native VideoFrame clone();

    /** frees the frame's memory, which garbage collection is too late for */
    public native void close();
}
