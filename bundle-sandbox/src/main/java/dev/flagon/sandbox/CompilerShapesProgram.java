package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.lang.reflect.Method;

public final class CompilerShapesProgram implements Program {

    private Source source;

    @Override
    public String id() {
        return "shapes";
    }

    @Override
    public String description() {
        return "runs code that the compiler once got wrong";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        Recorder recorder = arguments.length > 0
                ? new QuietRecorder() : new ConsoleRecorder(console);
        source = arguments.length > 0 ? new Counter() : new Sleeper();
        recorder.put("shapes: a split conditional carries ", source == null ? -1 : source.take());
        source = null;
        recorder.put("shapes: a missing source reads ", source == null ? -1 : source.take());

        boolean listed = false;
        for (Method method : ReflectedSignature.class.getDeclaredMethods()) {
            listed |= method.getName().equals("label");
        }
        console.info("shapes: a reflected class still lists label " + listed);
    }

    private abstract static class Recorder {
        abstract void put(String prefix, int value);
    }

    private static final class ConsoleRecorder extends Recorder {
        private final ConsoleSink console;

        ConsoleRecorder(ConsoleSink console) {
            this.console = console;
        }

        @Override
        void put(String prefix, int value) {
            console.info(prefix + value);
        }
    }

    private static final class QuietRecorder extends Recorder {
        @Override
        void put(String prefix, int value) {
        }
    }

    private abstract static class Source {
        abstract int take() throws InterruptedException;
    }

    private static final class Sleeper extends Source {
        @Override
        int take() throws InterruptedException {
            Thread.sleep(1L);
            return 7;
        }
    }

    private static final class Counter extends Source {
        @Override
        int take() {
            return 3;
        }
    }
}
