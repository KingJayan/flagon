package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E> {

    private final ArrayDeque<E> items = new ArrayDeque<>();

    public ConcurrentLinkedQueue() {
    }

    public ConcurrentLinkedQueue(Collection<? extends E> initial) {
        items.addAll(initial);
    }

    @Override
    public synchronized boolean offer(E item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        return items.offerLast(item);
    }

    @Override
    public synchronized E poll() {
        return items.pollFirst();
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
    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public synchronized Iterator<E> iterator() {
        return new ArrayDeque<>(items).iterator();
    }
}
