package dev.flagon.web.transport;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** a certificate pin of WebTransportOptions */
public interface WebTransportHash extends JSObject {
    /** @param value the certificate's sha-256 digest */
    static WebTransportHash sha256(ArrayBufferView value) {
        WebTransportHash hash = JSObjects.create();
        hash.setAlgorithm("sha-256");
        hash.setValue(value);
        return hash;
    }

    @JSProperty
    void setAlgorithm(String algorithm);

    @JSProperty
    void setValue(ArrayBufferView value);
}
