package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

/** prints the concurrency checks, which the browser test compares with what the jdk printed */
public final class ConcurrencyProgram implements Program {

    @Override
    public String id() {
        return "concurrency";
    }

    @Override
    public String description() {
        return "barriers, phasers, fork/join, skip lists and completable futures on green threads";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        for (String line : ConcurrencyChecks.run()) {
            console.info("concurrency: " + line);
        }
        console.info("concurrency: done");
    }
}
