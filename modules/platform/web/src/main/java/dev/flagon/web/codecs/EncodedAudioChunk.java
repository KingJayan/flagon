package dev.flagon.web.codecs;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/EncodedAudioChunk */
@JSClass
public class EncodedAudioChunk implements JSObject {

    public EncodedAudioChunk(EncodedChunkInit init) {
    }

    /** key or delta */
    @JSProperty
    public native String getType();

    /** in microseconds */
    @JSProperty
    public native double getTimestamp();

    /** in microseconds */
    @JSProperty
    public native double getDuration();

    @JSProperty
    public native int getByteLength();

    public native void copyTo(ArrayBufferView destination);
}
