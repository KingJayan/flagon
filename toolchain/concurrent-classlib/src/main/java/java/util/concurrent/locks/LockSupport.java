package java.util.concurrent.locks;

/** a green thread parks by waiting on itself, and unparks when another thread says so. */
public final class LockSupport {

    private LockSupport() {
    }

    public static void park() {
        park(Thread.currentThread());
    }

    public static void park(Object blocker) {
        synchronized (blocker) {
            try {
                blocker.wait();
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void parkNanos(long nanos) {
        parkNanos(Thread.currentThread(), nanos);
    }

    public static void parkNanos(Object blocker, long nanos) {
        long millis = Math.max(1L, nanos / 1_000_000L);
        synchronized (blocker) {
            try {
                blocker.wait(millis);
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void unpark(Thread thread) {
        if (thread == null) {
            return;
        }
        synchronized (thread) {
            thread.notifyAll();
        }
    }
}
