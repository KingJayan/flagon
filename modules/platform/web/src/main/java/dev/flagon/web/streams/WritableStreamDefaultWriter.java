package dev.flagon.web.streams;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;

/** https://developer.mozilla.org/docs/Web/API/WritableStreamDefaultWriter */
public interface WritableStreamDefaultWriter extends JSObject {
    @JSProperty
    JSPromise<JSUndefined> getReady();

    @JSProperty
    JSPromise<JSUndefined> getClosed();

    @JSProperty
    double getDesiredSize();

    JSPromise<JSUndefined> write(JSObject chunk);

    JSPromise<JSUndefined> close();

    JSPromise<JSUndefined> abort();

    void releaseLock();
}
