package java.util.concurrent;

/** a thread of a fork/join pool; a task forked on it goes to the same pool. */
public class ForkJoinWorkerThread extends Thread {

    private final ForkJoinPool pool;
    private final int index;

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("ForkJoinPool-worker-" + pool.nextIndex());
        this.pool = pool;
        this.index = pool.getPoolSize();
        setDaemon(true);
    }

    public ForkJoinPool getPool() {
        return pool;
    }

    public int getPoolIndex() {
        return index;
    }

    protected void onStart() {
    }

    protected void onTermination(Throwable exception) {
    }

    @Override
    public void run() {
        Throwable failure = null;
        try {
            onStart();
            pool.work();
        } catch (Throwable thrown) {
            failure = thrown;
        } finally {
            onTermination(failure);
        }
    }
}
