package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * what code writes when it holds a lock through the interface rather than the class
 */
public interface Lock {

    void lock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;

    void unlock();

    Condition newCondition();
}
