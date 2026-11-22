package java.util.concurrent;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;

/** a sorted set threads share: the keys of a {@link ConcurrentSkipListMap}. */
public class ConcurrentSkipListSet<E> extends AbstractSet<E>
        implements NavigableSet<E>, Cloneable {

    private final ConcurrentNavigableMap<E, Object> map;
    /** false for a map's key set, which cannot add */
    private final boolean addable;

    public ConcurrentSkipListSet() {
        this(new ConcurrentSkipListMap<>(), true);
    }

    public ConcurrentSkipListSet(Comparator<? super E> comparator) {
        this(new ConcurrentSkipListMap<>(comparator), true);
    }

    public ConcurrentSkipListSet(Collection<? extends E> items) {
        this();
        addAll(items);
    }

    public ConcurrentSkipListSet(SortedSet<E> items) {
        this(items.comparator());
        addAll(items);
    }

    @SuppressWarnings("unchecked")
    ConcurrentSkipListSet(ConcurrentNavigableMap<E, ?> map, boolean addable) {
        this.map = (ConcurrentNavigableMap<E, Object>) map;
        this.addable = addable;
    }

    private ConcurrentSkipListSet<E> view(ConcurrentNavigableMap<E, Object> part) {
        return new ConcurrentSkipListSet<>(part, addable);
    }

    @Override
    public ConcurrentSkipListSet<E> clone() {
        ConcurrentSkipListSet<E> copy = new ConcurrentSkipListSet<>(comparator());
        copy.addAll(this);
        return copy;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object item) {
        return map.containsKey(item);
    }

    @Override
    public boolean add(E item) {
        if (!addable) {
            throw new UnsupportedOperationException("a map's key set cannot add");
        }
        return map.putIfAbsent(item, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object item) {
        return map.remove(item) != null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return new ConcurrentSkipListMap.Walk<>(keys(), map::remove);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private List<E> keys() {
        return ((ConcurrentSkipListMap<E, Object>) map).keys();
    }

    @Override
    public Object[] toArray() {
        return keys().toArray();
    }

    @Override
    public <T> T[] toArray(T[] into) {
        return keys().toArray(into);
    }

    @Override
    public Comparator<? super E> comparator() {
        return map.comparator();
    }

    @Override
    public E first() {
        return map.firstKey();
    }

    @Override
    public E last() {
        return map.lastKey();
    }

    @Override
    public E lower(E item) {
        return map.lowerKey(item);
    }

    @Override
    public E floor(E item) {
        return map.floorKey(item);
    }

    @Override
    public E ceiling(E item) {
        return map.ceilingKey(item);
    }

    @Override
    public E higher(E item) {
        return map.higherKey(item);
    }

    @Override
    public E pollFirst() {
        Map.Entry<E, Object> entry = map.pollFirstEntry();
        return entry == null ? null : entry.getKey();
    }

    @Override
    public E pollLast() {
        Map.Entry<E, Object> entry = map.pollLastEntry();
        return entry == null ? null : entry.getKey();
    }

    @Override
    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        return view(map.subMap(from, fromInclusive, to, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(E to, boolean inclusive) {
        return view(map.headMap(to, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        return view(map.tailMap(from, inclusive));
    }

    @Override
    public NavigableSet<E> subSet(E from, E to) {
        return subSet(from, true, to, false);
    }

    @Override
    public NavigableSet<E> headSet(E to) {
        return headSet(to, false);
    }

    @Override
    public NavigableSet<E> tailSet(E from) {
        return tailSet(from, true);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return view(map.descendingMap());
    }

    @Override
    public boolean removeAll(Collection<?> items) {
        boolean changed = false;
        for (Object item : items) {
            changed |= remove(item);
        }
        return changed;
    }
}
