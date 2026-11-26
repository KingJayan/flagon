package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** java.net.http against the page's own probes */
public final class HttpProgram implements Program {

    @Override
    public String id() {
        return "http";
    }

    @Override
    public String description() {
        return "sends java.net.http requests through fetch (argument: the page's origin)";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        String origin = arguments.length > 0 ? arguments[0] : "http://127.0.0.1:5273";
        URI echo = URI.create(origin + "/flagon-http-echo");
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build();

        HttpResponse<String> got = client.send(HttpRequest.newBuilder(echo).build(),
                HttpResponse.BodyHandlers.ofString());
        console.info("http: get answered " + got.statusCode() + " '" + got.body() + "' echo "
                + got.headers().firstValue("X-Echo").orElse("none"));

        HttpResponse<String> posted = client.send(HttpRequest.newBuilder(echo)
                        .header("x-flagon", "marked")
                        .POST(HttpRequest.BodyPublishers.ofString("héllo")).build(),
                HttpResponse.BodyHandlers.ofString());
        console.info("http: post answered '" + posted.body() + "'");

        HttpResponse<byte[]> put = client.send(HttpRequest.newBuilder(echo)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(new byte[] {1, 2, 3}))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        console.info("http: put read back " + put.body().length + " bytes");

        HttpResponse<Void> missing = client.send(
                HttpRequest.newBuilder(URI.create(echo + "?status=404")).build(),
                HttpResponse.BodyHandlers.discarding());
        console.info("http: a missing page answers " + missing.statusCode());

        CompletableFuture<String> later = client.sendAsync(HttpRequest.newBuilder(echo)
                        .method("PATCH", HttpRequest.BodyPublishers.ofString("async")).build(),
                HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body);
        console.info("http: async answered '" + later.join() + "'");

        HttpResponse<InputStream> streamed = client.send(HttpRequest.newBuilder(echo).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = streamed.body()) {
            console.info("http: a stream reads '"
                    + new String(in.readAllBytes(), StandardCharsets.UTF_8) + "'");
        }

        HttpResponse<String> moved = client.send(
                HttpRequest.newBuilder(URI.create(origin + "/flagon-http-redirect")).build(),
                HttpResponse.BodyHandlers.ofString());
        console.info("http: a redirect ends at " + moved.uri().getQuery());

        refusals(console, origin, echo);
        console.info("http: done");
    }

    private void refusals(ConsoleSink console, String origin, URI echo) throws Exception {
        try {
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(origin + "/flagon-http-redirect")).build(),
                    HttpResponse.BodyHandlers.discarding());
            console.info("http: a redirect went through a client that never follows");
        } catch (IOException hidden) {
            console.info("http: a client that never follows refuses the redirect");
        }
        try {
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(origin + "/flagon-http-slow"))
                            .timeout(Duration.ofMillis(300)).build(),
                    HttpResponse.BodyHandlers.discarding());
            console.info("http: a slow answer came in time");
        } catch (HttpTimeoutException late) {
            console.info("http: a slow answer times out: " + late.getMessage());
        }
        try {
            HttpRequest.newBuilder(echo).header("Host", "elsewhere");
            console.info("http: a host header was taken");
        } catch (IllegalArgumentException restricted) {
            console.info("http: " + restricted.getMessage());
        }
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("text/plain")),
                (name, value) -> true);
        console.info("http: headers ignore case " + headers.firstValue("content-type").isPresent());
    }
}
