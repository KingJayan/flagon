package dev.flagon.web.transport;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** https://developer.mozilla.org/docs/Web/API/WebTransport/close */
public interface WebTransportCloseInfo extends JSObject {
    static WebTransportCloseInfo create(int closeCode, String reason) {
        WebTransportCloseInfo info = JSObjects.create();
        info.setCloseCode(closeCode);
        info.setReason(reason);
        return info;
    }

    @JSProperty
    int getCloseCode();

    @JSProperty
    void setCloseCode(int closeCode);

    @JSProperty
    String getReason();

    @JSProperty
    void setReason(String reason);
}
