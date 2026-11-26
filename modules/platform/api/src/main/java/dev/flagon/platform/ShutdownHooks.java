package dev.flagon.platform;

import java.util.ArrayList;
import java.util.List;

/**
 addShutdownHook, removeShutdownHook and exit
 */
public final class ShutdownHooks {

    private static final List<Thread> HOOKS = new ArrayList<>();

    private static boolean shuttingDown;

    private ShutdownHooks() {
    }

    public static void add(Runtime runtime, Thread hook) {
        if (hook == null) {
            throw new NullPointerException("a shutdown hook must not be null");
        }
        if (shuttingDown) {
            throw new IllegalStateException("shutdown is already in progress");
        }
        if (HOOKS.contains(hook)) {
            throw new IllegalArgumentException("this hook is already registered");
        }
        HOOKS.add(hook);
    }

    public static boolean remove(Runtime runtime, Thread hook) {
        if (shuttingDown) {
            throw new IllegalStateException("shutdown is already in progress");
        }
        return HOOKS.remove(hook);
    }

    public static void exit(Runtime runtime, int code) {
        runAll();
        runtime.exit(code);
    }

    public static void halt(Runtime runtime, int code) {
        runtime.exit(code);
    }

    public static void runAll() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        for (Thread hook : HOOKS) {
            try {
                hook.run();
            } catch (Throwable failed) {
                report(hook, failed);
            }
        }
        HOOKS.clear();
    }

    public static int pending() {
        return HOOKS.size();
    }

    private static void report(Thread hook, Throwable failed) {
        String warning = "flagon: the shutdown hook '" + hook.getName() + "' threw " + failed;
        try {
            Platforms.get().console().warn(warning);
        } catch (RuntimeException noPlatform) {
            System.err.println(warning);
        }
    }

    public static void reset() {
        HOOKS.clear();
        shuttingDown = false;
    }
}
