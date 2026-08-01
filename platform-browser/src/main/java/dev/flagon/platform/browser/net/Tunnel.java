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
            + "return scheme + location.host + path"
            + " + '?host=' + encodeURIComponent(host) + '&port=' + port;")
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
        while (buffered(handle) > SEND_LIMIT && isOpen(handle)) {
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

    /**
     * written without {@code async}, {@code await} or arrow functions on purpose
     */
    @JSBody(script = """
            if (window.__flagonNet) { return; }
            var HOLD_AT = 256 * 1024;
            var RESUME_AT = 64 * 1024;
            var flow = function(state) {
                if (state.socket.readyState !== 1) { return; }
                if (!state.held && state.queued >= HOLD_AT) {
                    state.held = true;
                    state.socket.send('pause');
                } else if (state.held && state.queued <= RESUME_AT) {
                    state.held = false;
                    state.socket.send('resume');
                }
            };
            var deliver = function(state) {
                if (state.pending === null) { return; }
                var done = state.pending;
                if (state.chunks.length > 0) {
                    state.pending = null;
                    var chunk = state.chunks.shift();
                    state.queued -= chunk.length;
                    flow(state);
                    done(null, chunk);
                } else if (state.failure !== null) {
                    state.pending = null;
                    done(state.failure, null);
                } else if (state.closed) {
                    state.pending = null;
                    done(null, null);
                }
            };
            var report = function(state, message) {
                if (state.opened !== null) {
                    var open = state.opened;
                    state.opened = null;
                    open(message, null);
                    return;
                }
                state.failure = message;
                deliver(state);
            };
            window.__flagonNet = {
                open: function(url, done) {
                    var missing = 'no flagon tcp proxy answered at ' + url + '; a page reaches'
                            + ' the network only through one served with it, which a hosted'
                            + ' build has to run for itself';
                    var socket = new WebSocket(url);
                    socket.binaryType = 'arraybuffer';
                    var state = { socket: socket, chunks: [], queued: 0, pending: null,
                                  closed: false, failure: null, opened: done, held: false };
                    socket.onmessage = function(event) {
                        if (typeof event.data === 'string') {
                            if (state.opened !== null && event.data === 'open') {
                                var open = state.opened;
                                state.opened = null;
                                open(null, state);
                            } else {
                                report(state, event.data);
                            }
                            return;
                        }
                        var chunk = new Int8Array(event.data);
                        if (chunk.length > 0) {
                            state.chunks.push(chunk);
                            state.queued += chunk.length;
                            flow(state);
                        }
                        deliver(state);
                    };
                    socket.onclose = function() {
                        state.closed = true;
                        if (state.opened !== null) {
                            var open = state.opened;
                            state.opened = null;
                            open(missing, null);
                            return;
                        }
                        deliver(state);
                    };
                    socket.onerror = function() {
                        report(state, state.opened !== null ? missing
                                : 'the connection to the proxy failed');
                    };
                },
                receive: function(state, done) {
                    state.pending = done;
                    deliver(state);
                },
                available: function(state) { return state.queued; },
                isOpen: function(state) { return !state.closed && state.failure === null; },
                buffered: function(state) { return state.socket.bufferedAmount; },
                send: function(state, data) { state.socket.send(data); },
                close: function(state) {
                    state.closed = true;
                    if (state.socket.readyState < 2) { state.socket.close(); }
                    deliver(state);
                }
            };
            """)
    static native void install();

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

    @JSBody(params = {"handle"}, script = "window.__flagonNet.close(handle);")
    static native void close(Handle handle);
}
