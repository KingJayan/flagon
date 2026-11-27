package dev.flagon.web.transport;

import dev.flagon.web.streams.WritableStream;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.streams.ReadableStream;

/** https://developer.mozilla.org/docs/Web/API/WebTransport */
@JSClass
public class WebTransport implements JSObject {

    public WebTransport(String url) {
    }

    public WebTransport(String url, WebTransportOptions options) {
    }

    @JSBody(script = "return typeof WebTransport !== 'undefined';")
    public static native boolean isSupported();

    @JSProperty
    public native JSPromise<JSUndefined> getReady();

    @JSProperty
    public native JSPromise<WebTransportCloseInfo> getClosed();

    @JSProperty
    public native WebTransportDatagramDuplexStream getDatagrams();

    /** a stream of the server's WebTransportBidirectionalStream */
    @JSProperty
    public native ReadableStream getIncomingBidirectionalStreams();

    /** a stream of the server's ReadableStream */
    @JSProperty
    public native ReadableStream getIncomingUnidirectionalStreams();

    public native JSPromise<WebTransportBidirectionalStream> createBidirectionalStream();

    public native JSPromise<WritableStream> createUnidirectionalStream();

    public native void close();

    public native void close(WebTransportCloseInfo info);
}
