package dev.flagon.wasi;

import dev.flagon.runtime.Program;

public final class HelloProgram implements Program {

    @Override
    public String id() {
        return "hello";
    }

    @Override
    public String description() {
        return "greets its arguments and does a little arithmetic";
    }

    @Override
    public void run(String[] arguments) {
        String who = arguments.length == 0 ? "wasi" : String.join(" ", arguments);
        System.out.println("Hello, " + who + "!");
        System.out.println("sin(1) = " + Math.sin(1) + ", 2^0.5 = " + Math.pow(2, 0.5));
        System.out.println("the clock is running: " + (System.currentTimeMillis() > 0));
        System.err.println("and stderr is separate");
    }
}
