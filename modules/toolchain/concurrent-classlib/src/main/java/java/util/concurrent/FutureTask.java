package java.util.concurrent;

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
