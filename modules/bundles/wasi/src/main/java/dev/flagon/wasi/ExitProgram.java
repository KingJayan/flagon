package dev.flagon.wasi;

import dev.flagon.runtime.Program;

/** System.exit runs the shutdown hooks, then ends the component with the status */
public final class ExitProgram implements Program {

    @Override
    public String id() {
        return "exit";
    }

    @Override
    public String description() {
        return "exits with the status its argument names, after a shutdown hook";
    }

    @Override
    public void run(String[] arguments) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("hook ran")));
        System.out.println("exiting");
        System.exit(arguments.length == 0 ? 0 : Integer.parseInt(arguments[0]));
        System.out.println("still here");
    }
}
