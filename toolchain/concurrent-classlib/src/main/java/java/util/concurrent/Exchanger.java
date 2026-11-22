package java.util.concurrent;

/** two threads meet here and swap one object each. */
public class Exchanger<V> {

    /** what the first of a pair left, and what the second hands back */
    private static final class Meeting {
        final Object offered;
        Object answer;
        boolean answered;

        Meeting(Object offered) {
            this.offered = offered;
        }
    }

    private Meeting open;

    public Exchanger() {
    }

    public V exchange(V item) throws InterruptedException {
        try {
            return meet(item, false, 0L);
        } catch (TimeoutException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public V exchange(V item, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        return meet(item, true, unit.toMillis(timeout));
    }

    @SuppressWarnings("unchecked")
    private synchronized V meet(V item, boolean timed, long millis)
            throws InterruptedException, TimeoutException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Meeting waiting = open;
        if (waiting != null) {
            open = null;
            waiting.answer = item;
            waiting.answered = true;
            notifyAll();
            return (V) waiting.offered;
        }
        Meeting mine = new Meeting(item);
        open = mine;
        long deadline = System.currentTimeMillis() + millis;
        while (!mine.answered) {
            try {
                if (!timed) {
                    wait();
                } else {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0L) {
                        open = null;
                        throw new TimeoutException();
                    }
                    wait(left);
                }
            } catch (InterruptedException stopped) {
                if (!mine.answered) {
                    open = null;
                    throw stopped;
                }
                Thread.currentThread().interrupt();
            }
        }
        return (V) mine.answer;
    }
}
