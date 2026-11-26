package dev.flagon.platform.net;

import java.util.List;
import java.util.Map;

public interface Fetch {

    String name();

    boolean isAvailable();

    /**
     * sends one request and waits for the whole answer, pausing only the calling thread.
     * @throws TimedOut when no answer arrives within the request's timeout
     * @throws NetworkException when the request could not be sent or answered
     */
    Response send(Request request);

    /**
     * @param headers names and values in order; a name can repeat
     * @param body null to send none
     * @param timeoutMillis how long to wait for the answer to start; 0 waits for ever
     * @param followRedirects false fails on a redirect, which a browser does not show
     */
    record Request(String method, String url, List<Map.Entry<String, String>> headers,
                   byte[] body, long timeoutMillis, boolean followRedirects) {
    }

    /**
     * @param url where the answer came from, after any redirects
     * @param headers lowercase names, in the order they arrived
     */
    record Response(int status, String url, Map<String, List<String>> headers, byte[] body) {
    }

    /** a request that got no answer in time */
    final class TimedOut extends NetworkException {

        public TimedOut(String message) {
            super(message);
        }
    }

    /** @return a fetch that refuses every request with this reason */
    static Fetch unavailable(String reason) {
        return new Fetch() {
            @Override
            public String name() {
                return "no fetch";
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Response send(Request request) {
                throw new NetworkException(reason);
            }
        };
    }
}
