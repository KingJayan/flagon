package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.SortedSet;

/** an unbounded priority queue threads share; take waits while it is empty. */
public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private static final int DEFAULT_CAPACITY = 11;

    private final PriorityQueue<E> heap;

    public PriorityBlockingQueue() {
        this(DEFAULT_CAPACITY, null);
    }

    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("capacity must be positive: " + initialCapacity);
        }
        heap = new PriorityQueue<>(initialCapacity, comparator);
    }

    @SuppressWarnings("unchecked")
    public PriorityBlockingQueue(Collection<? extends E> items) {
        Comparator<? super E> order = null;
        if (items instanceof SortedSet<?> sorted) {
            order = (Comparator<? super E>) sorted.comparator();
        } else if (items instanceof PriorityBlockingQueue<?> queue) {
            order = (Comparator<? super E>) queue.comparator();
        } else if (items instanceof PriorityQueue<?> queue) {
            order = (Comparator<? super E>) queue.comparator();
        }
        heap = new PriorityQueue<>(Math.max(1, items.size()), order);
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

    @Override
    public E poll() {
        return heap.poll();
    }

    @Override
    public synchronized E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long remaining = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + remaining;
        while (heap.isEmpty() && remaining > 0L) {
            await(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        return heap.poll();
    }

    @Override
    public synchronized E take() throws InterruptedException {
        while (heap.isEmpty()) {
            await();
        }
        return heap.poll();
    }

    @Override
    public E peek() {
        return heap.peek();
    }

    public Comparator<? super E> comparator() {
        return heap.comparator();
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
    public boolean contains(Object item) {
        return heap.contains(item);
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

    @Override
    public int drainTo(Collection<? super E> into, int most) {
        if (into == this) {
            throw new IllegalArgumentException("a queue cannot drain into itself");
        }
        int moved = 0;
        while (moved < most && !heap.isEmpty()) {
            into.add(heap.poll());
            moved++;
        }
        return moved;
    }

    /** walks a snapshot, as the jdk's does; remove goes to the queue */
    @Override
    public Iterator<E> iterator() {
        return new ConcurrentSkipListMap.Walk<>(new ArrayList<>(heap), this::removeSame);
    }

    private void removeSame(E item) {
        heap.removeIf(each -> each == item);
    }

    @Override
    public String toString() {
        return heap.toString();
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
