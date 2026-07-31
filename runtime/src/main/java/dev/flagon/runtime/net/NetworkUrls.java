package dev.flagon.runtime.net;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.net.Network;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * makes {@code java.net.URL} open http through the platform's network
 *
 * <p>this is the seam an unmodified application already reads through, the way
 * {@code StoreFileSystem} is for {@code java.io}. https is left alone: a browser page cannot
 * speak tls down a raw socket, and pretending otherwise would be worse than not offering it.
 */
public final class NetworkUrls {

    private static boolean installed;

    private NetworkUrls() {
    }

    /** installs the handler once per process; a second call, or a host that has none, does nothing. */
    public static void install(Network network, ConsoleSink console) {
        if (installed || network == null || !network.isAvailable()) {
            return;
        }
        installed = true;
        try {
            URL.setURLStreamHandlerFactory(new Handlers(network));
        } catch (Error taken) {
            // something else got there first, which is its right; http urls keep going wherever
            // they went before rather than the runtime failing to boot over it
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
            return "http".equalsIgnoreCase(protocol) ? new HttpHandler(network) : null;
        }
    }

    private static final class HttpHandler extends URLStreamHandler {

        private final Network network;

        private HttpHandler(Network network) {
            this.network = network;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new NetworkHttpConnection(url, network);
        }

        @Override
        protected int getDefaultPort() {
            return 80;
        }
    }
}
