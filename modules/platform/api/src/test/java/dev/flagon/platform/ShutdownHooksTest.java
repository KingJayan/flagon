package dev.flagon.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownHooksTest {

    private final Runtime runtime = Runtime.getRuntime();

    @BeforeEach
    void start() {
        ShutdownHooks.reset();
    }

    private static Thread hook(List<String> ran, String name) {
        return new Thread(() -> ran.add(name), name);
    }

    @Test
    void runsEveryHookInTheOrderItWasAdded() {
        List<String> ran = new ArrayList<>();
        ShutdownHooks.add(runtime, hook(ran, "first"));
        ShutdownHooks.add(runtime, hook(ran, "second"));

        ShutdownHooks.runAll();

        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    void runsTheHooksOnlyOnce() {
        List<String> ran = new ArrayList<>();
        ShutdownHooks.add(runtime, hook(ran, "once"));

        ShutdownHooks.runAll();
        ShutdownHooks.runAll();

        assertEquals(List.of("once"), ran);
    }

    @Test
    void keepsGoingWhenOneHookThrows() {
        List<String> ran = new ArrayList<>();
        ShutdownHooks.add(runtime, new Thread(() -> {
            throw new IllegalStateException("this hook is broken");
        }, "broken"));
        ShutdownHooks.add(runtime, hook(ran, "after"));

        ShutdownHooks.runAll();

        assertEquals(List.of("after"), ran);
    }

    @Test
    void forgetsAHookThatWasRemoved() {
        List<String> ran = new ArrayList<>();
        Thread going = hook(ran, "going");
        ShutdownHooks.add(runtime, going);

        assertTrue(ShutdownHooks.remove(runtime, going));
        assertFalse(ShutdownHooks.remove(runtime, going));
        ShutdownHooks.runAll();

        assertEquals(List.of(), ran);
    }

    @Test
    void refusesTheSameHookTwice() {
        Thread same = hook(new ArrayList<>(), "same");
        ShutdownHooks.add(runtime, same);

        assertThrows(IllegalArgumentException.class, () -> ShutdownHooks.add(runtime, same));
    }

    @Test
    void refusesAHookOnceShutdownHasStarted() {
        ShutdownHooks.runAll();

        assertThrows(IllegalStateException.class,
            () -> ShutdownHooks.add(runtime, hook(new ArrayList<>(), "late")));
    }

    @Test
    void countsWhatIsStillWaiting() {
        assertEquals(0, ShutdownHooks.pending());
        ShutdownHooks.add(runtime, hook(new ArrayList<>(), "waiting"));

        assertEquals(1, ShutdownHooks.pending());
    }
}
