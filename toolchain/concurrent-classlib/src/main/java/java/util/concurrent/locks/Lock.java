package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * what code writes when it holds a lock through the interface rather than the class.
 * newCondition is left out: there is no condition queue over the green threads yet, and a
 * method that is absent fails at build time instead of at the wrong moment.
 */
public interface Lock {

    void lock();

    void lockInterruptibly() throws InterruptedException;

    boolean tryLock();

    boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException;

    void unlock();
}
