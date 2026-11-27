package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/AudioData/AudioData#options */
public interface AudioDataInit extends JSObject {
    /**
     * @param format such as f32 or s16-planar
     * @param timestamp in microseconds
     */
    static AudioDataInit create(String format, float sampleRate, int numberOfFrames,
            int numberOfChannels, double timestamp, ArrayBufferView data) {
        AudioDataInit init = JSObjects.create();
        init.setFormat(format);
        init.setSampleRate(sampleRate);
        init.setNumberOfFrames(numberOfFrames);
        init.setNumberOfChannels(numberOfChannels);
        init.setTimestamp(timestamp);
        init.setData(data);
        return init;
    }

    @JSProperty
    void setFormat(String format);

    @JSProperty
    void setSampleRate(float sampleRate);

    @JSProperty
    void setNumberOfFrames(int numberOfFrames);

    @JSProperty
    void setNumberOfChannels(int numberOfChannels);

    @JSProperty
    void setTimestamp(double timestamp);

    @JSProperty
    void setData(ArrayBufferView data);
}
