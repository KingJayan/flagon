package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/AudioEncoder/configure#config */
public interface AudioEncoderConfig extends JSObject {
    /** @param codec a codec string, such as opus or mp4a.40.2 */
    static AudioEncoderConfig create(String codec, int sampleRate, int numberOfChannels) {
        AudioEncoderConfig config = JSObjects.create();
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
    int getSampleRate();

    @JSProperty
    void setSampleRate(int sampleRate);

    @JSProperty
    int getNumberOfChannels();

    @JSProperty
    void setNumberOfChannels(int numberOfChannels);

    /** in bits per second */
    @JSProperty
    void setBitrate(double bitrate);
}
