package java.util.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * an executor over teavm's green threads: the workers are real threads, but only one of them
 * runs at a time and a switch happens where a task suspends, so this buys ordering rather
 * than parallelism
 */
class GreenPool implements ExecutorService {

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private final List<Thread> workers = new ArrayList<>();
    private final int size;
    private final ThreadFactory factory;

    private boolean shutdown;
    private int idle;
    private int busy;

    GreenPool(int size, ThreadFactory factory) {
        if (size <= 0) {
            throw new IllegalArgumentException("a pool needs at least one worker: " + size);
        }
        this.size = size;
        this.factory = factory;
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        synchronized (this) {
            if (shutdown) {
                throw new RejectedExecutionException("this executor has been shut down");
            }
            queue.addLast(task);
            // a worker is only started when there is nobody free to take the work
            if (idle == 0 && workers.size() < size) {
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
                    while (queue.isEmpty() && !shutdown) {
                        wait();
                    }
                } catch (InterruptedException interrupted) {
                    idle--;
                    return;
                }
                idle--;
                if (queue.isEmpty()) {
                    notifyAll();
                    return;
                }
                task = queue.removeFirst();
                busy++;
            }
            try {
                task.run();
            } catch (RuntimeException | Error uncaught) {
                // a task that throws takes down its own future, never the worker running it
            } finally {
                synchronized (this) {
                    busy--;
                    notifyAll();
                }
            }
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        GreenFuture<T> future = new GreenFuture<>(task);
        execute(future::run);
        return future;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    @Override
    public synchronized List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> abandoned = new ArrayList<>(queue);
        queue.clear();
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
