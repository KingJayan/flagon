package dev.flagon.web.transport;

import dev.flagon.web.streams.WritableStream;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.streams.ReadableStream;

/** https://developer.mozilla.org/docs/Web/API/WebTransportBidirectionalStream */
public interface WebTransportBidirectionalStream extends JSObject {
    @JSProperty
    ReadableStream getReadable();

    @JSProperty
    WritableStream getWritable();
}
