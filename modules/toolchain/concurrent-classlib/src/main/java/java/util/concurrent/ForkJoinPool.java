package java.util.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class ForkJoinPool extends AbstractExecutorService {

    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool pool);
    }

    public interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
    }

    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory =
            DefaultFactory::new;

    private static final int MAX_CAP = 0x7FFF;
    private static ForkJoinPool common;

    private final int parallelism;
    private final ForkJoinWorkerThreadFactory factory;
    private final Thread.UncaughtExceptionHandler handler;
    private final boolean asyncMode;
    private final boolean isCommon;
    private final ArrayDeque<ForkJoinTask<?>> queue = new ArrayDeque<>();
    private final List<ForkJoinWorkerThread> workers = new ArrayList<>();
    private int idle;
    private int active;
    private int named;
    private boolean shutdown;

    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()));
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory,
                        Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
        this(parallelism, factory, handler, asyncMode, false);
    }

    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory,
                        Thread.UncaughtExceptionHandler handler, boolean asyncMode,
                        int corePoolSize, int maximumPoolSize, int minimumRunnable,
                        Predicate<? super ForkJoinPool> saturate, long keepAliveTime,
                        TimeUnit unit) {
        this(parallelism, factory, handler, asyncMode, false);
    }

    private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory,
                         Thread.UncaughtExceptionHandler handler, boolean asyncMode,
                         boolean isCommon) {
        if (parallelism <= 0 || parallelism > MAX_CAP) {
            throw new IllegalArgumentException("parallelism out of range: " + parallelism);
        }
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        this.parallelism = parallelism;
        this.factory = factory;
        this.handler = handler;
        this.asyncMode = asyncMode;
        this.isCommon = isCommon;
    }

    public static synchronized ForkJoinPool commonPool() {
        if (common == null) {
            int size = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            common = new ForkJoinPool(Math.min(MAX_CAP, size), defaultForkJoinWorkerThreadFactory,
                    null, false, true);
        }
        return common;
    }

    public static int getCommonPoolParallelism() {
        return commonPool().parallelism;
    }

    /** the pool of the thread this runs on, or null off a worker */
    static ForkJoinPool current() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread worker ? worker.getPool()
                : null;
    }

    public <T> T invoke(ForkJoinTask<T> task) {
        push(task);
        return task.join();
    }

    public void execute(ForkJoinTask<?> task) {
        push(task);
    }

    @Override
    public void execute(Runnable task) {
        push(task instanceof ForkJoinTask<?> forked ? forked : ForkJoinTask.adapt(task));
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        push(task);
        return task;
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return submit(ForkJoinTask.adapt(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return submit(ForkJoinTask.adapt(task, result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ForkJoinTask<?> submit(Runnable task) {
        return submit(task instanceof ForkJoinTask<?> forked ? (ForkJoinTask<Object>) forked
                : ForkJoinTask.adapt(task, null));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            futures.add(submit(task));
        }
        for (Future<T> future : futures) {
            ((ForkJoinTask<T>) future).quietlyJoin();
        }
        return futures;
    }

    synchronized void push(ForkJoinTask<?> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        if (shutdown) {
            throw new RejectedExecutionException("the pool is shut down");
        }
        queue.addLast(task);
        if (idle > 0) {
            notifyAll();
        } else if (workers.size() < parallelism) {
            ForkJoinWorkerThread worker = factory.newThread(this);
            if (worker != null) {
                workers.add(worker);
                worker.start();
            }
        }
    }

    boolean unqueue(ForkJoinTask<?> task) {
        return queue.removeIf(queued -> queued == task);
    }

    int nextIndex() {
        return named++;
    }

    /** the loop each worker runs until the pool shuts down */
    void work() {
        while (true) {
            ForkJoinTask<?> task;
            synchronized (this) {
                while (queue.isEmpty() && !shutdown) {
                    idle++;
                    try {
                        wait();
                    } catch (InterruptedException stopped) {
                        // a worker leaves only when the pool shuts down
                    } finally {
                        idle--;
                    }
                }
                if (queue.isEmpty()) {
                    workers.remove(Thread.currentThread());
                    notifyAll();
                    return;
                }
                task = queue.pollFirst();
                active++;
            }
            try {
                task.doExec();
            } catch (Throwable thrown) {
                if (handler != null) {
                    handler.uncaughtException(Thread.currentThread(), thrown);
                }
            } finally {
                synchronized (this) {
                    active--;
                    notifyAll();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        if (isCommon) {
            return;
        }
        synchronized (this) {
            shutdown = true;
            notifyAll();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (isCommon) {
            return List.of();
        }
        List<ForkJoinTask<?>> pending;
        synchronized (this) {
            shutdown = true;
            pending = new ArrayList<>(queue);
            queue.clear();
            notifyAll();
        }
        for (ForkJoinTask<?> task : pending) {
            task.cancel(false);
        }
        return List.of();
    }

    @Override
    public void close() {
        if (!isCommon) {
            super.close();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown && queue.isEmpty() && workers.isEmpty();
    }

    public boolean isTerminating() {
        return shutdown && !isTerminated();
    }

    @Override
    public synchronized boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (isCommon) {
            return awaitQuiescence(timeout, unit);
        }
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (!isTerminated()) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0L) {
                return false;
            }
            wait(left);
        }
        return true;
    }

    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        long deadline = millis == Long.MAX_VALUE ? Long.MAX_VALUE
                : System.currentTimeMillis() + millis;
        ForkJoinPool here = current();
        while (true) {
            ForkJoinTask<?> help = null;
            synchronized (this) {
                if (queue.isEmpty() && active <= (here == this ? 1 : 0)) {
                    return true;
                }
                if (here == this) {
                    help = queue.pollFirst();
                }
                if (help == null) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0L) {
                        return false;
                    }
                    try {
                        wait(left);
                    } catch (InterruptedException stopped) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            if (help != null) {
                help.doExec();
            }
        }
    }

    public boolean isQuiescent() {
        return queue.isEmpty() && active == 0;
    }

    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        while (!blocker.isReleasable()) {
            if (blocker.block()) {
                return;
            }
        }
    }

    public int getParallelism() {
        return parallelism;
    }

    public int getPoolSize() {
        return workers.size();
    }

    public int getActiveThreadCount() {
        return active;
    }

    public int getRunningThreadCount() {
        return active;
    }

    public long getQueuedTaskCount() {
        return queue.size();
    }

    public int getQueuedSubmissionCount() {
        return queue.size();
    }

    public boolean hasQueuedSubmissions() {
        return !queue.isEmpty();
    }

    public long getStealCount() {
        return 0L;
    }

    public boolean getAsyncMode() {
        return asyncMode;
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return factory;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return handler;
    }

    @Override
    public String toString() {
        String state = !shutdown ? "Running" : isTerminated() ? "Terminated" : "Shutting down";
        return super.toString() + "[" + state + ", parallelism = " + parallelism + ", size = "
                + workers.size() + ", active = " + active + ", queued tasks = " + queue.size()
                + "]";
    }

    /** the threads a pool makes unless told otherwise */
    private static final class DefaultFactory extends ForkJoinWorkerThread {
        DefaultFactory(ForkJoinPool pool) {
            super(pool);
        }
    }
}
