package java.util.concurrent;

/** a gate the frame loop can wait at while workers finish. */
public class CountDownLatch {

    private int count;

    public CountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        this.count = count;
    }

    public synchronized void countDown() {
        if (count == 0) {
            return;
        }
        if (--count == 0) {
            notifyAll();
        }
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait();
        }
    }

    public synchronized boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (count > 0 && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return count == 0;
    }
}
