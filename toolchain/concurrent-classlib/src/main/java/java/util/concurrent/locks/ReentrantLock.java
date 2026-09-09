package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/** a lock one green thread holds while others wait their turn at it. */
public class ReentrantLock {

    private final Object monitor = new Object();

    private Thread owner;
    private int held;

    public ReentrantLock() {
    }

    public ReentrantLock(boolean fair) {
    }

    public void lock() {
        synchronized (monitor) {
            Thread caller = Thread.currentThread();
            while (owner != null && owner != caller) {
                try {
                    monitor.wait();
                } catch (InterruptedException interrupted) {
                    // a lock is taken either way; the interrupt is left for the caller to see
                    Thread.currentThread().interrupt();
                }
            }
            owner = caller;
            held++;
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        synchronized (monitor) {
            Thread caller = Thread.currentThread();
            while (owner != null && owner != caller) {
                monitor.wait();
            }
            owner = caller;
            held++;
        }
    }

    public boolean tryLock() {
        synchronized (monitor) {
            Thread caller = Thread.currentThread();
            if (owner != null && owner != caller) {
                return false;
            }
            owner = caller;
            held++;
            return true;
        }
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        synchronized (monitor) {
            Thread caller = Thread.currentThread();
            while (owner != null && owner != caller && remaining > 0L) {
                monitor.wait(remaining);
                remaining = deadline - System.currentTimeMillis();
            }
            if (owner != null && owner != caller) {
                return false;
            }
            owner = caller;
            held++;
            return true;
        }
    }

    public void unlock() {
        synchronized (monitor) {
            if (owner != Thread.currentThread()) {
                throw new IllegalMonitorStateException("this thread does not hold the lock");
            }
            if (--held == 0) {
                owner = null;
                monitor.notifyAll();
            }
        }
    }

    public boolean isLocked() {
        synchronized (monitor) {
            return owner != null;
        }
    }

    public boolean isHeldByCurrentThread() {
        synchronized (monitor) {
            return owner == Thread.currentThread();
        }
    }

    public int getHoldCount() {
        synchronized (monitor) {
            return owner == Thread.currentThread() ? held : 0;
        }
    }
}
