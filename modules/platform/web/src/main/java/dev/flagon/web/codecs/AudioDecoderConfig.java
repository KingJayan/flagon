package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/AudioDecoder/configure#config */
public interface AudioDecoderConfig extends JSObject {
    static AudioDecoderConfig create(String codec, int sampleRate, int numberOfChannels) {
        AudioDecoderConfig config = JSObjects.create();
        config.setCodec(codec);
        config.setSampleRate(sampleRate);
        config.setNumberOfChannels(numberOfChannels);
        return config;
    }

    @JSProperty
    String getCodec();

    @JSProperty
    void setCodec(String codec);

    @JSProperty
    void setSampleRate(int sampleRate);

    @JSProperty
    void setNumberOfChannels(int numberOfChannels);

    @JSProperty
    void setDescription(ArrayBufferView description);
}
