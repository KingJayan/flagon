package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** how a VideoFrame reads raw pixels */
public interface VideoFrameBufferInit extends JSObject {
    /**
     * @param format such as RGBA or I420
     * @param timestamp in microseconds
     */
    static VideoFrameBufferInit create(String format, int codedWidth, int codedHeight,
            double timestamp) {
        VideoFrameBufferInit init = JSObjects.create();
        init.setFormat(format);
        init.setCodedWidth(codedWidth);
        init.setCodedHeight(codedHeight);
        init.setTimestamp(timestamp);
        return init;
    }

    @JSProperty
    void setFormat(String format);

    @JSProperty
    void setCodedWidth(int codedWidth);

    @JSProperty
    void setCodedHeight(int codedHeight);

    @JSProperty
    void setTimestamp(double timestamp);

    @JSProperty
    void setDuration(double duration);
}
