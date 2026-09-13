package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * a pool that holds work back until its time comes.
 *
 * <p>one green thread watches the list, sleeps until the next task is due, and runs it. sleeping
 * is what hands the page on, so a schedule here keeps the frame loop going rather than blocking
 * it. a repeating task is put back on the list once it has run.
 */
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor
        implements ScheduledExecutorService {

    private final List<Scheduled<?>> waiting = new ArrayList<>();
    private final Object clock = new Object();

    private boolean watching;
    private boolean runDelayedAfterShutdown = true;
    private boolean runPeriodicAfterShutdown;

    public ScheduledThreadPoolExecutor(int coreSize) {
        super(coreSize, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public ScheduledThreadPoolExecutor(int coreSize, ThreadFactory factory) {
        super(coreSize, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), factory);
    }

    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean keep) {
        this.runDelayedAfterShutdown = keep;
    }

    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return runDelayedAfterShutdown;
    }

    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean keep) {
        this.runPeriodicAfterShutdown = keep;
    }

    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return runPeriodicAfterShutdown;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return schedule(Executors.callable(task), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return hold(new Scheduled<>(task, due(delay, unit), 0L, false));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period,
                                                  TimeUnit unit) {
        requirePeriod(period);
        return hold(new Scheduled<>(Executors.callable(task), due(initialDelay, unit),
                unit.toMillis(period), true));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay,
                                                     TimeUnit unit) {
        requirePeriod(delay);
        return hold(new Scheduled<>(Executors.callable(task), due(initialDelay, unit),
                unit.toMillis(delay), false));
    }

    private static void requirePeriod(long period) {
        if (period <= 0L) {
            throw new IllegalArgumentException(
                    "a repeating task needs a period above zero, got " + period);
        }
    }

    private static long due(long delay, TimeUnit unit) {
        return System.currentTimeMillis() + Math.max(0L, unit.toMillis(delay));
    }

    private <V> ScheduledFuture<V> hold(Scheduled<V> task) {
        if (isShutdown()) {
            throw new RejectedExecutionException("this executor has been shut down");
        }
        synchronized (clock) {
            waiting.add(task);
            if (!watching) {
                watching = true;
                Thread watcher = getThreadFactory().newThread(this::watch);
                watcher.setDaemon(true);
                watcher.start();
            }
            clock.notifyAll();
        }
        return task;
    }

    private void watch() {
        while (true) {
            Scheduled<?> next;
            synchronized (clock) {
                if (waiting.isEmpty()) {
                    watching = false;
                    return;
                }
                next = earliest();
                long sleep = next.dueAt - System.currentTimeMillis();
                if (sleep > 0L) {
                    try {
                        clock.wait(sleep);
                    } catch (InterruptedException interrupted) {
                        watching = false;
                        return;
                    }
                    continue;
                }
                waiting.remove(next);
            }
            if (!allowedNow(next)) {
                continue;
            }
            next.run();
            synchronized (clock) {
                if (next.period > 0L && !next.isCancelled()) {
                    next.dueAt = next.fixedRate
                            ? next.dueAt + next.period
                            : System.currentTimeMillis() + next.period;
                    waiting.add(next);
                }
            }
        }
    }

    private boolean allowedNow(Scheduled<?> task) {
        if (!isShutdown()) {
            return true;
        }
        return task.period > 0L ? runPeriodicAfterShutdown : runDelayedAfterShutdown;
    }

    private Scheduled<?> earliest() {
        Scheduled<?> soonest = waiting.get(0);
        for (Scheduled<?> task : waiting) {
            if (task.dueAt < soonest.dueAt) {
                soonest = task;
            }
        }
        return soonest;
    }

    @Override
    public List<Runnable> shutdownNow() {
        synchronized (clock) {
            waiting.clear();
            clock.notifyAll();
        }
        return super.shutdownNow();
    }

    /** work with a time on it, which is its own future once it has run */
    private static final class Scheduled<V> extends FutureTask<V>
            implements RunnableScheduledFuture<V> {

        private final Callable<V> work;
        private final long period;
        private final boolean fixedRate;

        private long dueAt;

        Scheduled(Callable<V> work, long dueAt, long period, boolean fixedRate) {
            super(work);
            this.work = work;
            this.dueAt = dueAt;
            this.period = period;
            this.fixedRate = fixedRate;
        }

        /**
         * a repeating task never settles: it has no one result, and the real one never reports
         * done either. so the work is called straight and the future is left as it was.
         */
        @Override
        public void run() {
            if (!isPeriodic()) {
                super.run();
                return;
            }
            try {
                work.call();
            } catch (Exception uncaught) {
                // a repeating task that throws is stopped, the way the real one stops it
                cancel(false);
            }
        }

        @Override
        public boolean isPeriodic() {
            return period > 0L;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(dueAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS),
                    other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
