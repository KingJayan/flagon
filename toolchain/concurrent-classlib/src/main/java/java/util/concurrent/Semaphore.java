package java.util.concurrent;

public class Semaphore {

    private int permits;

    public Semaphore(int permits) {
        this.permits = permits;
    }

    public Semaphore(int permits, boolean fair) {
        this(permits);
    }

    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) {
            wait();
        }
        permits--;
    }

    public synchronized void acquireUninterruptibly() {
        try {
            acquire();
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean tryAcquire() {
        if (permits == 0) {
            return false;
        }
        permits--;
        return true;
    }

    public synchronized boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (permits == 0 && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return tryAcquire();
    }

    public synchronized void release() {
        permits++;
        notifyAll();
    }

    public synchronized int availablePermits() {
        return permits;
    }
}
