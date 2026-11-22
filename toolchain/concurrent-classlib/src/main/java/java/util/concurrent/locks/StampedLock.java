package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * a read-write lock that hands out stamps, with optimistic reads. a stamp holds the lock's
 * version; the low bits tell a read stamp (1) from a write stamp (128) from an optimistic one (0).
 * not reentrant, like the jdk's.
 */
public class StampedLock {

    private static final long RBITS = 0x7FL;
    private static final long WBIT = 0x80L;
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS;
    private static final long ORIGIN = WBIT << 1;

    /** the version, with WBIT set while written */
    private long state = ORIGIN;
    private int readers;

    public StampedLock() {
    }

    public long writeLock() {
        try {
            return acquireWrite(false, false, 0L);
        } catch (InterruptedException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public long tryWriteLock() {
        return free() ? takeWrite() : 0L;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        return acquireWrite(true, true, unit.toMillis(time));
    }

    public long writeLockInterruptibly() throws InterruptedException {
        return acquireWrite(true, false, 0L);
    }

    public long readLock() {
        try {
            return acquireRead(false, false, 0L);
        } catch (InterruptedException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public long tryReadLock() {
        return isWritten() ? 0L : takeRead();
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        return acquireRead(true, true, unit.toMillis(time));
    }

    public long readLockInterruptibly() throws InterruptedException {
        return acquireRead(true, false, 0L);
    }

    public long tryOptimisticRead() {
        return isWritten() ? 0L : state & SBITS;
    }

    public boolean validate(long stamp) {
        return (stamp & SBITS) == (state & SBITS);
    }

    public synchronized void unlockWrite(long stamp) {
        if (stamp != state || !isWritten()) {
            throw new IllegalMonitorStateException();
        }
        releaseWrite();
    }

    public synchronized void unlockRead(long stamp) {
        if (!holdsRead(stamp)) {
            throw new IllegalMonitorStateException();
        }
        releaseRead();
    }

    public synchronized void unlock(long stamp) {
        if ((stamp & WBIT) != 0L) {
            unlockWrite(stamp);
        } else {
            unlockRead(stamp);
        }
    }

    public long tryConvertToWriteLock(long stamp) {
        if ((stamp & WBIT) != 0L) {
            return stamp == state && isWritten() ? stamp : 0L;
        }
        if ((stamp & RBITS) != 0L) {
            if (holdsRead(stamp) && readers == 1) {
                readers = 0;
                return takeWrite();
            }
            return 0L;
        }
        return stamp != 0L && validate(stamp) && free() ? takeWrite() : 0L;
    }

    public synchronized long tryConvertToReadLock(long stamp) {
        if ((stamp & WBIT) != 0L) {
            if (stamp != state || !isWritten()) {
                return 0L;
            }
            releaseWrite();
            return takeRead();
        }
        if ((stamp & RBITS) != 0L) {
            return holdsRead(stamp) ? stamp : 0L;
        }
        return stamp != 0L && validate(stamp) && !isWritten() ? takeRead() : 0L;
    }

    public synchronized long tryConvertToOptimisticRead(long stamp) {
        if ((stamp & WBIT) != 0L) {
            if (stamp != state || !isWritten()) {
                return 0L;
            }
            releaseWrite();
            return state & SBITS;
        }
        if ((stamp & RBITS) != 0L) {
            if (!holdsRead(stamp)) {
                return 0L;
            }
            releaseRead();
            return state & SBITS;
        }
        return stamp != 0L && validate(stamp) ? stamp : 0L;
    }

    public synchronized boolean tryUnlockWrite() {
        if (!isWritten()) {
            return false;
        }
        releaseWrite();
        return true;
    }

    public synchronized boolean tryUnlockRead() {
        if (readers == 0) {
            return false;
        }
        releaseRead();
        return true;
    }

    public boolean isWriteLocked() {
        return isWritten();
    }

    public boolean isReadLocked() {
        return readers > 0;
    }

    public int getReadLockCount() {
        return readers;
    }

    public static boolean isWriteLockStamp(long stamp) {
        return (stamp & ABITS) == WBIT;
    }

    public static boolean isReadLockStamp(long stamp) {
        return (stamp & RBITS) != 0L;
    }

    public static boolean isLockStamp(long stamp) {
        return (stamp & ABITS) != 0L;
    }

    public static boolean isOptimisticReadStamp(long stamp) {
        return (stamp & ABITS) == 0L && stamp != 0L;
    }

    public Lock asReadLock() {
        return new View(false);
    }

    public Lock asWriteLock() {
        return new View(true);
    }

    public ReadWriteLock asReadWriteLock() {
        return new ReadWriteLock() {
            @Override
            public Lock readLock() {
                return asReadLock();
            }

            @Override
            public Lock writeLock() {
                return asWriteLock();
            }
        };
    }

    @Override
    public String toString() {
        return super.toString() + (isWritten() ? "[Write-locked]"
                : readers > 0 ? "[Read-locks:" + readers + "]" : "[Unlocked]");
    }

    private boolean isWritten() {
        return (state & WBIT) != 0L;
    }

    private boolean free() {
        return !isWritten() && readers == 0;
    }

    private boolean holdsRead(long stamp) {
        return (stamp & RBITS) != 0L && (stamp & SBITS) == (state & SBITS) && readers > 0;
    }

    private long takeWrite() {
        state |= WBIT;
        return state;
    }

    private long takeRead() {
        readers++;
        return (state & SBITS) | 1L;
    }

    private void releaseWrite() {
        long next = (state & ~ABITS) + ORIGIN;
        state = next == 0L ? ORIGIN : next;
        notifyAll();
    }

    private void releaseRead() {
        if (--readers == 0) {
            notifyAll();
        }
    }

    private synchronized long acquireWrite(boolean interruptible, boolean timed, long millis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (!free()) {
            if (!pause(interruptible, timed, deadline)) {
                return 0L;
            }
        }
        return takeWrite();
    }

    private synchronized long acquireRead(boolean interruptible, boolean timed, long millis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (isWritten()) {
            if (!pause(interruptible, timed, deadline)) {
                return 0L;
            }
        }
        return takeRead();
    }

    /** waits once; false when the time is up */
    private boolean pause(boolean interruptible, boolean timed, long deadline)
            throws InterruptedException {
        if (interruptible && Thread.interrupted()) {
            throw new InterruptedException();
        }
        try {
            if (!timed) {
                wait();
            } else {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0L) {
                    return false;
                }
                wait(left);
            }
        } catch (InterruptedException stopped) {
            if (interruptible) {
                throw stopped;
            }
            Thread.currentThread().interrupt();
        }
        return true;
    }

    /** a lock over one mode of the stamped lock, without stamps */
    private final class View implements Lock {

        private final boolean write;

        View(boolean write) {
            this.write = write;
        }

        @Override
        public void lock() {
            if (write) {
                writeLock();
            } else {
                readLock();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (write) {
                writeLockInterruptibly();
            } else {
                readLockInterruptibly();
            }
        }

        @Override
        public boolean tryLock() {
            return (write ? tryWriteLock() : tryReadLock()) != 0L;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return (write ? tryWriteLock(time, unit) : tryReadLock(time, unit)) != 0L;
        }

        @Override
        public void unlock() {
            boolean released = write ? tryUnlockWrite() : tryUnlockRead();
            if (!released) {
                throw new IllegalMonitorStateException();
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("a stamped lock has no conditions");
        }
    }
}
