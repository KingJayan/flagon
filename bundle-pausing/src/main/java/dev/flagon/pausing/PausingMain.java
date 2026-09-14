package dev.flagon.pausing;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;

public final class PausingMain {

    private PausingMain() {
    }

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new MainProgram("load", "loads with string setup that can pause",
                        PausingMain::load));
    }

    private static void load(String[] arguments) {
        if (arguments.length > 99) {
            throw new PausingException();
        }
        String first = "Aa";
        String second = "BB";
        System.out.println("pausing: the bundle loaded, and two names with one hash stay apart "
                + (first.hashCode() == second.hashCode() && first != second.intern()));
    }
}
