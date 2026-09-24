package dev.flagon.runtime.net;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.net.Network;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * makes {@code java.net.URL} open http through network
 */
public final class NetworkUrls {

    private static boolean installed;

    private NetworkUrls() {
    }

    public static void install(Network network, ConsoleSink console) {
        if (installed || network == null || !network.isAvailable()) {
            return;
        }
        installed = true;
        try {
            URL.setURLStreamHandlerFactory(new Handlers(network));
        } catch (Error taken) {
            if (console != null) {
                console.warn("url handlers were already installed; http urls will not go through "
                        + network.name());
            }
        }
    }

    private static final class Handlers implements URLStreamHandlerFactory {

        private final Network network;

        private Handlers(Network network) {
            this.network = network;
        }

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("http".equalsIgnoreCase(protocol)) {
                return new HttpHandler(network, 80);
            }
            return "https".equalsIgnoreCase(protocol) ? new HttpHandler(network, 443) : null;
        }
    }

    private static final class HttpHandler extends URLStreamHandler {

        private final Network network;
        private final int defaultPort;

        private HttpHandler(Network network, int defaultPort) {
            this.network = network;
            this.defaultPort = defaultPort;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new NetworkHttpConnection(url, network);
        }

        @Override
        protected int getDefaultPort() {
            return defaultPort;
        }
    }
}
