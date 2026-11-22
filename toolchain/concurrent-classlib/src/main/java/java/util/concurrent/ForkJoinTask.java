package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ForkJoinTask<V> implements Future<V> {

    private static final int NEW = 0;
    private static final int RUNNING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;

    private int status;
    private Throwable failure;
    private short tag;

    public ForkJoinTask() {
    }

    public abstract V getRawResult();

    protected abstract void setRawResult(V value);

    /** @return whether the task is done; a counted completer finishes later */
    protected abstract boolean exec();

    public final ForkJoinTask<V> fork() {
        ForkJoinPool pool = ForkJoinPool.current();
        (pool != null ? pool : ForkJoinPool.commonPool()).push(this);
        return this;
    }

    public final V join() {
        awaitDone();
        return reported();
    }

    public final V invoke() {
        doExec();
        awaitDone();
        return reported();
    }

    public static void invokeAll(ForkJoinTask<?> first, ForkJoinTask<?> second) {
        second.fork();
        first.quietlyInvoke();
        second.quietlyJoin();
        first.rethrow();
        second.rethrow();
    }

    public static void invokeAll(ForkJoinTask<?>... tasks) {
        invokeAll(List.of(tasks));
    }

    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        List<T> all = new ArrayList<>(tasks);
        for (int at = all.size() - 1; at > 0; at--) {
            all.get(at).fork();
        }
        if (!all.isEmpty()) {
            all.get(0).quietlyInvoke();
        }
        for (T task : all) {
            task.quietlyJoin();
        }
        for (T task : all) {
            task.rethrow();
        }
        return tasks;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (status == NEW) {
                status = CANCELLED;
                failure = new CancellationException();
                notifyAll();
            }
            return status == CANCELLED;
        }
    }

    @Override
    public final boolean isDone() {
        return status >= NORMAL;
    }

    @Override
    public final boolean isCancelled() {
        return status == CANCELLED;
    }

    public final boolean isCompletedAbnormally() {
        return status >= EXCEPTIONAL;
    }

    public final boolean isCompletedNormally() {
        return status == NORMAL;
    }

    public final Throwable getException() {
        return status >= EXCEPTIONAL ? failure : null;
    }

    public void completeExceptionally(Throwable thrown) {
        finish(thrown instanceof RuntimeException || thrown instanceof Error ? thrown
                : new RuntimeException(thrown));
    }

    public void complete(V value) {
        try {
            setRawResult(value);
        } catch (Throwable thrown) {
            finish(thrown);
            return;
        }
        finish(null);
    }

    public final void quietlyComplete() {
        finish(null);
    }

    @Override
    public final V get() throws InterruptedException, ExecutionException {
        awaitDone();
        return got();
    }

    @Override
    public final V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        if (ForkJoinPool.current() != null) {
            doExec();
        }
        synchronized (this) {
            while (status < NORMAL) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0L) {
                    throw new TimeoutException();
                }
                wait(left);
            }
        }
        return got();
    }

    public final void quietlyJoin() {
        awaitDone();
    }

    public final void quietlyInvoke() {
        doExec();
        awaitDone();
    }

    public void reinitialize() {
        status = NEW;
        failure = null;    }

    public static ForkJoinPool getPool() {
        return ForkJoinPool.current();
    }

    public static boolean inForkJoinPool() {
        return ForkJoinPool.current() != null;
    }

    public boolean tryUnfork() {
        ForkJoinPool pool = ForkJoinPool.current();
        return (pool != null ? pool : ForkJoinPool.commonPool()).unqueue(this);
    }

    public static int getQueuedTaskCount() {
        ForkJoinPool pool = ForkJoinPool.current();
        return pool == null ? 0 : pool.getQueuedSubmissionCount();
    }

    public static int getSurplusQueuedTaskCount() {
        return 0;
    }

    public static void helpQuiesce() {
        ForkJoinPool pool = ForkJoinPool.current();
        (pool != null ? pool : ForkJoinPool.commonPool()).awaitQuiescence(Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);
    }

    protected static ForkJoinTask<?> peekNextLocalTask() {
        return null;
    }

    protected static ForkJoinTask<?> pollNextLocalTask() {
        return null;
    }

    protected static ForkJoinTask<?> pollTask() {
        return null;
    }

    public final short getForkJoinTaskTag() {
        return tag;
    }

    public final short setForkJoinTaskTag(short newTag) {
        short old = tag;
        tag = newTag;
        return old;
    }

    public final boolean compareAndSetForkJoinTaskTag(short expected, short newTag) {
        if (tag != expected) {
            return false;
        }
        tag = newTag;
        return true;
    }

    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return adapt(runnable, null);
    }

    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new Adapted<>(() -> {
            runnable.run();
            return result;
        });
    }

    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new Adapted<>(callable);
    }

    /** runs the task here if no thread has started it */
    final void doExec() {
        if (status != NEW) {
            return;
        }
        status = RUNNING;
        boolean done;
        try {
            done = exec();
        } catch (Throwable thrown) {
            finish(thrown);
            return;
        }
        if (done) {
            finish(null);
        }
    }

    final boolean isNew() {
        return status == NEW;
    }

    /** settles the task once; a failure also goes to whatever the task reports to */
    final void finish(Throwable thrown) {
        synchronized (this) {
            if (status >= NORMAL) {
                return;
            }
            status = thrown == null ? NORMAL : EXCEPTIONAL;
            failure = thrown;
            notifyAll();
        }
        if (thrown != null) {
            onFailure(thrown);
        }
    }

    /** a counted completer passes a failure on to its completer */
    void onFailure(Throwable thrown) {
    }

    /** a worker runs an unstarted task itself; any other thread waits for the pool */
    private void awaitDone() {
        if (ForkJoinPool.current() != null) {
            doExec();
        }
        boolean interrupted = false;
        synchronized (this) {
            while (status < NORMAL) {
                try {
                    wait();
                } catch (InterruptedException stopped) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    final void rethrow() {
        Throwable thrown = getException();
        if (thrown instanceof RuntimeException runtime) {
            throw runtime;
        } else if (thrown instanceof Error error) {
            throw error;
        } else if (thrown != null) {
            throw new RuntimeException(thrown);
        }
    }

    private V reported() {
        rethrow();
        return getRawResult();
    }

    private V got() throws ExecutionException {
        if (status == CANCELLED) {
            throw new CancellationException();
        }
        if (status == EXCEPTIONAL) {
            throw new ExecutionException(failure);
        }
        return getRawResult();
    }

    /** a callable or runnable as a task */
    static final class Adapted<T> extends ForkJoinTask<T> implements RunnableFuture<T> {

        private final Callable<? extends T> callable;
        private T result;

        Adapted(Callable<? extends T> callable) {
            this.callable = callable;
        }

        @Override
        public T getRawResult() {
            return result;
        }

        @Override
        protected void setRawResult(T value) {
            result = value;
        }

        @Override
        protected boolean exec() {
            try {
                result = callable.call();
            } catch (RuntimeException | Error unchecked) {
                throw unchecked;
            } catch (Exception checked) {
                throw new RuntimeException(checked);
            }
            return true;
        }

        @Override
        public void run() {
            quietlyInvoke();
        }
    }
}
