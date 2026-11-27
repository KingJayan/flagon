package dev.flagon.web.indexeddb;

import dev.flagon.web.WebException;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.indexeddb.IDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

/** parks the java thread until an indexeddb request or transaction ends */
final class Requests {

    private Requests() {
    }

    @Async
    static native JSObject result(IDBRequest request);

    private static void result(IDBRequest request, AsyncCallback<JSObject> callback) {
        request.setOnSuccess(() -> callback.complete(resultOf(request)));
        request.setOnError(() -> callback.error(WebException.of(errorOf(request))));
    }

    /** waits for the transaction to commit, which a write's own success does not promise */
    @Async
    static native Void committed(IDBTransaction transaction);

    private static void committed(IDBTransaction transaction, AsyncCallback<Void> callback) {
        transaction.setOnComplete(() -> callback.complete(null));
        transaction.setOnAbort(() -> callback.error(aborted(transaction)));
    }

    private static RuntimeException aborted(IDBTransaction transaction) {
        JSObject error = errorOf(transaction);
        return error == null ? new WebException("AbortError", "the transaction was aborted")
                : WebException.of(error);
    }

    @JSBody(params = "request", script = "return request.result;")
    private static native JSObject resultOf(IDBRequest request);

    @JSBody(params = "source", script = "return source.error;")
    private static native JSObject errorOf(JSObject source);
}
