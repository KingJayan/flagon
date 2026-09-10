package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

/** prints more lines than the shell keeps, so the log's cap is exercised. */
public final class ChattyProgram implements Program {

    private static final int DEFAULT_LINES = 2500;

    @Override
    public String id() {
        return "chatty";
    }

    @Override
    public String description() {
        return "prints thousands of lines, the way a per-frame log would";
    }

    @Override
    public void run(String[] arguments) {
        int lines = arguments.length > 0 ? Integer.parseInt(arguments[0]) : DEFAULT_LINES;
        for (int line = 1; line <= lines; line++) {
            Platforms.get().console().info("line " + line);
        }
    }
}
