package dev.flagon.runtime;

import dev.flagon.platform.Platforms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagonRuntimeTest {

    @AfterEach
    void tearDown() {
        Platforms.reset();
    }

    private static Program program(String id, List<String> log) {
        return new Program() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String description() {
                return "test program " + id;
            }

            @Override
            public void run(String[] arguments) {
                log.add(id + String.join(",", arguments));
            }
        };
    }

    @Test
    void reportsPhasesInOrderAndExitsCleanly() {
        List<String> ran = new ArrayList<>();
        FakePlatform platform = new FakePlatform("hello", "world");
        ProgramRegistry registry = new ProgramRegistry().register(program("hello", ran));

        int code = FlagonRuntime.boot(platform, registry);

        assertEquals(0, code);
        assertEquals(List.of("helloworld"), ran);
        assertEquals(
                List.of(
                        "status:platform:fake",
                        "status:select:hello",
                        "status:program:hello",
                        "ready",
                        "exited:0"),
                platform.events);
    }

    @Test
    void installsPlatformBeforeRunningProgram() {
        List<String> ran = new ArrayList<>();
        FakePlatform platform = new FakePlatform();
        ProgramRegistry registry = new ProgramRegistry().register(new Program() {
            @Override
            public String id() {
                return "probe";
            }

            @Override
            public String description() {
                return "checks the platform is reachable";
            }

            @Override
            public void run(String[] arguments) {
                ran.add(Platforms.get().name());
            }
        });

        assertEquals(0, FlagonRuntime.boot(platform, registry));
        assertEquals(List.of("fake"), ran);
    }

    @Test
    void defaultsToTheFirstRegisteredProgramWhenNoArgumentsAreGiven() {
        List<String> ran = new ArrayList<>();
        FakePlatform platform = new FakePlatform();
        ProgramRegistry registry = new ProgramRegistry()
                .register(program("first", ran))
                .register(program("second", ran));

        assertEquals(0, FlagonRuntime.boot(platform, registry));
        assertEquals(List.of("first"), ran);
    }

    @Test
    void reportsUnknownProgramAsFatalInTheSelectPhase() {
        FakePlatform platform = new FakePlatform("nope");
        ProgramRegistry registry = new ProgramRegistry().register(program("hello", new ArrayList<>()));

        assertEquals(-1, FlagonRuntime.boot(platform, registry));
        assertTrue(platform.events.contains("fatal:select"), platform.events.toString());
        assertInstanceOf(IllegalArgumentException.class, platform.fatalError);
        assertTrue(platform.fatalError.getMessage().contains("hello"));
    }

    @Test
    void reportsProgramFailureAsFatalWithoutThrowing() {
        FakePlatform platform = new FakePlatform("boom");
        ProgramRegistry registry = new ProgramRegistry().register(new Program() {
            @Override
            public String id() {
                return "boom";
            }

            @Override
            public String description() {
                return "always fails";
            }

            @Override
            public void run(String[] arguments) {
                throw new IllegalStateException("expected failure");
            }
        });

        assertEquals(-1, FlagonRuntime.boot(platform, registry));
        assertEquals("fatal:program", platform.events.get(platform.events.size() - 1));
        assertNotNull(platform.fatalError);
        assertEquals("expected failure", platform.fatalError.getMessage());
    }

    @Test
    void rejectsDuplicateProgramIds() {
        ProgramRegistry registry = new ProgramRegistry().register(program("dup", new ArrayList<>()));
        assertThrows(IllegalStateException.class, () -> registry.register(program("dup", new ArrayList<>())));
    }

    @Test
    void reportsAnEmptyRegistryAsFatal() {
        FakePlatform platform = new FakePlatform();
        assertEquals(-1, FlagonRuntime.boot(platform, new ProgramRegistry()));
        assertTrue(platform.events.contains("fatal:select"), platform.events.toString());
    }
}
