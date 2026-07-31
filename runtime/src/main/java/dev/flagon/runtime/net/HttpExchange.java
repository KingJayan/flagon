package dev.flagon.runtime.net;

import dev.flagon.platform.net.Connection;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.net.NetworkException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * one http request and the response to it, over a {@link Connection}
 */
final class HttpExchange {

    private final Connection connection;
    private final int status;
    private final String message;
    private final Map<String, List<String>> headers;
    private final InputStream body;

    private HttpExchange(Connection connection, int status, String message,
                         Map<String, List<String>> headers, InputStream body) {
        this.connection = connection;
        this.status = status;
        this.message = message;
        this.headers = headers;
        this.body = body;
    }

    static HttpExchange send(Network network, URL url, String method,
                             Map<String, List<String>> requestHeaders, byte[] requestBody)
            throws IOException {
        String host = url.getHost();
        int port = url.getPort() < 0 ? 80 : url.getPort();
        Connection connection = open(network, host, port);
        try {
            byte[] head = head(url, method, host, port, requestHeaders, requestBody).getBytes(
                    StandardCharsets.ISO_8859_1);
            connection.write(head, 0, head.length);
            if (requestBody != null && requestBody.length > 0) {
                connection.write(requestBody, 0, requestBody.length);
            }
            return read(connection, method);
        } catch (IOException | RuntimeException failed) {
            connection.close();
            throw failed;
        }
    }

    int status() {
        return status;
    }

    String message() {
        return message;
    }

    Map<String, List<String>> headers() {
        return headers;
    }

    InputStream body() {
        return body;
    }

    void close() {
        connection.close();
    }

    private static Connection open(Network network, String host, int port) throws IOException {
        try {
            return network.connect(host, port);
        } catch (NetworkException refused) {
            throw new IOException(refused.getMessage(), refused);
        }
    }

    private static String head(URL url, String method, String host, int port,
                               Map<String, List<String>> requestHeaders, byte[] requestBody) {
        StringBuilder head = new StringBuilder();
        head.append(method).append(' ').append(target(url)).append(" HTTP/1.1\r\n");
        head.append("Host: ").append(port == 80 ? host : host + ":" + port).append("\r\n");
        for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
            if (isOurs(header.getKey())) {
                continue;
            }
            for (String value : header.getValue()) {
                head.append(header.getKey()).append(": ").append(value).append("\r\n");
            }
        }
        if (requestBody != null) {
            head.append("Content-Length: ").append(requestBody.length).append("\r\n");
        }
        head.append("Connection: close\r\n\r\n");
        return head.toString();
    }

    /** headers this exchange decides for itself, whatever the caller asked for */
    private static boolean isOurs(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals("host") || lower.equals("connection") || lower.equals("content-length");
    }

    private static String target(URL url) {
        String path = url.getPath() == null || url.getPath().isEmpty() ? "/" : url.getPath();
        return url.getQuery() == null ? path : path + "?" + url.getQuery();
    }

    private static HttpExchange read(Connection connection, String method) throws IOException {
        InputStream raw = new ConnectionStream(connection);
        String statusLine = HttpLines.readLine(raw);
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2 || !parts[0].startsWith("HTTP/")) {
            throw new IOException("the server answered with '" + statusLine + "', which is not http");
        }
        int status = parseStatus(parts[1], statusLine);
        String message = parts.length > 2 ? parts[2] : "";

        Map<String, List<String>> headers = readHeaders(raw);
        InputStream body = body(raw, headers, status, method);
        return new HttpExchange(connection, status, message, headers, body);
    }

    private static int parseStatus(String code, String statusLine) throws IOException {
        try {
            return Integer.parseInt(code);
        } catch (NumberFormatException notANumber) {
            throw new IOException("the server answered with '" + statusLine + "'", notANumber);
        }
    }

    private static Map<String, List<String>> readHeaders(InputStream raw) throws IOException {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        String line = HttpLines.readLine(raw);
        while (!line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), any -> new ArrayList<>())
                        .add(value);
            }
            line = HttpLines.readLine(raw);
        }
        return headers;
    }

    /** how long a body is, in the order http says to work it out */
    private static InputStream body(InputStream raw, Map<String, List<String>> headers,
                                    int status, String method) {
        if ("HEAD".equals(method) || status == 204 || status == 304
                || (status >= 100 && status < 200)) {
            return new BoundedStream(raw, 0);
        }
        String encoding = first(headers, "transfer-encoding");
        if (encoding != null && encoding.toLowerCase(Locale.ROOT).contains("chunked")) {
            return new ChunkedStream(raw);
        }
        String length = first(headers, "content-length");
        if (length != null) {
            try {
                return new BoundedStream(raw, Long.parseLong(length.trim()));
            } catch (NumberFormatException notANumber) {
                // the close still ends the body
            }
        }
        return raw;
    }

    private static String first(Map<String, List<String>> headers, String name) {
        List<String> values = headers.getOrDefault(name, Collections.emptyList());
        return values.isEmpty() ? null : values.get(0);
    }
}
