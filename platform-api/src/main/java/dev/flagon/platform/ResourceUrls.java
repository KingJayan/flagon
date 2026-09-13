package dev.flagon.platform;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/** a url for a file in the pack. the scheme is ours, and only this class opens it. */
public final class ResourceUrls {

    public static final String SCHEME = "flagon";

    private static final URLStreamHandler HANDLER = new PackHandler();

    private ResourceUrls() {
    }

    public static URL of(String name) {
        String clean = Resources.clean(name);
        if (!Resources.has(clean)) {
            return null;
        }
        try {
            return new URL(SCHEME, "", -1, "/" + clean, HANDLER);
        } catch (MalformedURLException cannotHappen) {
            return null;
        }
    }

    private static final class PackHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) {
            return new PackConnection(url);
        }
    }

    private static final class PackConnection extends URLConnection {

        PackConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (!Resources.has(url.getPath())) {
                throw new IOException("this bundle carries no " + url.getPath());
            }
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return Resources.open(url.getPath());
        }

        @Override
        public int getContentLength() {
            return Resources.sizeOf(url.getPath());
        }

        @Override
        public long getContentLengthLong() {
            return getContentLength();
        }
    }
}
