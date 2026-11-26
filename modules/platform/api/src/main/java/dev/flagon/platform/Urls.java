package dev.flagon.platform;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public final class Urls {

    private Urls() {
    }

    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return url.openConnection();
    }
}
