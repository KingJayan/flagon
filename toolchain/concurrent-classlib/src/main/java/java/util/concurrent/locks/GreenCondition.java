package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

final class GreenCondition implements Condition {

    interface Held {

        void requireHeld();

        int releaseFully();

        void takeBack(int holds);
    }

    private final Object queue = new Object();
    private final Held lock;

    GreenCondition(Held lock) {
        this.lock = lock;
    }

    @Override
    public void await() throws InterruptedException {
        sleep(0L, true);
    }

    @Override
    public void awaitUninterruptibly() {
        try {
            sleep(0L, false);
        } catch (InterruptedException cannotHappen) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long awaitNanos(long nanos) throws InterruptedException {
        long deadline = System.nanoTime() + nanos;
        sleep(Math.max(1L, nanos / 1000000L), true);
        return deadline - System.nanoTime();
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return awaitNanos(unit.toNanos(time)) > 0L;
    }

    @Override
    public void signal() {
        lock.requireHeld();
        synchronized (queue) {
            queue.notify();
        }
    }

    @Override
    public void signalAll() {
        lock.requireHeld();
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    private void sleep(long millis, boolean interruptible) throws InterruptedException {
        lock.requireHeld();
        int holds = lock.releaseFully();
        try {
            synchronized (queue) {
                queue.wait(millis);
            }
        } catch (InterruptedException interrupted) {
            if (interruptible) {
                lock.takeBack(holds);
                throw interrupted;
            }
            Thread.currentThread().interrupt();
        }
        lock.takeBack(holds);
    }
}
