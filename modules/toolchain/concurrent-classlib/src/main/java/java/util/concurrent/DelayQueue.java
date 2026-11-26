package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;

/** an unbounded queue whose items come out only once their delay has run out. */
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final PriorityQueue<E> heap = new PriorityQueue<>();

    public DelayQueue() {
    }

    public DelayQueue(Collection<? extends E> items) {
        addAll(items);
    }

    @Override
    public boolean add(E item) {
        return offer(item);
    }

    @Override
    public boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException();
        }
        heap.offer(item);
        wake();
        return true;
    }

    @Override
    public boolean offer(E item, long timeout, TimeUnit unit) {
        return offer(item);
    }

    @Override
    public void put(E item) {
        offer(item);
    }

    /** the head if its delay has run out, else null */
    @Override
    public E poll() {
        E head = heap.peek();
        return head == null || head.getDelay(TimeUnit.NANOSECONDS) > 0L ? null : heap.poll();
    }

    @Override
    public synchronized E take() throws InterruptedException {
        while (true) {
            E head = heap.peek();
            if (head == null) {
                await();
                continue;
            }
            long due = head.getDelay(TimeUnit.MILLISECONDS);
            if (head.getDelay(TimeUnit.NANOSECONDS) <= 0L) {
                return heap.poll();
            }
            await(Math.max(1L, due));
        }
    }

    @Override
    public synchronized E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (true) {
            E head = heap.peek();
            if (head != null && head.getDelay(TimeUnit.NANOSECONDS) <= 0L) {
                return heap.poll();
            }
            long left = deadline - System.currentTimeMillis();
            if (left <= 0L) {
                return null;
            }
            long due = head == null ? left : Math.max(1L, head.getDelay(TimeUnit.MILLISECONDS));
            await(Math.min(left, due));
        }
    }

    /** the head, due or not */
    @Override
    public E peek() {
        return heap.peek();
    }

    @Override
    public E remove() {
        E head = poll();
        if (head == null) {
            throw new java.util.NoSuchElementException();
        }
        return head;
    }

    @Override
    public int size() {
        return heap.size();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean remove(Object item) {
        return heap.remove(item);
    }

    @Override
    public void clear() {
        heap.clear();
    }

    @Override
    public Object[] toArray() {
        return heap.toArray();
    }

    @Override
    public <T> T[] toArray(T[] into) {
        return heap.toArray(into);
    }

    @Override
    public int drainTo(Collection<? super E> into) {
        return drainTo(into, Integer.MAX_VALUE);
    }

    /** moves only the items that are due */
    @Override
    public int drainTo(Collection<? super E> into, int most) {
        if (into == this) {
            throw new IllegalArgumentException("a queue cannot drain into itself");
        }
        int moved = 0;
        E due;
        while (moved < most && (due = poll()) != null) {
            into.add(due);
            moved++;
        }
        return moved;
    }

    @Override
    public Iterator<E> iterator() {
        return new ConcurrentSkipListMap.Walk<>(new ArrayList<>(heap), this::removeSame);
    }

    private void removeSame(E item) {
        heap.removeIf(each -> each == item);
    }

    /** blocked takers; offer wakes them without pausing, so Collection.add stays plain */
    private int waiting;

    private void await(long millis) throws InterruptedException {
        waiting++;
        try {
            wait(millis);
        } finally {
            waiting--;
        }
    }

    private void await() throws InterruptedException {
        await(0L);
    }

    private void wake() {
        if (waiting > 0) {
            Thread waker = new Thread(() -> {
                synchronized (this) {
                    notifyAll();
                }
            });
            waker.setDaemon(true);
            waker.start();
        }
    }
}
