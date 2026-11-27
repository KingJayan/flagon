package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** what an EncodedVideoChunk or EncodedAudioChunk is made from */
public interface EncodedChunkInit extends JSObject {
    /**
     * @param type key or delta
     * @param timestamp in microseconds
     */
    static EncodedChunkInit create(String type, double timestamp, ArrayBufferView data) {
        EncodedChunkInit init = JSObjects.create();
        init.setType(type);
        init.setTimestamp(timestamp);
        init.setData(data);
        return init;
    }

    @JSProperty
    void setType(String type);

    @JSProperty
    void setTimestamp(double timestamp);

    @JSProperty
    void setDuration(double duration);

    @JSProperty
    void setData(ArrayBufferView data);
}
