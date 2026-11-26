package dev.flagon.platform.browser.net;

import dev.flagon.platform.browser.js.JsArrays;
import dev.flagon.platform.net.Fetch;
import dev.flagon.platform.net.NetworkException;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.Int8Array;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** {@link Fetch} over the page's fetch, which web/src/fetch.ts installs; CORS applies */
public final class BrowserFetch implements Fetch {

    private static final String TIMED_OUT = "timed out";

    interface Answer extends JSObject {

        @JSProperty
        int getStatus();

        @JSProperty
        String getUrl();

        @JSProperty
        String getHeaders();

        @JSProperty
        Int8Array getBody();
    }

    @JSFunctor
    interface Settled extends JSObject {
        void call(String error, Answer answer);
    }

    @Override
    public String name() {
        return "fetch";
    }

    @Override
    public boolean isAvailable() {
        return hasFetch();
    }

    @Override
    public Response send(Request request) {
        if (!hasFetch()) {
            throw new NetworkException("no self.__flagonFetch on this page; the flagon bundle"
                    + " loader installs it");
        }
        StringBuilder lines = new StringBuilder();
        for (Map.Entry<String, String> header : request.headers()) {
            lines.append(header.getKey()).append('\n').append(header.getValue()).append('\n');
        }
        byte[] body = request.body();
        Answer answer = fetch(request.method(), request.url(), lines.toString(),
                body == null ? null : JsArrays.bytes(body, body.length),
                (int) Math.min(request.timeoutMillis(), Integer.MAX_VALUE),
                request.followRedirects());
        return new Response(answer.getStatus(), answer.getUrl(), headers(answer.getHeaders()),
                JsArrays.bytesOf(answer.getBody()));
    }

    private static Map<String, List<String>> headers(String lines) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        String[] parts = lines.split("\n", -1);
        for (int at = 0; at + 1 < parts.length; at += 2) {
            headers.computeIfAbsent(parts[at].toLowerCase(Locale.ROOT), any -> new ArrayList<>())
                    .add(parts[at + 1]);
        }
        return headers;
    }

    @Async
    private static native Answer fetch(String method, String url, String headers, Int8Array body,
                                       int timeout, boolean follow);

    private static void fetch(String method, String url, String headers, Int8Array body,
                              int timeout, boolean follow, AsyncCallback<Answer> callback) {
        start(method, url, headers, body, timeout, follow, (error, answer) -> {
            if (error == null) {
                callback.complete(answer);
            } else if (TIMED_OUT.equals(error)) {
                callback.error(new TimedOut("no answer from " + url + " within " + timeout
                        + " ms"));
            } else {
                callback.error(new NetworkException(error));
            }
        });
    }

    @JSBody(script = "return typeof self.__flagonFetch !== 'undefined';")
    private static native boolean hasFetch();

    @JSBody(params = {"method", "url", "headers", "body", "timeout", "follow", "done"},
            script = "self.__flagonFetch.send(method, url, headers, body, timeout, follow,"
                    + " done);")
    private static native void start(String method, String url, String headers, Int8Array body,
                                     int timeout, boolean follow, Settled done);
}
