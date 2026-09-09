package java.util.concurrent;

/** a result that arrives later, waited on by suspending the green thread that asked for it. */
class GreenFuture<V> implements Future<V> {

    private final Callable<V> work;

    private boolean done;
    private boolean cancelled;
    private boolean running;
    private V value;
    private Throwable failure;

    GreenFuture(Callable<V> work) {
        this.work = work;
    }

    /** @return false when the task was cancelled before anything ran it */
    synchronized boolean claim() {
        if (cancelled) {
            return false;
        }
        running = true;
        return true;
    }

    void run() {
        if (!claim()) {
            return;
        }
        V produced = null;
        Throwable thrown = null;
        try {
            produced = work.call();
        } catch (Throwable failing) {
            thrown = failing;
        }
        settle(produced, thrown);
    }

    private synchronized void settle(V produced, Throwable thrown) {
        if (done) {
            return;
        }
        value = produced;
        failure = thrown;
        done = true;
        running = false;
        notifyAll();
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (done || running) {
            return false;
        }
        cancelled = true;
        done = true;
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        while (!done) {
            wait();
        }
        return taken();
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (!done && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        if (!done) {
            throw new TimeoutException("waited " + unit.toMillis(timeout) + "ms");
        }
        return taken();
    }

    private V taken() throws ExecutionException {
        if (cancelled) {
            throw new CancellationException();
        }
        if (failure != null) {
            throw new ExecutionException(failure);
        }
        return value;
    }
}
