package dev.flagon.runtime.net;

import dev.flagon.platform.net.Network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code java.net.HttpURLConnection} answered over a {@link Network}
 */
final class NetworkHttpConnection extends HttpURLConnection {

    private final Network network;
    private final Map<String, List<String>> requested = new LinkedHashMap<>();
    private ByteArrayOutputStream sending;
    private HttpExchange exchange;

    NetworkHttpConnection(URL url, Network network) {
        super(url);
        this.network = network;
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        byte[] body = sending == null ? null : sending.toByteArray();
        exchange = HttpExchange.send(network, url, getRequestMethod(), requested, body);
        connected = true;
        responseCode = exchange.status();
        responseMessage = exchange.message();
    }

    @Override
    public void disconnect() {
        if (exchange != null) {
            exchange.close();
        }
        connected = false;
    }

    @Override
    public boolean usingProxy() {
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (connected) {
            throw new IOException("the request has already been sent to " + url);
        }
        if (!doOutput) {
            throw new IOException("this connection was not opened for output; call setDoOutput");
        }
        if (sending == null) {
            sending = new ByteArrayOutputStream();
        }
        return sending;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (responseCode >= 400) {
            throw new IOException("the server answered " + responseCode + " " + responseMessage
                    + " for " + url);
        }
        return exchange.body();
    }

    /** what the server sent about a failure, which is where an application looks for the reason */
    @Override
    public InputStream getErrorStream() {
        if (exchange == null || responseCode < 400) {
            return null;
        }
        return exchange.body();
    }

    @Override
    public int getResponseCode() throws IOException {
        connect();
        return responseCode;
    }

    @Override
    public String getResponseMessage() throws IOException {
        connect();
        return responseMessage;
    }

    @Override
    public String getHeaderField(String name) {
        List<String> values = header(name);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            connect();
        } catch (IOException unreachable) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(exchange.headers());
    }

    @Override
    public void setRequestProperty(String name, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        requested.put(name, values);
    }

    @Override
    public void addRequestProperty(String name, String value) {
        requested.computeIfAbsent(name, any -> new ArrayList<>()).add(value);
    }

    @Override
    public String getRequestProperty(String name) {
        List<String> values = requested.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return Collections.unmodifiableMap(requested);
    }

    private List<String> header(String name) {
        try {
            connect();
        } catch (IOException unreachable) {
            return Collections.emptyList();
        }
        return exchange.headers().getOrDefault(name.toLowerCase(Locale.ROOT),
                Collections.emptyList());
    }
}
