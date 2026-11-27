package dev.flagon.web.transport;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/WebTransport/WebTransport#options */
public interface WebTransportOptions extends JSObject {
    static WebTransportOptions create() {
        return JSObjects.create();
    }

    @JSProperty
    void setAllowPooling(boolean allowPooling);

    /** default, throughput or low-latency */
    @JSProperty
    void setCongestionControl(String congestionControl);

    @JSProperty
    void setRequireUnreliable(boolean requireUnreliable);

    /** pins self-signed certificates of at most 14 days */
    @JSProperty
    void setServerCertificateHashes(WebTransportHash[] hashes);
}
