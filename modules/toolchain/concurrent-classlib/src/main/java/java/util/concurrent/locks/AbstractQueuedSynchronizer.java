package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

public abstract class AbstractQueuedSynchronizer {

    private final Object monitor = new Object();

    private volatile int state;

    protected AbstractQueuedSynchronizer() {
    }

    protected final int getState() {
        return state;
    }

    protected final void setState(int value) {
        synchronized (monitor) {
            state = value;
            monitor.notifyAll();
        }
    }

    protected final boolean compareAndSetState(int expected, int value) {
        synchronized (monitor) {
            if (state != expected) {
                return false;
            }
            state = value;
            monitor.notifyAll();
            return true;
        }
    }

    protected boolean tryAcquire(int argument) {
        throw new UnsupportedOperationException("this synchronizer does not acquire exclusively");
    }

    protected boolean tryRelease(int argument) {
        throw new UnsupportedOperationException("this synchronizer does not release exclusively");
    }

    protected int tryAcquireShared(int argument) {
        throw new UnsupportedOperationException("this synchronizer does not acquire shared");
    }

    protected boolean tryReleaseShared(int argument) {
        throw new UnsupportedOperationException("this synchronizer does not release shared");
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException("this synchronizer has no exclusive holder");
    }

    public final void acquire(int argument) {
        waitUninterruptibly(() -> tryAcquire(argument));
    }

    public final void acquireInterruptibly(int argument) throws InterruptedException {
        waitUntil(() -> tryAcquire(argument), true, 0L);
    }

    public final boolean tryAcquireNanos(int argument, long nanos) throws InterruptedException {
        return waitUntil(() -> tryAcquire(argument), true, nanos);
    }

    public final boolean release(int argument) {
        if (!tryRelease(argument)) {
            return false;
        }
        wakeEveryone();
        return true;
    }

    public final void acquireShared(int argument) {
        waitUninterruptibly(() -> tryAcquireShared(argument) >= 0);
    }

    public final void acquireSharedInterruptibly(int argument) throws InterruptedException {
        waitUntil(() -> tryAcquireShared(argument) >= 0, true, 0L);
    }

    public final boolean tryAcquireSharedNanos(int argument, long nanos)
            throws InterruptedException {
        return waitUntil(() -> tryAcquireShared(argument) >= 0, true, nanos);
    }

    public final boolean releaseShared(int argument) {
        if (!tryReleaseShared(argument)) {
            return false;
        }
        wakeEveryone();
        return true;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    /** an interrupt does not stop this acquire, so it is kept for the caller to see later. */
    private void waitUninterruptibly(Ready ready) {
        try {
            waitUntil(ready, false, 0L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void wakeEveryone() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    /** @return whether the test passed before the time ran out */
    private boolean waitUntil(Ready ready, boolean interruptible, long nanos)
            throws InterruptedException {
        long remaining = nanos <= 0L ? 0L : Math.max(1L, nanos / 1000000L);
        long deadline = System.currentTimeMillis() + remaining;
        synchronized (monitor) {
            while (!ready.test()) {
                if (nanos > 0L && remaining <= 0L) {
                    return false;
                }
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException interrupted) {
                    if (interruptible) {
                        throw interrupted;
                    }
                    Thread.currentThread().interrupt();
                }
                if (nanos > 0L) {
                    remaining = deadline - System.currentTimeMillis();
                }
            }
            return true;
        }
    }

    /** @return whether the synchronizer is in the state the caller is waiting for */
    private interface Ready {

        boolean test();
    }

    /** the condition a synchronizer hands out, which waits on the same monitor the state does. */
    public class ConditionObject implements Condition {

        private final Object queue = new Object();

        public ConditionObject() {
        }

        @Override
        public void await() throws InterruptedException {
            synchronized (queue) {
                queue.wait();
            }
        }

        @Override
        public void awaitUninterruptibly() {
            try {
                await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public long awaitNanos(long nanos) throws InterruptedException {
            long deadline = System.nanoTime() + nanos;
            synchronized (queue) {
                queue.wait(Math.max(1L, nanos / 1000000L));
            }
            return deadline - System.nanoTime();
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return awaitNanos(unit.toNanos(time)) > 0L;
        }

        @Override
        public void signal() {
            synchronized (queue) {
                queue.notify();
            }
        }

        @Override
        public void signalAll() {
            synchronized (queue) {
                queue.notifyAll();
            }
        }
    }
}
