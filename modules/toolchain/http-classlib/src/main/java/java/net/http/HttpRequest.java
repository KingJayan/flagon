package java.net.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class HttpRequest {

    protected HttpRequest() {
    }

    public interface Builder {

        Builder uri(URI uri);

        Builder expectContinue(boolean enable);

        Builder version(HttpClient.Version version);

        Builder header(String name, String value);

        Builder headers(String... headers);

        Builder timeout(Duration duration);

        Builder setHeader(String name, String value);

        Builder GET();

        Builder POST(BodyPublisher publisher);

        Builder PUT(BodyPublisher publisher);

        Builder DELETE();

        default Builder HEAD() {
            return method("HEAD", BodyPublishers.noBody());
        }

        Builder method(String method, BodyPublisher publisher);

        HttpRequest build();

        Builder copy();
    }

    public static Builder newBuilder(URI uri) {
        return new Requests.Building().uri(uri);
    }

    public static Builder newBuilder() {
        return new Requests.Building();
    }

    public static Builder newBuilder(HttpRequest request, BiPredicate<String, String> filter) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(filter);
        Builder builder = newBuilder().uri(request.uri())
                .expectContinue(request.expectContinue());
        request.version().ifPresent(builder::version);
        request.timeout().ifPresent(builder::timeout);
        request.headers().map().forEach((name, values) -> {
            for (String value : values) {
                if (filter.test(name, value)) {
                    builder.header(name, value);
                }
            }
        });
        String method = request.method();
        BodyPublisher body = request.bodyPublisher().orElse(BodyPublishers.noBody());
        switch (method) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            default -> builder.method(method, body);
        }
        return builder;
    }

    public abstract Optional<BodyPublisher> bodyPublisher();

    public abstract String method();

    public abstract Optional<Duration> timeout();

    public abstract boolean expectContinue();

    public abstract URI uri();

    public abstract Optional<HttpClient.Version> version();

    public abstract HttpHeaders headers();

    @Override
    public final boolean equals(Object other) {
        return other instanceof HttpRequest that && method().equals(that.method())
                && uri().equals(that.uri()) && headers().equals(that.headers());
    }

    @Override
    public final int hashCode() {
        return method().hashCode() + uri().hashCode() + headers().hashCode();
    }

    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {
        long contentLength();
    }

    public static class BodyPublishers {

        private BodyPublishers() {
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
            return new Requests.Relayed(Objects.requireNonNull(publisher), -1L);
        }

        public static BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher,
                                                  long contentLength) {
            if (contentLength < 1) {
                throw new IllegalArgumentException("non-positive contentLength: "
                        + contentLength);
            }
            return new Requests.Relayed(Objects.requireNonNull(publisher), contentLength);
        }

        public static BodyPublisher ofString(String body) {
            return ofString(body, StandardCharsets.UTF_8);
        }

        public static BodyPublisher ofString(String body, Charset charset) {
            return ofByteArray(body.getBytes(charset));
        }

        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> supplier) {
            Objects.requireNonNull(supplier);
            return new Requests.Bytes(-1L, () -> {
                try (InputStream in = supplier.get()) {
                    return in.readAllBytes();
                } catch (IOException failed) {
                    throw new UncheckedIOException(failed);
                }
            });
        }

        public static BodyPublisher ofByteArray(byte[] buf) {
            return ofByteArray(buf, 0, buf.length);
        }

        public static BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, buf.length);
            byte[] copy = new byte[length];
            System.arraycopy(buf, offset, copy, 0, length);
            return new Requests.Bytes(length, () -> copy);
        }

        public static BodyPublisher ofFile(Path path) throws FileNotFoundException {
            if (!Files.exists(path)) {
                throw new FileNotFoundException(path + " not found");
            }
            long length;
            try {
                length = Files.size(path);
            } catch (IOException unreadable) {
                length = -1L;
            }
            return new Requests.Bytes(length, () -> {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException failed) {
                    throw new UncheckedIOException(failed);
                }
            });
        }

        public static BodyPublisher ofByteArrays(Iterable<byte[]> iter) {
            Objects.requireNonNull(iter);
            return new Requests.Bytes(-1L, () -> {
                List<byte[]> parts = new ArrayList<>();
                iter.forEach(parts::add);
                return Requests.join(parts);
            });
        }

        public static BodyPublisher noBody() {
            return new Requests.Bytes(0L, () -> new byte[0]);
        }

        public static BodyPublisher concat(BodyPublisher... publishers) {
            List<BodyPublisher> parts = List.of(publishers);
            if (parts.isEmpty()) {
                return noBody();
            }
            if (parts.size() == 1) {
                return parts.get(0);
            }
            long length = 0L;
            for (BodyPublisher part : parts) {
                long each = part.contentLength();
                length = length < 0 || each < 0 ? -1L : length + each;
            }
            return new Requests.Bytes(length, () -> {
                List<byte[]> bytes = new ArrayList<>();
                for (BodyPublisher part : parts) {
                    try {
                        bytes.add(Requests.collect(part));
                    } catch (IOException failed) {
                        throw new UncheckedIOException(failed);
                    }
                }
                return Requests.join(bytes);
            });
        }
    }
}
