package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * a pool of green threads that take work off a queue.
 *
 * <p>the size arguments are kept and answered, but only one worker runs at a time: the threads
 * under this switch where a task suspends, so a pool buys ordering and a place to queue, never
 * more work at once. a program that sizes a pool from the core count gets what it asked for and
 * finishes its work; it just does not finish it sooner.
 */
public class ThreadPoolExecutor extends AbstractExecutorService {

    private final BlockingQueue<Runnable> queue;
    private final List<Thread> workers = new ArrayList<>();
    private final int coreSize;
    private final int maximumSize;
    private final long keepAliveMillis;

    private ThreadFactory factory;
    private boolean shutdown;
    private int idle;
    private int busy;
    private long completed;

    public ThreadPoolExecutor(int coreSize, int maximumSize, long keepAlive, TimeUnit unit,
                              BlockingQueue<Runnable> queue) {
        this(coreSize, maximumSize, keepAlive, unit, queue, Executors.defaultThreadFactory());
    }

    public ThreadPoolExecutor(int coreSize, int maximumSize, long keepAlive, TimeUnit unit,
                              BlockingQueue<Runnable> queue, ThreadFactory factory) {
        if (coreSize < 0 || maximumSize <= 0 || maximumSize < coreSize || keepAlive < 0) {
            throw new IllegalArgumentException(
                    "a pool of " + coreSize + ".." + maximumSize + " holding work for "
                            + keepAlive + " is not a pool");
        }
        if (queue == null || factory == null) {
            throw new NullPointerException("a pool needs a queue and a thread factory");
        }
        this.coreSize = coreSize;
        this.maximumSize = maximumSize;
        this.keepAliveMillis = unit.toMillis(keepAlive);
        this.queue = queue;
        this.factory = factory;
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("there is nothing to run");
        }
        synchronized (this) {
            if (shutdown) {
                throw new RejectedExecutionException("this executor has been shut down");
            }
            if (!queue.offer(task)) {
                throw new RejectedExecutionException("the queue is full");
            }
            if (idle == 0 && workers.size() < maximumSize) {
                start();
            }
            notifyAll();
        }
    }

    private void start() {
        Thread worker = factory.newThread(this::work);
        worker.setDaemon(true);
        workers.add(worker);
        worker.start();
    }

    private void work() {
        while (true) {
            Runnable task;
            synchronized (this) {
                idle++;
                try {
                    long waited = 0L;
                    while (queue.isEmpty() && !shutdown) {
                        if (workers.size() > coreSize && waited >= keepAliveMillis
                                && keepAliveMillis > 0L) {
                            break;
                        }
                        long before = System.currentTimeMillis();
                        wait(keepAliveMillis > 0L ? keepAliveMillis : 0L);
                        waited += System.currentTimeMillis() - before;
                    }
                } catch (InterruptedException interrupted) {
                    idle--;
                    workers.remove(Thread.currentThread());
                    return;
                }
                idle--;
                task = queue.poll();
                if (task == null) {
                    workers.remove(Thread.currentThread());
                    notifyAll();
                    return;
                }
                busy++;
            }
            try {
                task.run();
            } catch (RuntimeException | Error uncaught) {
                // a task that throws takes down its own future, never the worker running it
            } finally {
                synchronized (this) {
                    busy--;
                    completed++;
                    notifyAll();
                }
            }
        }
    }

    public synchronized ThreadFactory getThreadFactory() {
        return factory;
    }

    public synchronized void setThreadFactory(ThreadFactory replacement) {
        if (replacement == null) {
            throw new NullPointerException("a pool needs a thread factory");
        }
        this.factory = replacement;
    }

    public int getCorePoolSize() {
        return coreSize;
    }

    public int getMaximumPoolSize() {
        return maximumSize;
    }

    public synchronized int getPoolSize() {
        return workers.size();
    }

    public synchronized int getActiveCount() {
        return busy;
    }

    public synchronized long getCompletedTaskCount() {
        return completed;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    @Override
    public synchronized List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> abandoned = new ArrayList<>();
        queue.drainTo(abandoned);
        notifyAll();
        return abandoned;
    }

    @Override
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    @Override
    public synchronized boolean isTerminated() {
        return shutdown && queue.isEmpty() && busy == 0;
    }

    @Override
    public synchronized boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (!isTerminated() && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return isTerminated();
    }
}
