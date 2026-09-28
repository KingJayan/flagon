package dev.flagon.shims;

import org.teavm.vm.spi.TeaVMHost;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class BuildClasspath {

    static final String PATHS = "flagon.classpath";

    private BuildClasspath() {
    }

    /** reads only the classpath of the bundle, so no jar of the build tool gets in */
    static ClassLoader of(TeaVMHost host) {
        String listed = host.getProperties().getProperty(PATHS, "");
        if (listed.isBlank()) {
            return host.getClassLoader();
        }
        List<URL> urls = new ArrayList<>();
        for (String entry : listed.split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                urls.add(urlOf(Path.of(entry.trim())));
            }
        }
        return new URLClassLoader(urls.toArray(URL[]::new), null);
    }

    private static URL urlOf(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException unusable) {
            throw new IllegalStateException(path + " is not a classpath entry", unusable);
        }
    }
}
