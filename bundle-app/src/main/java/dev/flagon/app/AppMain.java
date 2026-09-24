package dev.flagon.app;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;

/**
 * entry point of bundle
 */
public final class AppMain {

    private AppMain() {
    }

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return AppEntry.registry();
    }
}
