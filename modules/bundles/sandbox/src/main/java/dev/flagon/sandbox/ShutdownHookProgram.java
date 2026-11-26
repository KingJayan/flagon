package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

public final class ShutdownHookProgram implements Program {

    @Override
    public String id() {
        return "shutdownhook";
    }

    @Override
    public String description() {
        return "registers shutdown hooks and lets the runtime run them";
    }

    @Override
    public void run(String[] arguments) {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(() -> say("first hook ran"), "first"));
        runtime.addShutdownHook(new Thread(() -> say("second hook ran"), "second"));

        Thread dropped = new Thread(() -> say("this hook should never run"), "dropped");
        runtime.addShutdownHook(dropped);
        say("removed a hook: " + runtime.removeShutdownHook(dropped));

        say("main is done; the hooks run next");
    }

    private static void say(String line) {
        Platforms.get().console().info(line);
    }
}
