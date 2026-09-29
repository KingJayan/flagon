package dev.flagon.platform;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * holds objects under the small dense names an api like gl or al hands out. a name is an index,
 * so a lookup boxes nothing, and a freed name comes back before a new one is made.
 */
public final class Names<T> implements Iterable<T> {

    private final List<T> held = new ArrayList<>();
    private final BitSet free = new BitSet();
    private int[] freeNames = new int[16];
    private int head;
    private int tail;

    public Names() {
        held.add(null);
    }

    public int add(T object) {
        int reused = take();
        if (reused == 0) {
            held.add(object);
            return held.size() - 1;
        }
        held.set(reused, object);
        return reused;
    }

    /** puts an object under a name the caller chose, filling any gap before it */
    public void put(int name, T object) {
        if (name <= 0) {
            throw new IllegalArgumentException("not a name: " + name);
        }
        while (held.size() <= name) {
            release(held.size());
            held.add(null);
        }
        free.clear(name);
        held.set(name, object);
    }

    public T get(int name) {
        return name > 0 && name < held.size() ? held.get(name) : null;
    }

    public boolean has(int name) {
        return get(name) != null;
    }

    /** @return what the name held, or null if it held nothing */
    public T remove(int name) {
        T was = get(name);
        if (was == null) {
            return null;
        }
        held.set(name, null);
        release(name);
        return was;
    }

    /** one past the highest name in use */
    public int size() {
        return held.size();
    }

    public void clear() {
        held.clear();
        held.add(null);
        free.clear();
        head = 0;
        tail = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {

            private int at = next(1);

            private int next(int from) {
                int name = from;
                while (name < held.size() && held.get(name) == null) {
                    name++;
                }
                return name;
            }

            @Override
            public boolean hasNext() {
                return at < held.size();
            }

            @Override
            public T next() {
                if (at >= held.size()) {
                    throw new NoSuchElementException();
                }
                T object = held.get(at);
                at = next(at + 1);
                return object;
            }
        };
    }

    private void release(int name) {
        if (free.get(name)) {
            return;
        }
        free.set(name);
        if (tail == freeNames.length) {
            compact();
        }
        freeNames[tail++] = name;
    }

    private int take() {
        while (head < tail) {
            int name = freeNames[head++];
            if (free.get(name)) {
                free.clear(name);
                return name;
            }
        }
        head = 0;
        tail = 0;
        return 0;
    }

    private void compact() {
        int kept = tail - head;
        int[] into = kept * 2 > freeNames.length ? new int[freeNames.length * 2] : freeNames;
        System.arraycopy(freeNames, head, into, 0, kept);
        freeNames = into;
        head = 0;
        tail = kept;
    }
}
