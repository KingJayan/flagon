package dev.flagon.platform.browser.net;

import dev.flagon.platform.net.NetworkException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Location;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;

/**
 a socket at the far side of a ws
 */
final class Tunnel {

    static final String PATH = "/__flagon/tcp";

    private static final int SEND_LIMIT = 1 << 20;

    private static final long SEND_DEADLINE_MS = 30_000L;

    interface Handle extends JSObject {
    }

    @JSFunctor
    interface Settled extends JSObject {
        void call(String error, JSObject result);
    }

    @JSFunctor
    interface Tick extends JSObject {
        void call();
    }

    private Tunnel() {
    }

    static boolean isAvailable() {
        return hasWebSocket() && hasLocation();
    }

    @JSBody(script = "return typeof WebSocket !== 'undefined';")
    private static native boolean hasWebSocket();

    @JSBody(script = "return self.location;")
    private static native Location location();

    @JSBody(script = "return typeof location !== 'undefined';")
    private static native boolean hasLocation();

    static String endpoint(String host, int port, String path, boolean secure) {
        Location page = location();
        String scheme = "https:".equals(page.getProtocol()) ? "wss://" : "ws://";
        StringBuilder url = new StringBuilder(scheme)
                .append(page.getHost())
                .append(path)
                .append("?host=").append(encode(host))
                .append("&port=").append(port);
        if (secure) {
            url.append("&tls=1");
        }
        if (hasProxyToken()) {
            String token = proxyToken();
            if (!token.isEmpty()) {
                url.append("&token=").append(encode(token));
            }
        }
        return url.toString();
    }

    @JSBody(params = "text", script = "return encodeURIComponent(text);")
    private static native String encode(String text);

    @JSBody(script = "return typeof self.__flagonProxyToken === 'string';")
    private static native boolean hasProxyToken();

    @JSBody(script = "return self.__flagonProxyToken;")
    private static native String proxyToken();

    @Async
    static native Handle open(String url);

    private static void open(String url, AsyncCallback<Handle> callback) {
        netOpen(url, settle(callback));
    }

    @Async
    static native Int8Array receive(Handle handle);

    private static void receive(Handle handle, AsyncCallback<Int8Array> callback) {
        netReceive(handle, settle(callback));
    }

    static void send(Handle handle, Int8Array data) {
        long waitedFrom = now();
        while (buffered(handle) > SEND_LIMIT && isOpen(handle)) {
            if (now() - waitedFrom > SEND_DEADLINE_MS) {
                close(handle);
                throw new NetworkException("the proxy stopped taking writes for "
                        + SEND_DEADLINE_MS / 1000 + "s with " + buffered(handle)
                        + " bytes still buffered");
            }
            settleBuffer();
        }
        netSend(handle, data);
    }

    @Async
    private static native void settleBuffer();

    private static void settleBuffer(AsyncCallback<Void> callback) {
        soon(() -> callback.complete(null));
    }

    private static <T extends JSObject> Settled settle(AsyncCallback<T> callback) {
        return (error, result) -> {
            if (error != null) {
                callback.error(new NetworkException(error));
            } else {
                @SuppressWarnings("unchecked")
                T typed = (T) result;
                callback.complete(typed);
            }
        };
    }

    @JSBody(script = "return typeof self.__flagonNet !== 'undefined';")
    static native boolean isInstalled();

    @JSBody(params = {"url", "done"}, script = "self.__flagonNet.open(url, done);")
    private static native void netOpen(String url, Settled done);

    @JSBody(params = {"handle", "done"}, script = "self.__flagonNet.receive(handle, done);")
    private static native void netReceive(Handle handle, Settled done);

    @JSBody(params = {"handle"}, script = "return self.__flagonNet.available(handle);")
    static native int available(Handle handle);

    @JSBody(params = {"handle"}, script = "return self.__flagonNet.isOpen(handle);")
    static native boolean isOpen(Handle handle);

    @JSBody(params = {"handle", "data"}, script = "self.__flagonNet.send(handle, data);")
    private static native void netSend(Handle handle, Int8Array data);

    @JSBody(params = {"handle"}, script = "return self.__flagonNet.buffered(handle);")
    private static native int buffered(Handle handle);

    @JSBody(params = {"done"}, script = "setTimeout(done, 4);")
    private static native void soon(Tick done);

    @JSBody(script = "return Date.now();")
    private static native double clock();

    private static long now() {
        return (long) clock();
    }

    @JSBody(params = {"handle"}, script = "self.__flagonNet.close(handle);")
    static native void close(Handle handle);
}
