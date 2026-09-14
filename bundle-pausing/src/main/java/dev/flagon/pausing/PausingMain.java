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
        try {
            System.out.println("pausing: a deep pause comes back with " + deepPause(16)
                    + " frames");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static int deepPause(int depth) throws InterruptedException {
        Object[] kept = {"frame"};
        if (depth == 0) {
            Thread.sleep(1);
            return kept.length;
        }
        return deepPause(depth - 1) + kept.length;
    }
}
