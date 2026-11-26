package java.net.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * sends requests through the host's fetch. a browser applies its CORS rules, hides redirects
 * it does not follow, and keeps cookies and proxies to itself.
 */
public abstract class HttpClient implements AutoCloseable {

    protected HttpClient() {
    }

    public static HttpClient newHttpClient() {
        return newBuilder().build();
    }

    public static Builder newBuilder() {
        return new FetchClient.Building();
    }

    public interface Builder {

        Builder connectTimeout(Duration duration);

        Builder executor(Executor executor);

        Builder followRedirects(Redirect policy);

        Builder version(Version version);

        Builder priority(int priority);

        Builder authenticator(Authenticator authenticator);

        default Builder localAddress(InetAddress localAddr) {
            throw new UnsupportedOperationException();
        }

        HttpClient build();
    }

    public abstract Optional<Duration> connectTimeout();

    public abstract Redirect followRedirects();

    public abstract Optional<Authenticator> authenticator();

    public abstract Version version();

    public abstract Optional<Executor> executor();

    public enum Version {
        HTTP_1_1,
        HTTP_2
    }

    public enum Redirect {
        NEVER,
        ALWAYS,
        NORMAL
    }

    public abstract <T> HttpResponse<T> send(HttpRequest request,
                                             HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException;

    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler);

    public abstract <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler);

    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    public boolean awaitTermination(Duration duration) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public boolean isTerminated() {
        throw new UnsupportedOperationException();
    }

    public void shutdownNow() {
        throw new UnsupportedOperationException();
    }

    /** stops new requests; unlike the jdk's, it does not wait for those already sent */
    @Override
    public void close() {
        shutdown();
    }
}
