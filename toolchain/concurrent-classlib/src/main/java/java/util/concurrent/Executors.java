package java.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public final class Executors {

    private static final AtomicInteger POOLS = new AtomicInteger();

    private Executors() {
    }

    public static ExecutorService newFixedThreadPool(int size) {
        return new GreenPool(size, named("pool-" + POOLS.incrementAndGet()));
    }

    public static ExecutorService newFixedThreadPool(int size, ThreadFactory factory) {
        return new GreenPool(size, factory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return newFixedThreadPool(1);
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory factory) {
        return new GreenPool(1, factory);
    }

    /**
     * a page has one core to give, so an unbounded pool is bounded here. green threads make the
     * count a scheduling choice rather than a hardware one, and more of them buys nothing.
     */
    public static ExecutorService newCachedThreadPool() {
        return newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory factory) {
        return new GreenPool(Runtime.getRuntime().availableProcessors(), factory);
    }

    public static ExecutorService newWorkStealingPool() {
        return newCachedThreadPool();
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return newFixedThreadPool(parallelism);
    }

    public static Callable<Object> callable(Runnable task) {
        return callable(task, null);
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException("there is nothing to call");
        }
        return () -> {
            task.run();
            return result;
        };
    }

    public static ThreadFactory defaultThreadFactory() {
        return named("pool-" + POOLS.incrementAndGet());
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger threads = new AtomicInteger();
        return task -> new Thread(task, prefix + "-thread-" + threads.incrementAndGet());
    }
}
