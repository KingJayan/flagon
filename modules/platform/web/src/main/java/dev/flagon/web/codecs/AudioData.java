package dev.flagon.web.codecs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/AudioData */
@JSClass
public class AudioData implements JSObject {

    public AudioData(AudioDataInit init) {
    }

    @JSBody(script = "return typeof AudioData !== 'undefined';")
    public static native boolean isSupported();

    /** u8, s16, s32 or f32, with a -planar suffix when each channel has its own plane */
    @JSProperty
    public native String getFormat();

    @JSProperty
    public native float getSampleRate();

    @JSProperty
    public native int getNumberOfFrames();

    @JSProperty
    public native int getNumberOfChannels();

    /** in microseconds */
    @JSProperty
    public native double getTimestamp();

    /** in microseconds */
    @JSProperty
    public native double getDuration();

    public native int allocationSize(AudioDataCopyToOptions options);

    public native void copyTo(ArrayBufferView destination, AudioDataCopyToOptions options);

    public native AudioData clone();

    public native void close();
}
