package java.util.concurrent;

/** parties wait here until all have come; the last to come runs the action and opens it again. */
public class CyclicBarrier {

    /** one use of the barrier; a reset or a break starts a new one */
    private static final class Generation {
        boolean broken;
    }

    private final int parties;
    private final Runnable action;
    private Generation generation = new Generation();
    private int waiting;

    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties must be positive: " + parties);
        }
        this.parties = parties;
        this.action = barrierAction;
    }

    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    public int getParties() {
        return parties;
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return arrive(false, 0L);
        } catch (TimeoutException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public int await(long timeout, TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        return arrive(true, unit.toMillis(timeout));
    }

    public boolean isBroken() {
        return generation.broken;
    }

    public synchronized void reset() {
        breakBarrier();
        generation = new Generation();
    }

    public int getNumberWaiting() {
        return waiting;
    }

    private synchronized int arrive(boolean timed, long millis)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        Generation current = generation;
        if (current.broken) {
            throw new BrokenBarrierException();
        }
        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }
        int index = parties - 1 - waiting;
        if (index == 0) {
            boolean ran = false;
            try {
                if (action != null) {
                    action.run();
                }
                ran = true;
                waiting = 0;
                generation = new Generation();
                notifyAll();
                return 0;
            } finally {
                if (!ran) {
                    breakBarrier();
                }
            }
        }
        waiting++;
        long deadline = System.currentTimeMillis() + millis;
        while (true) {
            try {
                if (!timed) {
                    wait();
                } else if (millis > 0L) {
                    wait(millis);
                }
            } catch (InterruptedException stopped) {
                if (current == generation && !current.broken) {
                    breakBarrier();
                    throw stopped;
                }
                Thread.currentThread().interrupt();
            }
            if (current.broken) {
                throw new BrokenBarrierException();
            }
            if (current != generation) {
                return index;
            }
            if (timed) {
                millis = deadline - System.currentTimeMillis();
                if (millis <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        }
    }

    private void breakBarrier() {
        generation.broken = true;
        waiting = 0;
        notifyAll();
    }
}
