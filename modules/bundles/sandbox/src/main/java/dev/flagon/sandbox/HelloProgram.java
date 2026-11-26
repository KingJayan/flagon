package dev.flagon.sandbox;

import dev.flagon.platform.Platform;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

/** smallest hosted program: proves a java main reached the browser. */
public final class HelloProgram implements Program {

    @Override
    public String id() {
        return "hello";
    }

    @Override
    public String description() {
        return "prints a greeting";
    }

    @Override
    public void run(String[] arguments) {
        String who = arguments.length > 0 ? arguments[0] : "world";
        Platform platform = Platforms.get();
        platform.console().info("Hello, " + who + "!");
        platform.console().info("Wall clock: " + platform.clock().currentTimeMillis());
    }
}
