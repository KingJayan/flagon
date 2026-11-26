package java.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

final class Requests {

    private static final Set<String> RESTRICTED =
            Set.of("connection", "content-length", "expect", "host", "upgrade");

    private Requests() {
    }

    static final class Building implements HttpRequest.Builder {

        private URI uri;
        private boolean expectContinue;
        private HttpClient.Version version;
        private Duration timeout;
        private String method = "GET";
        private HttpRequest.BodyPublisher body;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();

        @Override
        public HttpRequest.Builder uri(URI uri) {
            Objects.requireNonNull(uri, "uri must be non-null");
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new IllegalArgumentException("URI with undefined scheme");
            }
            String lower = scheme.toLowerCase(Locale.ROOT);
            if (!lower.equals("http") && !lower.equals("https")) {
                throw new IllegalArgumentException("invalid URI scheme " + scheme);
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("unsupported URI " + uri);
            }
            this.uri = uri;
            return this;
        }

        @Override
        public HttpRequest.Builder expectContinue(boolean enable) {
            this.expectContinue = enable;
            return this;
        }

        @Override
        public HttpRequest.Builder version(HttpClient.Version version) {
            this.version = Objects.requireNonNull(version);
            return this;
        }

        @Override
        public HttpRequest.Builder header(String name, String value) {
            check(name, value);
            find(name).add(value);
            return this;
        }

        @Override
        public HttpRequest.Builder headers(String... pairs) {
            if (pairs.length == 0 || pairs.length % 2 != 0) {
                throw new IllegalArgumentException("wrong number, " + pairs.length
                        + ", of parameters");
            }
            for (int at = 0; at < pairs.length; at += 2) {
                header(pairs[at], pairs[at + 1]);
            }
            return this;
        }

