package dev.flagon.platform.browser.net;

import dev.flagon.platform.net.NetworkException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;

/**
 a socket at the far side of a websocket
 */
final class Tunnel {

    static final String PATH = "/__flagon/tcp";

    private static final int SEND_LIMIT = 1 << 20;

    /** how long a full browser buffer may stay full before the connection is called lost */
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

    @JSBody(script = "return typeof WebSocket !== 'undefined' && typeof location !== 'undefined';")
    static native boolean isAvailable();

    @JSBody(params = {"host", "port", "path"}, script =
            "var scheme = location.protocol === 'https:' ? 'wss://' : 'ws://';"
            + "var token = window.__flagonProxyToken;"
            + "return scheme + location.host + path"
            + " + '?host=' + encodeURIComponent(host) + '&port=' + port"
            + " + (typeof token === 'string' && token"
            + " ? '&token=' + encodeURIComponent(token) : '');")
    static native String endpoint(String host, int port, String path);

    @Async
    static native Handle open(String url);

    private static void open(String url, AsyncCallback<Handle> callback) {
        netOpen(url, settle(callback));
    }

    /** @return the next bytes to arrive, or null once the far end has hung up */
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

    @JSBody(script = "return typeof window.__flagonNet !== 'undefined';")
    static native boolean isInstalled();

    @JSBody(params = {"url", "done"}, script = "window.__flagonNet.open(url, done);")
    private static native void netOpen(String url, Settled done);

    @JSBody(params = {"handle", "done"}, script = "window.__flagonNet.receive(handle, done);")
    private static native void netReceive(Handle handle, Settled done);

    @JSBody(params = {"handle"}, script = "return window.__flagonNet.available(handle);")
    static native int available(Handle handle);

    @JSBody(params = {"handle"}, script = "return window.__flagonNet.isOpen(handle);")
    static native boolean isOpen(Handle handle);

    @JSBody(params = {"handle", "data"}, script = "window.__flagonNet.send(handle, data);")
    private static native void netSend(Handle handle, Int8Array data);

    @JSBody(params = {"handle"}, script = "return window.__flagonNet.buffered(handle);")
    private static native int buffered(Handle handle);

    @JSBody(params = {"done"}, script = "setTimeout(done, 4);")
    private static native void soon(Tick done);

    @JSBody(script = "return Date.now();")
    private static native double clock();

    private static long now() {
        return (long) clock();
    }

    @JSBody(params = {"handle"}, script = "window.__flagonNet.close(handle);")
    static native void close(Handle handle);
}
