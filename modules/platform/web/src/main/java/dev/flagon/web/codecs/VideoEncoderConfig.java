package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/VideoEncoder/configure#config */
public interface VideoEncoderConfig extends JSObject {
    /** @param codec a codec string, such as vp8, vp09.00.10.08 or avc1.42001f */
    static VideoEncoderConfig create(String codec, int width, int height) {
        VideoEncoderConfig config = JSObjects.create();
        config.setCodec(codec);
        config.setWidth(width);
        config.setHeight(height);
        return config;
    }

    @JSProperty
    String getCodec();

    @JSProperty
    void setCodec(String codec);

    @JSProperty
    int getWidth();

    @JSProperty
    void setWidth(int width);

    @JSProperty
    int getHeight();

    @JSProperty
    void setHeight(int height);

    /** in bits per second */
    @JSProperty
    void setBitrate(double bitrate);

    @JSProperty
    void setFramerate(double framerate);

    /** quality or realtime */
    @JSProperty
    void setLatencyMode(String latencyMode);

    /** no-preference, prefer-hardware or prefer-software */
    @JSProperty
    void setHardwareAcceleration(String hardwareAcceleration);
}
