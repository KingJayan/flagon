package java.util.concurrent.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLock implements ReadWriteLock {

    private final Object monitor = new Object();
    private final Map<Thread, Integer> readers = new HashMap<>();
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    private Thread writer;
    private int writeHolds;

    public ReentrantReadWriteLock() {
    }

    public ReentrantReadWriteLock(boolean fair) {
    }

    public ReadLock readLock() {
        return readLock;
    }

    public WriteLock writeLock() {
        return writeLock;
    }

    public boolean isWriteLocked() {
        synchronized (monitor) {
            return writer != null;
        }
    }

    public boolean isWriteLockedByCurrentThread() {
        synchronized (monitor) {
            return writer == Thread.currentThread();
        }
    }

    public int getReadLockCount() {
        synchronized (monitor) {
            int total = 0;
            for (int held : readers.values()) {
                total += held;
            }
            return total;
        }
    }

    /** the write lock is out of reach while another thread writes */
    private boolean writtenByAnother() {
        return writer != null && writer != Thread.currentThread();
    }

    public class ReadLock implements Lock {

        @Override
        public void lock() {
            synchronized (monitor) {
                while (writtenByAnother()) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                take();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            synchronized (monitor) {
                while (writtenByAnother()) {
                    monitor.wait();
                }
                take();
            }
        }

        @Override
        public boolean tryLock() {
            synchronized (monitor) {
                if (writtenByAnother()) {
                    return false;
                }
                take();
                return true;
            }
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            long remaining = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + remaining;
            synchronized (monitor) {
                while (writtenByAnother() && remaining > 0L) {
                    monitor.wait(remaining);
                    remaining = deadline - System.currentTimeMillis();
                }
                if (writtenByAnother()) {
                    return false;
                }
                take();
                return true;
            }
        }

        private void take() {
            readers.merge(Thread.currentThread(), 1, Integer::sum);
        }

        @Override
        public void unlock() {
            synchronized (monitor) {
                Thread caller = Thread.currentThread();
                Integer held = readers.get(caller);
                if (held == null) {
                    throw new IllegalMonitorStateException(
                            "this thread does not hold the read lock");
                }
                if (held == 1) {
                    readers.remove(caller);
                    monitor.notifyAll();
                } else {
                    readers.put(caller, held - 1);
                }
            }
        }

        /**
         * a reader cannot wait on a condition: many threads hold the read lock at once, so
         * there is no one holder for a signal to hand it back to. the real one refuses too.
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException(
                    "the read lock has no condition: it is held by many threads at once");
        }
    }

    public class WriteLock implements Lock {

        @Override
        public void lock() {
            synchronized (monitor) {
                if (reenter()) {
                    return;
                }
                refuseAnUpgrade();
                while (writer != null || !readers.isEmpty()) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                take();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            synchronized (monitor) {
                if (reenter()) {
                    return;
                }
                refuseAnUpgrade();
                while (writer != null || !readers.isEmpty()) {
                    monitor.wait();
                }
                take();
            }
        }

        @Override
        public boolean tryLock() {
            synchronized (monitor) {
                if (reenter()) {
                    return true;
                }
                if (writer != null || !readers.isEmpty()) {
                    return false;
                }
                take();
                return true;
            }
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            long remaining = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + remaining;
            synchronized (monitor) {
                if (reenter()) {
                    return true;
                }
                refuseAnUpgrade();
                while ((writer != null || !readers.isEmpty()) && remaining > 0L) {
                    monitor.wait(remaining);
                    remaining = deadline - System.currentTimeMillis();
                }
                if (writer != null || !readers.isEmpty()) {
                    return false;
                }
                take();
                return true;
            }
        }

        private boolean reenter() {
            if (writer != Thread.currentThread()) {
                return false;
            }
            writeHolds++;
            return true;
        }

        private void refuseAnUpgrade() {
            if (readers.containsKey(Thread.currentThread())) {
                throw new IllegalStateException("this thread already holds the read lock, and"
                        + " waiting for the write lock from there would never end. release the"
                        + " read lock first.");
            }
        }

        private void take() {
            writer = Thread.currentThread();
            writeHolds = 1;
        }

        @Override
        public void unlock() {
            synchronized (monitor) {
                if (writer != Thread.currentThread()) {
                    throw new IllegalMonitorStateException(
                            "this thread does not hold the write lock");
                }
                if (--writeHolds == 0) {
                    writer = null;
                    monitor.notifyAll();
                }
            }
        }

        @Override
        public Condition newCondition() {
            return new GreenCondition(new GreenCondition.Held() {
                @Override
                public void requireHeld() {
                    if (!isWriteLockedByCurrentThread()) {
                        throw new IllegalMonitorStateException(
                                "a condition belongs to the write lock, and this thread does "
                                        + "not hold it");
                    }
                }

                @Override
                public int releaseFully() {
                    synchronized (monitor) {
                        int holds = writeHolds;
                        writeHolds = 0;
                        writer = null;
                        monitor.notifyAll();
                        return holds;
                    }
                }

                @Override
                public void takeBack(int holds) {
                    lock();
                    synchronized (monitor) {
                        writeHolds = holds;
                    }
                }
            });
        }
    }
}
