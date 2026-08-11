package dev.flagon.app;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;

/**
 * entry point of the bundle built from whatever jar is in {@code vendor/}
 *
 * <p>the same composition root every bundle has. what changes is that its programs are written by
 * the build from what the jar says about itself, so a jar can be run without writing any java.
 */
public final class AppMain {

    private AppMain() {
    }

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    /** the bundle's programs, read by the build to write the catalog beside the bundle */
    public static ProgramRegistry registry() {
        return AppEntry.registry();
    }
}
