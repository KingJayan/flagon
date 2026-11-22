package java.net.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface HttpResponse<T> {

    int statusCode();

    HttpRequest request();

    Optional<HttpResponse<T>> previousResponse();

    HttpHeaders headers();

    T body();

    URI uri();

    HttpClient.Version version();

    interface ResponseInfo {

        int statusCode();

        HttpHeaders headers();

        HttpClient.Version version();
    }

    @FunctionalInterface
    interface BodyHandler<T> {

        BodySubscriber<T> apply(ResponseInfo responseInfo);
    }

    interface BodySubscriber<T> extends Flow.Subscriber<List<ByteBuffer>> {

        CompletionStage<T> getBody();
    }

    /** a browser never pushes, so these are never asked for */
    interface PushPromiseHandler<T> {

        void applyPushPromise(HttpRequest initiatingRequest, HttpRequest pushPromiseRequest,
                              Function<BodyHandler<T>, CompletableFuture<HttpResponse<T>>>
                                      acceptor);
    }

    class BodyHandlers {

        private BodyHandlers() {
        }

        public static BodyHandler<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            Objects.requireNonNull(subscriber);
            return info -> BodySubscribers.fromSubscriber(subscriber);
        }

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodyHandler<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            return info -> BodySubscribers.fromSubscriber(subscriber, finisher);
        }

        public static BodyHandler<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            Objects.requireNonNull(subscriber);
            return info -> BodySubscribers.fromLineSubscriber(subscriber, any -> null,
                    Responses.charsetOf(info.headers()), null);
        }

        public static <S extends Flow.Subscriber<? super String>, T> BodyHandler<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                                   String lineSeparator) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            if (lineSeparator != null && lineSeparator.isEmpty()) {
                throw new IllegalArgumentException("empty line separator");
            }
            return info -> BodySubscribers.fromLineSubscriber(subscriber, finisher,
                    Responses.charsetOf(info.headers()), lineSeparator);
        }

        public static BodyHandler<Void> discarding() {
            return info -> BodySubscribers.discarding();
        }

        public static <U> BodyHandler<U> replacing(U value) {
            return info -> BodySubscribers.replacing(value);
        }

        public static BodyHandler<String> ofString(Charset charset) {
            Objects.requireNonNull(charset);
            return info -> BodySubscribers.ofString(charset);
        }

        public static BodyHandler<Path> ofFile(Path file, OpenOption... openOptions) {
            Objects.requireNonNull(file);
            List<OpenOption> options = List.of(openOptions);
            if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)
                    || options.contains(StandardOpenOption.READ)) {
                throw new IllegalArgumentException("unsupported option in " + options);
            }
            return info -> BodySubscribers.ofFile(file, openOptions);
        }

        public static BodyHandler<Path> ofFile(Path file) {
            return ofFile(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }

        public static BodyHandler<Void> ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
            Objects.requireNonNull(consumer);
            return info -> BodySubscribers.ofByteArrayConsumer(consumer);
        }

        public static BodyHandler<byte[]> ofByteArray() {
            return info -> BodySubscribers.ofByteArray();
        }

        public static BodyHandler<String> ofString() {
            return info -> BodySubscribers.ofString(Responses.charsetOf(info.headers()));
        }

        public static BodyHandler<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return info -> BodySubscribers.ofPublisher();
        }

        public static BodyHandler<InputStream> ofInputStream() {
            return info -> BodySubscribers.ofInputStream();
        }

        public static BodyHandler<Stream<String>> ofLines() {
            return info -> BodySubscribers.ofLines(Responses.charsetOf(info.headers()));
        }

        public static <T> BodyHandler<T> buffering(BodyHandler<T> downstreamHandler,
                                                   int bufferSize) {
            Objects.requireNonNull(downstreamHandler);
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("must be greater than 0");
            }
            return downstreamHandler;
        }
    }

    class BodySubscribers {

        private BodySubscribers() {
        }

        public static BodySubscriber<Void> fromSubscriber(
                Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            return fromSubscriber(subscriber, any -> null);
        }

        public static <S extends Flow.Subscriber<? super List<ByteBuffer>>, T> BodySubscriber<T>
                fromSubscriber(S subscriber, Function<? super S, ? extends T> finisher) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            return new Responses.Relaying<>(subscriber, () -> finisher.apply(subscriber));
        }

        public static BodySubscriber<Void> fromLineSubscriber(
                Flow.Subscriber<? super String> subscriber) {
            return fromLineSubscriber(subscriber, any -> null, StandardCharsets.UTF_8, null);
        }

        public static <S extends Flow.Subscriber<? super String>, T> BodySubscriber<T>
                fromLineSubscriber(S subscriber, Function<? super S, ? extends T> finisher,
                                   Charset charset, String lineSeparator) {
            Objects.requireNonNull(subscriber);
            Objects.requireNonNull(finisher);
            Objects.requireNonNull(charset);
            if (lineSeparator != null && lineSeparator.isEmpty()) {
                throw new IllegalArgumentException("empty line separator");
            }
            return new Responses.Collecting<>(bytes -> {
                Responses.feedLines(subscriber, new String(bytes, charset), lineSeparator);
                return finisher.apply(subscriber);
            });
        }

        public static BodySubscriber<String> ofString(Charset charset) {
            Objects.requireNonNull(charset);
            return new Responses.Collecting<>(bytes -> new String(bytes, charset));
        }

        public static BodySubscriber<byte[]> ofByteArray() {
            return new Responses.Collecting<>(bytes -> bytes);
        }

        public static BodySubscriber<Path> ofFile(Path file, OpenOption... openOptions) {
            Objects.requireNonNull(file);
            OpenOption[] options = openOptions.length == 0
                    ? new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING}
                    : openOptions.clone();
            return new Responses.Collecting<>(bytes -> {
                try {
                    Files.write(file, bytes, options);
                } catch (IOException failed) {
                    throw new UncheckedIOException(failed);
                }
                return file;
            });
        }

        public static BodySubscriber<Path> ofFile(Path file) {
            return ofFile(file, new OpenOption[0]);
        }

        public static BodySubscriber<Void> ofByteArrayConsumer(
                Consumer<Optional<byte[]>> consumer) {
            Objects.requireNonNull(consumer);
            return new Responses.Collecting<>(bytes -> {
                if (bytes.length > 0) {
                    consumer.accept(Optional.of(bytes));
                }
                consumer.accept(Optional.empty());
                return null;
            });
        }

        public static BodySubscriber<InputStream> ofInputStream() {
            return new Responses.Collecting<>(ByteArrayInputStream::new);
        }

        public static BodySubscriber<Stream<String>> ofLines(Charset charset) {
            Objects.requireNonNull(charset);
            return new Responses.Collecting<>(bytes -> new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(bytes), charset)).lines());
        }

        public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>> ofPublisher() {
            return new Responses.Collecting<>(Responses::replay);
        }

        public static <U> BodySubscriber<U> replacing(U value) {
            return new Responses.Collecting<>(bytes -> value);
        }

        public static BodySubscriber<Void> discarding() {
            return new Responses.Collecting<>(bytes -> null);
        }

        public static <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream,
                                                      int bufferSize) {
            Objects.requireNonNull(downstream);
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("must be greater than 0");
            }
            return downstream;
        }

        public static <T, U> BodySubscriber<U> mapping(BodySubscriber<T> upstream,
                                                       Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(upstream);
            Objects.requireNonNull(mapper);
            return new Responses.Mapped<>(upstream, mapper);
        }
    }
}
