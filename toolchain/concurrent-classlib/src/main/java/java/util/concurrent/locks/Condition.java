package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * a queue a thread waits on while it holds a lock, and that another thread wakes.
 *
 * <p>the green threads under this switch where a thread suspends, so a wait here gives the page
 * back and a signal hands it on. a thread must hold the lock to wait or to signal, the same rule
 * the real one keeps.
 */
public interface Condition {

    void await() throws InterruptedException;

    void awaitUninterruptibly();

    long awaitNanos(long nanos) throws InterruptedException;

    boolean await(long time, TimeUnit unit) throws InterruptedException;

    void signal();

    void signalAll();
}
