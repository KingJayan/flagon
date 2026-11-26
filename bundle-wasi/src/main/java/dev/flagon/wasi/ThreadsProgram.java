package dev.flagon.wasi;

import dev.flagon.runtime.Program;

import java.util.ArrayList;
import java.util.List;

/** threads interleave on one wasm stack; sleeping on all of them sleeps the component */
public final class ThreadsProgram implements Program {

    @Override
    public String id() {
        return "threads";
    }

    @Override
    public String description() {
        return "starts threads that sleep for different times and joins them";
    }

    @Override
    public void run(String[] arguments) throws InterruptedException {
        List<String> woke = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        long start = System.nanoTime();
        for (String name : new String[] {"slow", "quick", "middle"}) {
            int millis = switch (name) {
                case "slow" -> 300;
                case "middle" -> 200;
                default -> 100;
            };
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                woke.add(Thread.currentThread().getName());
            }, name);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long took = (System.nanoTime() - start) / 1_000_000;
        System.out.println("woke in order: " + String.join(", ", woke));
        System.out.println("together, not one after another: " + (took >= 300 && took < 600));
    }
}
