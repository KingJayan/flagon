package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/AudioData/copyTo#options */
public interface AudioDataCopyToOptions extends JSObject {
    static AudioDataCopyToOptions plane(int planeIndex) {
        AudioDataCopyToOptions options = JSObjects.create();
        options.setPlaneIndex(planeIndex);
        return options;
    }

    @JSProperty
    void setPlaneIndex(int planeIndex);

    /** converts the samples, such as to f32-planar */
    @JSProperty
    void setFormat(String format);
}
