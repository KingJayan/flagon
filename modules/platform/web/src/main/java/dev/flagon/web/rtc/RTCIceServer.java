package dev.flagon.web.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** a stun or turn server of RTCConfiguration */
public interface RTCIceServer extends JSObject {
    /** @param url such as stun:stun.example.com:3478 */
    static RTCIceServer create(String url) {
        RTCIceServer server = JSObjects.create();
        server.setUrls(url);
        return server;
    }

    static RTCIceServer create(String url, String username, String credential) {
        RTCIceServer server = create(url);
        server.setUsername(username);
        server.setCredential(credential);
        return server;
    }

    @JSProperty
    void setUrls(String urls);

    @JSProperty
    void setUsername(String username);

    @JSProperty
    void setCredential(String credential);
}
