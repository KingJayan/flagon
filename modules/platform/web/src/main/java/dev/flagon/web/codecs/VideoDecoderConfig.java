package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/VideoDecoder/configure#config */
public interface VideoDecoderConfig extends JSObject {
    static VideoDecoderConfig create(String codec) {
        VideoDecoderConfig config = JSObjects.create();
        config.setCodec(codec);
        return config;
    }

    @JSProperty
    String getCodec();

    @JSProperty
    void setCodec(String codec);

    @JSProperty
    void setCodedWidth(int codedWidth);

    @JSProperty
    void setCodedHeight(int codedHeight);

    /** the codec's own setup bytes, such as an avcC box */
    @JSProperty
    void setDescription(ArrayBufferView description);

    @JSProperty
    void setHardwareAcceleration(String hardwareAcceleration);
}
