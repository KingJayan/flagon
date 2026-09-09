package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ExecutorService extends Executor {

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    default <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            futures.add(submit(task));
        }
        for (Future<T> future : futures) {
            try {
                future.get();
            } catch (ExecutionException failed) {
                // a failure belongs to the future that carries it, not to the batch
            }
        }
        return futures;
    }
}
