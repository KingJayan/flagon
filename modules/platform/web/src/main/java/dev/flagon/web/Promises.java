package dev.flagon.web;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.core.JSPromise;

/** waits for promises the way a blocking java call waits for its answer */
public final class Promises {

    private Promises() {
    }

    /**
     * parks the java thread until the promise settles. the page keeps running.
     *
     * @return the value the promise resolved with
     * @throws WebException when it rejects
     */
    @Async
    public static native <T> T await(JSPromise<T> promise);

    private static <T> void await(JSPromise<T> promise, AsyncCallback<T> callback) {
        promise.then(value -> {
            callback.complete(value);
            return null;
        }, error -> {
            callback.error(WebException.of(error));
            return null;
        });
    }
}
