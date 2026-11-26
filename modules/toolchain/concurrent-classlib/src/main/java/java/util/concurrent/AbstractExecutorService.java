package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractExecutorService implements ExecutorService {

    protected AbstractExecutorService() {
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> work) {
        return new FutureTask<>(work);
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable work, T result) {
        return new FutureTask<>(work, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> work) {
        RunnableFuture<T> task = newTaskFor(work);
        execute(task);
        return task;
    }

    @Override
    public <T> Future<T> submit(Runnable work, T result) {
        RunnableFuture<T> task = newTaskFor(work, result);
        execute(task);
        return task;
    }

    @Override
    public Future<?> submit(Runnable work) {
        return submit(work, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
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

    /** @return what the first task to finish gave */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("there is nothing to invoke");
        }
        ExecutionException lastFailure = null;
        for (Future<T> future : invokeAll(tasks)) {
            try {
                return future.get();
            } catch (ExecutionException failed) {
                lastFailure = failed;
            }
        }
        throw lastFailure;
    }
}
