package java.net.http;

import dev.flagon.platform.Platforms;
import dev.flagon.platform.net.Fetch;
import dev.flagon.platform.net.NetworkException;

import java.io.IOException;
import java.net.Authenticator;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/** an {@link HttpClient} over the platform's {@link Fetch} */
final class FetchClient extends HttpClient {

    private final Duration connectTimeout;
    private final Executor executor;
    private final Redirect redirect;
    private final Version version;
    private final Authenticator authenticator;
    private boolean shut;
    private int sending;

    private FetchClient(Building built) {
        this.connectTimeout = built.connectTimeout;
        this.executor = built.executor;
        this.redirect = built.redirect;
        this.version = built.version;
        this.authenticator = built.authenticator;
    }

    static final class Building implements HttpClient.Builder {

        private Duration connectTimeout;
        private Executor executor;
        private Redirect redirect = Redirect.NEVER;
        private Version version = Version.HTTP_2;
        private Authenticator authenticator;

        @Override
        public Builder connectTimeout(Duration duration) {
            Objects.requireNonNull(duration);
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Invalid duration: " + duration);
            }
            this.connectTimeout = duration;
            return this;
        }

        @Override
        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
            return this;
        }

        @Override
        public Builder followRedirects(Redirect policy) {
            this.redirect = Objects.requireNonNull(policy);
            return this;
        }

        @Override
        public Builder version(Version version) {
            this.version = Objects.requireNonNull(version);
            return this;
        }

        @Override
        public Builder priority(int priority) {
            if (priority < 1 || priority > 256) {
                throw new IllegalArgumentException("priority must be between 1 and 256");
            }
            return this;
        }

        @Override
        public Builder authenticator(Authenticator authenticator) {
            this.authenticator = Objects.requireNonNull(authenticator);
            return this;
        }

        @Override
        public HttpClient build() {
            return new FetchClient(this);
        }
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    @Override
    public Redirect followRedirects() {
        return redirect;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return Optional.ofNullable(authenticator);
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public Optional<Executor> executor() {
        return Optional.ofNullable(executor);
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request,
                                    HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(responseBodyHandler);
        if (shut) {
            throw new IOException("the http client is shut down");
        }
        sending++;
        try {
            return exchange(request, responseBodyHandler);
        } finally {
            finished();
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(responseBodyHandler);
        CompletableFuture<HttpResponse<T>> answer = new CompletableFuture<>();
        if (shut) {
            answer.completeExceptionally(new IOException("the http client is shut down"));
            return answer;
        }
        Runnable task = () -> {
            try {
                answer.complete(send(request, responseBodyHandler));
            } catch (Throwable failed) {
                answer.completeExceptionally(failed);
            }
        };
        if (executor != null) {
            executor.execute(task);
        } else {
            new Thread(task, "HttpClient-worker").start();
        }
        return answer;
    }

    /** a browser never pushes, so the push handler is never called */
    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    @Override
    public void shutdown() {
        shut = true;
    }

    /** an answer already on its way still arrives */
    @Override
    public void shutdownNow() {
        shut = true;
    }

    @Override
    public boolean isTerminated() {
        return shut && sending == 0;
    }

    @Override
    public synchronized boolean awaitTermination(Duration duration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + duration.toMillis();
        while (!isTerminated()) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) {
                return false;
            }
            wait(left);
        }
        return true;
    }

    private synchronized void finished() {
        sending--;
        notifyAll();
    }

    private <T> HttpResponse<T> exchange(HttpRequest request,
                                         HttpResponse.BodyHandler<T> handler)
            throws IOException {
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        request.headers().map().forEach((name, values) -> {
            for (String value : values) {
                headers.add(Map.entry(name, value));
            }
        });
        byte[] body = null;
        Optional<HttpRequest.BodyPublisher> publisher = request.bodyPublisher();
        if (publisher.isPresent()) {
            byte[] bytes = Requests.collect(publisher.get());
            String method = request.method();
            if (bytes.length > 0 || !(method.equals("GET") || method.equals("HEAD"))) {
                body = bytes;
            }
        }
        Duration wait = request.timeout().orElse(null);
        boolean connecting = connectTimeout != null
                && (wait == null || connectTimeout.compareTo(wait) < 0);
        if (connecting) {
            wait = connectTimeout;
        }
        Fetch.Response answer;
        try {
            answer = Platforms.get().fetch().send(new Fetch.Request(request.method(),
                    request.uri().toString(), headers, body, wait == null ? 0L : wait.toMillis(),
                    redirect != Redirect.NEVER));
        } catch (Fetch.TimedOut late) {
            throw connecting ? new HttpConnectTimeoutException("HTTP connect timed out")
                    : new HttpTimeoutException("request timed out");
        } catch (NetworkException failed) {
            throw new IOException(failed.getMessage(), failed);
        }
        HttpHeaders received = HttpHeaders.of(answer.headers(), (name, value) -> true);
        HttpResponse.BodySubscriber<T> subscriber =
                handler.apply(new Responses.Info(answer.status(), received, Version.HTTP_1_1));
        Responses.feed(subscriber, answer.body());
        T value;
        try {
            value = subscriber.getBody().toCompletableFuture().join();
        } catch (CompletionException failed) {
            Throwable cause = failed.getCause();
            throw cause instanceof IOException io ? io
                    : new IOException("the response body failed", cause);
        }
        URI from = URI.create(answer.url());
        HttpRequest asked = from.equals(request.uri()) ? request
                : HttpRequest.newBuilder(request, (name, text) -> true).uri(from).build();
        return new Responses.Answered<>(answer.status(), asked, received, value, from,
                Version.HTTP_1_1);
    }
}
