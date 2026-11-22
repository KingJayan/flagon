package java.net.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

final class Responses {

    private Responses() {
    }

    static Charset charsetOf(HttpHeaders headers) {
        String type = headers.firstValue("content-type").orElse("");
        for (String part : type.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                String name = trimmed.substring(8).replace("\"", "").trim();
                try {
                    return Charset.forName(name);
                } catch (RuntimeException unknown) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    static void feed(Flow.Subscriber<? super List<ByteBuffer>> subscriber, byte[] body) {
        List<List<ByteBuffer>> items = body.length == 0 ? List.of()
                : List.of(List.of(ByteBuffer.wrap(body).asReadOnlyBuffer()));
        subscriber.onSubscribe(new Paced<>(subscriber, items));
    }

    private static final class Paced<T> implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final List<T> items;
        private int next;
        private long demand;
        private boolean draining;
        private boolean done;

        Paced(Flow.Subscriber<? super T> subscriber, List<T> items) {
            this.subscriber = subscriber;
            this.items = items;
        }

        @Override
        public void request(long count) {
            if (done) {
                return;
            }
            if (count <= 0) {
                done = true;
                subscriber.onError(new IllegalArgumentException(
                        "non-positive subscription request: " + count));
                return;
            }
            demand = demand + count < 0 ? Long.MAX_VALUE : demand + count;
            if (draining) {
                return;
            }
            draining = true;
            while (!done && demand > 0 && next < items.size()) {
                demand--;
                subscriber.onNext(items.get(next++));
            }
            draining = false;
            if (!done && next == items.size()) {
                done = true;
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }

    static void feedLines(Flow.Subscriber<? super String> subscriber, String text,
                          String separator) {
        List<String> lines = new ArrayList<>();
        if (separator == null) {
            new BufferedReader(new StringReader(text)).lines().forEach(lines::add);
        } else {
            int from = 0;
            int at;
            while ((at = text.indexOf(separator, from)) >= 0) {
                lines.add(text.substring(from, at));
                from = at + separator.length();
            }
            if (from < text.length()) {
                lines.add(text.substring(from));
            }
        }
        subscriber.onSubscribe(new Paced<>(subscriber, lines));
    }

    static Flow.Publisher<List<ByteBuffer>> replay(byte[] body) {
        return subscriber -> feed(subscriber, body);
    }

    static final class Collecting<T> implements HttpResponse.BodySubscriber<T> {

        private final Function<byte[], T> finisher;
        private final CompletableFuture<T> body = new CompletableFuture<>();
        private final ByteArrayOutputStream gathered = new ByteArrayOutputStream();

        Collecting(Function<byte[], T> finisher) {
            this.finisher = finisher;
        }

        @Override
        public CompletionStage<T> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            for (ByteBuffer buffer : item) {
                byte[] part = new byte[buffer.remaining()];
                buffer.get(part);
                gathered.write(part, 0, part.length);
            }
        }

        @Override
        public void onError(Throwable failure) {
            body.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            try {
                body.complete(finisher.apply(gathered.toByteArray()));
            } catch (UncheckedIOException failed) {
                body.completeExceptionally(failed.getCause());
            } catch (RuntimeException failed) {
                body.completeExceptionally(failed);
            }
        }
    }

    static final class Relaying<T> implements HttpResponse.BodySubscriber<T> {

        private final Flow.Subscriber<? super List<ByteBuffer>> downstream;
        private final Supplier<T> finisher;
        private final CompletableFuture<T> body = new CompletableFuture<>();

        Relaying(Flow.Subscriber<? super List<ByteBuffer>> downstream, Supplier<T> finisher) {
            this.downstream = downstream;
            this.finisher = finisher;
        }

        @Override
        public CompletionStage<T> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            downstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            downstream.onNext(item);
        }

        @Override
        public void onError(Throwable failure) {
            downstream.onError(failure);
            body.completeExceptionally(failure);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            try {
                body.complete(finisher.get());
            } catch (RuntimeException failed) {
                body.completeExceptionally(failed);
            }
        }
    }

    static final class Mapped<T, U> implements HttpResponse.BodySubscriber<U> {

        private final HttpResponse.BodySubscriber<T> upstream;
        private final CompletionStage<U> body;

        Mapped(HttpResponse.BodySubscriber<T> upstream, Function<? super T, ? extends U> mapper) {
            this.upstream = upstream;
            this.body = upstream.getBody().thenApply(mapper);
        }

        @Override
        public CompletionStage<U> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            upstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            upstream.onNext(item);
        }

        @Override
        public void onError(Throwable failure) {
            upstream.onError(failure);
        }

        @Override
        public void onComplete() {
            upstream.onComplete();
        }
    }

    record Info(int statusCode, HttpHeaders headers, HttpClient.Version version)
            implements HttpResponse.ResponseInfo {
    }

    record Answered<T>(int statusCode, HttpRequest request, HttpHeaders headers, T body,
                       URI uri, HttpClient.Version version) implements HttpResponse<T> {

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "(" + request.method() + " " + request.uri() + ") " + statusCode;
        }
    }
}
