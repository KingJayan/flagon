package dev.flagon.web.streams;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

/** https://developer.mozilla.org/docs/Web/API/WritableStream */
public interface WritableStream extends JSObject {
    @JSProperty
    boolean isLocked();

    WritableStreamDefaultWriter getWriter();

    JSPromise<JSUndefined> close();

    JSPromise<JSUndefined> abort();

    JSPromise<JSUndefined> abort(String reason);
}
