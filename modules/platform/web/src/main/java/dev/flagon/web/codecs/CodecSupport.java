package dev.flagon.web.codecs;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/** what isConfigSupported answers */
public interface CodecSupport<T extends JSObject> extends JSObject {
    @JSProperty
    boolean isSupported();

    /** @return the config, with only the members the browser recognized */
    @JSProperty
    T getConfig();
}
