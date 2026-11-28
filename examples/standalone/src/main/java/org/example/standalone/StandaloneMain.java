package org.example.standalone;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;

public final class StandaloneMain {

    private StandaloneMain() {
    }

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new MainProgram("hello", "prints a line", StandaloneMain::hello));
    }

    private static void hello(String[] arguments) {
        System.out.println("hello from a bundle built outside the flagon repository");
    }
}
