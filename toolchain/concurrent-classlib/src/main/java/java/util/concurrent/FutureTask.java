package java.util.concurrent;

/**
 * work and the result it will have, in one object.
 *
 * <p>{@link #run} calls the work and settles the future with what it gave or what it threw.
 * a thread that asks for the result before then suspends, which hands the page on.
 */
public class FutureTask<V> implements RunnableFuture<V> {

    private final GreenFuture<V> future;

    public FutureTask(Callable<V> work) {
        if (work == null) {
            throw new NullPointerException("there is nothing to call");
        }
        this.future = new GreenFuture<>(work);
    }

    public FutureTask(Runnable work, V result) {
        this(Executors.callable(work, result));
    }

    @Override
    public void run() {
        future.run();
        done();
    }

    /** a subclass hooks in here, the way the real one lets one. */
    protected void done() {
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = future.cancel(mayInterruptIfRunning);
        if (cancelled) {
            done();
        }
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}
