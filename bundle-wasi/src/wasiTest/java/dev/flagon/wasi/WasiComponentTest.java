package dev.flagon.wasi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WasiComponentTest {

    private static final String COMPONENT = System.getProperty("flagon.wasi.component");
    private static final String WASMTIME = System.getProperty("flagon.wasmtime");

    private record Run(int status, String out, String err) {
    }

    @Test
    void greetsItsArgumentsOnStdoutAndWritesStderrApart() throws Exception {
        Run run = run(List.of(), "hello", "Ada", "Lovelace");
        assertEquals(0, run.status());
        assertEquals("""
                Hello, Ada Lovelace!
                sin(1) = 0.8414709848078965, 2^0.5 = 1.4142135623730951
                the clock is running: true
                """, run.out());
        assertEquals("and stderr is separate\n", run.err());
    }

    @Test
    void keepsFilesInThePreopenedDirectory(@TempDir Path directory) throws Exception {
        Run run = run(List.of("--dir", directory + "::/"), "files");
        assertEquals(0, run.status(), run.err());
        assertEquals("""
                made the directory: true
                read back: first line / second line
                its length: 23
                patched: first LINE
                moved: true false true
                listed: kept.txt, scratch.txt
                deleted: true false false
                """, run.out());
        assertEquals("first LINE\nsecond line\n",
                Files.readString(directory.resolve("notes/kept.txt")));
    }

    @Test
    void sleepsThreadsTogether() throws Exception {
        Run run = run(List.of(), "threads");
        assertEquals("""
                woke in order: quick, middle, slow
                together, not one after another: true
                """, run.out());
    }

    @Test
    void exitsWithTheStatusAfterTheShutdownHooks() throws Exception {
        Run failed = run(List.of(), "exit", "3");
        assertEquals("exiting\nhook ran\n", failed.out());
        // a preview 2 exit is success or failure, so every other status arrives as 1
        assertEquals(1, failed.status());
        assertEquals(0, run(List.of(), "exit", "0").status());
    }

    @Test
    void failsOnAProgramItDoesNotCarry() throws Exception {
        Run run = run(List.of(), "nosuch");
        assertEquals(1, run.status());
        assertTrue(run.err().contains("unknown program 'nosuch'; available: hello, files, threads, exit"),
                run.err());
    }

    @Test
    void exportsNumbersWithNoProgramRunning() throws Exception {
        assertEquals("42\n", run(List.of("--invoke", "add(40, 2)")).out());
        assertEquals("2880067194370816120\n", run(List.of("--invoke", "fibonacci(90)")).out());
    }

    private static Run run(List<String> options, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(WASMTIME, "run", "-W",
                "gc=y,function-references=y,exceptions=y"));
        command.addAll(options);
        command.add(COMPONENT);
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();
        byte[] out = process.getInputStream().readAllBytes();
        byte[] err = process.getErrorStream().readAllBytes();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "wasmtime did not finish");
        return new Run(process.exitValue(), new String(out, StandardCharsets.UTF_8),
                new String(err, StandardCharsets.UTF_8));
    }
}
