package dev.flagon.runtime;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.ShutdownHooks;
import dev.flagon.runtime.net.NetworkUrls;

import java.util.Arrays;
import java.util.List;

/*
 boot seq
 */
public final class FlagonRuntime {

    public static final String PHASE_PLATFORM = "platform";
    public static final String PHASE_SELECT = "select";
    public static final String PHASE_PROGRAM = "program";

    private FlagonRuntime() {
    }

    public static int boot(Platform platform, ProgramRegistry registry) {
        if (platform == null) {
            throw new IllegalArgumentException("platform must not be null");
        }
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }

        RuntimeHost host = platform.host();
        ShutdownHooks.reset();
        String phase = PHASE_PLATFORM;
        try {
            host.status(PHASE_PLATFORM, platform.name());
            Platforms.install(platform);

            ConsoleSink console = platform.console();
            ConsoleStreams.install(console);
            console.info("Flagon runtime starting on platform '" + platform.name() + "'");

            NetworkUrls.install(platform.network(), console);

            phase = PHASE_SELECT;
            announce(host, registry);
            String[] arguments = host.arguments();
            if (arguments == null) {
                arguments = new String[0];
            }
            Program program = select(registry, arguments);
            String[] programArguments = arguments.length == 0
                    ? arguments
                    : Arrays.copyOfRange(arguments, 1, arguments.length);
            host.status(PHASE_SELECT, program.id());
            console.info("Selected program '" + program.id() + "' — " + program.description());

            phase = PHASE_PROGRAM;
            host.status(PHASE_PROGRAM, program.id());
            host.ready();
            program.run(programArguments);
            ShutdownHooks.runAll();

            console.info("Program '" + program.id() + "' completed");
            host.exited(0);
            return 0;
        } catch (Throwable error) {
            reportFatal(platform, phase, error);
            return -1;
        } finally {
            ShutdownHooks.runAll();
            ConsoleStreams.restore();
        }
    }

    private static void announce(RuntimeHost host, ProgramRegistry registry) {
        List<Program> programs = registry.all();
        String[] ids = new String[programs.size()];
        String[] descriptions = new String[programs.size()];
        for (int index = 0; index < programs.size(); index++) {
            Program program = programs.get(index);
            ids[index] = program.id();
            descriptions[index] = program.description();
        }
        host.catalog(ids, descriptions);
    }

    private static Program select(ProgramRegistry registry, String[] arguments) {
        if (registry.isEmpty()) {
            throw new IllegalStateException("no programs are registered in this bundle");
        }
        if (arguments.length == 0) {
            return registry.all().get(0);
        }
        Program program = registry.find(arguments[0]);
        if (program == null) {
            throw new IllegalArgumentException(
                    "unknown program '" + arguments[0] + "'; available: " + registry.ids());
        }
        return program;
    }

    private static void reportFatal(Platform platform, String phase, Throwable error) {
        try {
            platform.console().error("Fatal error during phase '" + phase + "'", error);
        } catch (Throwable ignored) {
        }
        platform.host().fatal(phase, error);
    }
}
