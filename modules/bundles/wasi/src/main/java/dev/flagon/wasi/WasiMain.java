package dev.flagon.wasi;

import dev.flagon.platform.wasi.WasiPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;
import org.teavm.interop.Export;

/** a command line catalog: the first argument names the program, as in the browser */
public final class WasiMain {

    private WasiMain() {
    }

    public static void main(String[] arguments) {
        System.exit(FlagonRuntime.boot(WasiPlatform.create(arguments), registry()));
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new HelloProgram())
                .register(new FilesProgram())
                .register(new ThreadsProgram())
                .register(new ExitProgram());
    }

    /** called from outside, with no program running */
    @Export(name = "add")
    public static int add(int a, int b) {
        return a + b;
    }

    @Export(name = "fibonacci")
    public static long fibonacci(int n) {
        long previous = 0;
        long current = 1;
        for (int i = 0; i < n; i++) {
            long next = previous + current;
            previous = current;
            current = next;
        }
        return previous;
    }
}
