package dev.flagon.shims;

import org.teavm.vm.spi.TeaVMHost;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class BuildClasspath {

    private BuildClasspath() {
    }

    /**
     * reads only the classpath of the bundle, so no jar of the build tool gets in. the teavm
     * daemon loads the bundle through url loaders over the application loader
     */
    static ClassLoader of(TeaVMHost host) {
        List<URL> urls = new ArrayList<>();
        ClassLoader tool = ClassLoader.getSystemClassLoader();
        for (ClassLoader at = host.getClassLoader(); at instanceof URLClassLoader listed
                && at != tool; at = at.getParent()) {
            urls.addAll(Arrays.asList(listed.getURLs()));
        }
        return urls.isEmpty()
                ? host.getClassLoader()
                : new URLClassLoader(urls.toArray(URL[]::new), null);
    }
}