        @Override
        public HttpRequest.Builder timeout(Duration duration) {
            Objects.requireNonNull(duration);
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Invalid duration: " + duration);
            }
            this.timeout = duration;
            return this;
        }

        @Override
        public HttpRequest.Builder setHeader(String name, String value) {
            check(name, value);
            List<String> values = find(name);
            values.clear();
            values.add(value);
            return this;
        }

        @Override
        public HttpRequest.Builder GET() {
            return choose("GET", null);
        }

        @Override
        public HttpRequest.Builder POST(HttpRequest.BodyPublisher publisher) {
            return choose("POST", Objects.requireNonNull(publisher));
        }

        @Override
        public HttpRequest.Builder PUT(HttpRequest.BodyPublisher publisher) {
            return choose("PUT", Objects.requireNonNull(publisher));
        }

        @Override
        public HttpRequest.Builder DELETE() {
            return choose("DELETE", null);
        }

        @Override
        public HttpRequest.Builder method(String name, HttpRequest.BodyPublisher publisher) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(publisher);
            if (name.equals("CONNECT")) {
                throw new IllegalArgumentException("method CONNECT is not supported");
            }
            if (!isToken(name)) {
                throw new IllegalArgumentException("illegal method \"" + name + "\"");
            }
            return choose(name, publisher);
        }

        @Override
        public HttpRequest build() {
            if (uri == null) {
                throw new IllegalStateException("uri is null");
            }
            Map<String, List<String>> copied = new LinkedHashMap<>();
            headers.forEach((name, values) -> copied.put(name, List.copyOf(values)));
            return new Built(uri, method, body, timeout, expectContinue, version,
                    HttpHeaders.of(copied, (name, value) -> true));
        }

        @Override
        public HttpRequest.Builder copy() {
            Building copy = new Building();
            copy.uri = uri;
            copy.expectContinue = expectContinue;
            copy.version = version;
            copy.timeout = timeout;
            copy.method = method;
            copy.body = body;
            headers.forEach((name, values) -> copy.headers.put(name, new ArrayList<>(values)));
            return copy;
        }

        private HttpRequest.Builder choose(String name, HttpRequest.BodyPublisher publisher) {
            this.method = name;
            this.body = publisher;
            return this;
        }

        private List<String> find(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            List<String> values = new ArrayList<>();
            headers.put(name, values);
            return values;
        }

        private static void check(String name, String value) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            if (!isToken(name)) {
                throw new IllegalArgumentException("invalid header name: \"" + name + "\"");
            }
            if (RESTRICTED.contains(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("restricted header name: \"" + name + "\"");
            }
            for (int at = 0; at < value.length(); at++) {
                char c = value.charAt(at);
                if (c == '\r' || c == '\n' || c == 0 || c > 0xFF) {
                    throw new IllegalArgumentException("invalid header value for header '"
                            + name + "'");
                }
            }
        }

        private static boolean isToken(String text) {
            if (text.isEmpty()) {
                return false;
            }
            for (int at = 0; at < text.length(); at++) {
                char c = text.charAt(at);
                if (c <= ' ' || c >= 0x7F || "()<>@,;:\\\"/[]?={}".indexOf(c) >= 0) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class Built extends HttpRequest {

        private final URI uri;
        private final String method;
        private final BodyPublisher body;
        private final Duration timeout;
        private final boolean expectContinue;
        private final HttpClient.Version version;
        private final HttpHeaders headers;

        Built(URI uri, String method, BodyPublisher body, Duration timeout,
              boolean expectContinue, HttpClient.Version version, HttpHeaders headers) {
            this.uri = uri;
            this.method = method;
            this.body = body;
            this.timeout = timeout;
            this.expectContinue = expectContinue;
            this.version = version;
            this.headers = headers;
        }

        @Override
        public Optional<BodyPublisher> bodyPublisher() {
            return Optional.ofNullable(body);
        }

        @Override
        public String method() {
            return method;
        }

        @Override
        public Optional<Duration> timeout() {
            return Optional.ofNullable(timeout);
        }

        @Override
        public boolean expectContinue() {
            return expectContinue;
        }

        @Override
        public URI uri() {
            return uri;
        }

        @Override
        public Optional<HttpClient.Version> version() {
            return Optional.ofNullable(version);
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public String toString() {
            return uri + " " + method;
        }
    }

    /** a body known as bytes, made when it is sent */
    static final class Bytes implements HttpRequest.BodyPublisher {

        private final long length;
        private final Supplier<byte[]> source;

        Bytes(long length, Supplier<byte[]> source) {
            this.length = length;
            this.source = source;
        }

        byte[] bytes() {
            return source.get();
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            Objects.requireNonNull(subscriber);
            subscriber.onSubscribe(new Flow.Subscription() {
                private boolean done;

                @Override
                public void request(long count) {
                    if (done) {
                        return;
                    }
                    done = true;
                    if (count <= 0) {
                        subscriber.onError(new IllegalArgumentException(
                                "non-positive subscription request: " + count));
                        return;
                    }
                    byte[] made;
                    try {
                        made = bytes();
                    } catch (RuntimeException failed) {
                        subscriber.onError(failed);
                        return;
                    }
                    if (made.length > 0) {
                        subscriber.onNext(ByteBuffer.wrap(made));
                    }
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                    done = true;
                }
            });
        }
    }

    static final class Relayed implements HttpRequest.BodyPublisher {

        private final Flow.Publisher<? extends ByteBuffer> publisher;
        private final long length;

        Relayed(Flow.Publisher<? extends ByteBuffer> publisher, long length) {
            this.publisher = publisher;
            this.length = length;
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            publisher.subscribe(subscriber);
        }
    }

    static byte[] collect(HttpRequest.BodyPublisher publisher) throws IOException {
        try {
            if (publisher instanceof Bytes known) {
                return known.bytes();
            }
            CompletableFuture<byte[]> whole = new CompletableFuture<>();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    byte[] part = new byte[item.remaining()];
                    item.get(part);
                    out.write(part, 0, part.length);
                }

                @Override
                public void onError(Throwable failure) {
                    whole.completeExceptionally(failure);
                }

                @Override
                public void onComplete() {
                    whole.complete(out.toByteArray());
                }
            });
            return whole.join();
        } catch (UncheckedIOException failed) {
            throw failed.getCause();
        } catch (CompletionException failed) {
            Throwable cause = failed.getCause();
            throw cause instanceof IOException io ? io
                    : new IOException("the request body failed", cause);
        }
    }

    static byte[] join(List<byte[]> parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }
}
