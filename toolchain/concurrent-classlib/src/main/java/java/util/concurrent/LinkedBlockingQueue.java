package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

/**
 * a queue that holds a taker until something arrives.
 *
 * <p>a taker suspends its green thread, which hands the page on, so a worker waiting here costs
 * the frame loop nothing.
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final Deque<E> items = new ArrayDeque<>();
    private final int capacity;

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("a queue holds at least one: " + capacity);
        }
        this.capacity = capacity;
    }

    public LinkedBlockingQueue(Collection<? extends E> initial) {
        this(Integer.MAX_VALUE);
        addAll(initial);
    }

    @Override
    public synchronized boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException("a queue holds no nulls");
        }
        if (items.size() >= capacity) {
            return false;
        }
        items.addLast(item);
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean offer(E item, long timeout, TimeUnit unit)
            throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (items.size() >= capacity && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return offer(item);
    }

    @Override
    public synchronized void put(E item) throws InterruptedException {
        while (!offer(item)) {
            wait();
        }
    }

    @Override
    public synchronized E poll() {
        E item = items.pollFirst();
        if (item != null) {
            notifyAll();
        }
        return item;
    }

    @Override
    public synchronized E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (items.isEmpty() && remaining > 0L) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return poll();
    }

    @Override
    public synchronized E take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        return poll();
    }

    @Override
    public synchronized E peek() {
        return items.peekFirst();
    }

    @Override
    public synchronized int size() {
        return items.size();
    }

    @Override
    public synchronized int remainingCapacity() {
        return capacity - items.size();
    }

    @Override
    public synchronized boolean remove(Object item) {
        boolean removed = items.remove(item);
        if (removed) {
            notifyAll();
        }
        return removed;
    }

    @Override
    public synchronized boolean contains(Object item) {
        return items.contains(item);
    }

    @Override
    public int drainTo(Collection<? super E> into) {
        return drainTo(into, Integer.MAX_VALUE);
    }

    @Override
    public synchronized int drainTo(Collection<? super E> into, int most) {
        int moved = 0;
        while (moved < most && !items.isEmpty()) {
            into.add(items.removeFirst());
            moved++;
        }
        if (moved > 0) {
            notifyAll();
        }
        return moved;
    }

    @Override
    public synchronized Iterator<E> iterator() {
        return new ArrayDeque<>(items).iterator();
    }
}
