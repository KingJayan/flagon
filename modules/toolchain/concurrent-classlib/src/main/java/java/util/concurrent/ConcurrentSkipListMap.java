package java.util.concurrent;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * a sorted map threads share. green threads switch only where code pauses and no method here
 * pauses, so each call is atomic without a lock; a lock would make every map call in the program
 * pausable. iterators walk a snapshot: they never throw ConcurrentModificationException, and
 * their remove goes to the map. views write through.
 */
public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentNavigableMap<K, V>, Cloneable {

    private final NavigableMap<K, V> range;

    public ConcurrentSkipListMap() {
        this(new TreeMap<>());
    }

    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this(new TreeMap<>(comparator));
    }

    public ConcurrentSkipListMap(Map<? extends K, ? extends V> source) {
        this();
        putAll(source);
    }

    public ConcurrentSkipListMap(SortedMap<K, ? extends V> source) {
        this(new TreeMap<>(source.comparator()));
        putAll(source);
    }

    private ConcurrentSkipListMap(NavigableMap<K, V> range) {
        this.range = range;
    }

    private ConcurrentSkipListMap<K, V> view(NavigableMap<K, V> part) {
        return new ConcurrentSkipListMap<>(part);
    }

    private static <K, V> Map.Entry<K, V> frozen(Map.Entry<K, V> entry) {
        return entry == null ? null : new AbstractMap.SimpleImmutableEntry<>(entry);
    }

    private List<Map.Entry<K, V>> snapshot() {
        List<Map.Entry<K, V>> entries = new ArrayList<>(range.size());
        for (Map.Entry<K, V> entry : range.entrySet()) {
            entries.add(frozen(entry));
        }
        return entries;
    }

    @Override
    public ConcurrentSkipListMap<K, V> clone() {
        ConcurrentSkipListMap<K, V> copy =
                new ConcurrentSkipListMap<>(new TreeMap<>(range.comparator()));
        copy.range.putAll(range);
        return copy;
    }

    @Override
    public int size() {
        return range.size();
    }

    @Override
    public boolean isEmpty() {
        return range.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return range.containsKey(nonNull(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return range.containsValue(nonNull(value));
    }

    @Override
    public V get(Object key) {
        return range.get(nonNull(key));
    }

    @Override
    public V getOrDefault(Object key, V fallback) {
        V found = get(key);
        return found == null ? fallback : found;
    }

    @Override
    public V put(K key, V value) {
        return range.put(nonNull(key), nonNull(value));
    }

    @Override
    public V remove(Object key) {
        return range.remove(nonNull(key));
    }

    @Override
    public void clear() {
        range.clear();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V found = range.get(nonNull(key));
        if (found == null) {
            range.put(key, nonNull(value));
        }
        return found;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (value != null && value.equals(range.get(nonNull(key)))) {
            range.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V expected, V value) {
        nonNull(value);
        if (nonNull(expected).equals(range.get(nonNull(key)))) {
            range.put(key, value);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        nonNull(value);
        return range.containsKey(nonNull(key)) ? range.put(key, value) : null;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mapping) {
        V found = range.get(nonNull(key));
        if (found != null) {
            return found;
        }
        V made = mapping.apply(key);
        if (made != null) {
            range.put(key, made);
        }
        return made;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
        V found = range.get(nonNull(key));
        return found == null ? null : settle(key, remapping.apply(key, found));
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remapping) {
        return settle(nonNull(key), remapping.apply(key, range.get(key)));
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remapping) {
        nonNull(value);
        V found = range.get(nonNull(key));
        return settle(key, found == null ? value : remapping.apply(found, value));
    }

    private V settle(K key, V value) {
        if (value == null) {
            range.remove(key);
        } else {
            range.put(key, value);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        for (Map.Entry<K, V> entry : snapshot()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        for (Map.Entry<K, V> entry : snapshot()) {
            if (range.containsKey(entry.getKey())) {
                range.put(entry.getKey(),
                        nonNull(function.apply(entry.getKey(), range.get(entry.getKey()))));
            }
        }
    }

    @Override
    public Comparator<? super K> comparator() {
        return range.comparator();
    }

    @Override
    public K firstKey() {
        return range.firstKey();
    }

    @Override
    public K lastKey() {
        return range.lastKey();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return frozen(range.lowerEntry(nonNull(key)));
    }

    @Override
    public K lowerKey(K key) {
        return range.lowerKey(nonNull(key));
    }

    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return frozen(range.floorEntry(nonNull(key)));
    }

    @Override
    public K floorKey(K key) {
        return range.floorKey(nonNull(key));
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return frozen(range.ceilingEntry(nonNull(key)));
    }

    @Override
    public K ceilingKey(K key) {
        return range.ceilingKey(nonNull(key));
    }

    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return frozen(range.higherEntry(nonNull(key)));
    }

    @Override
    public K higherKey(K key) {
        return range.higherKey(nonNull(key));
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return frozen(range.firstEntry());
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return frozen(range.lastEntry());
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        return frozen(range.pollFirstEntry());
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        return frozen(range.pollLastEntry());
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
                                               boolean toInclusive) {
        return view(range.subMap(nonNull(fromKey), fromInclusive, nonNull(toKey), toInclusive));
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return view(range.headMap(nonNull(toKey), inclusive));
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return view(range.tailMap(nonNull(fromKey), inclusive));
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        return view(range.descendingMap());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new ConcurrentSkipListSet<>(this, false);
    }

    @Override
    public NavigableSet<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                Iterator<Map.Entry<K, V>> entries = entrySet().iterator();
                return new Iterator<V>() {
                    @Override
                    public boolean hasNext() {
                        return entries.hasNext();
                    }

                    @Override
                    public V next() {
                        return entries.next().getValue();
                    }

                    @Override
                    public void remove() {
                        entries.remove();
                    }
                };
            }

            @Override
            public int size() {
                return ConcurrentSkipListMap.this.size();
            }

            @Override
            public boolean contains(Object value) {
                return containsValue(value);
            }

            @Override
            public void clear() {
                ConcurrentSkipListMap.this.clear();
            }
        };
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Walk<>(snapshot(),
                        entry -> ConcurrentSkipListMap.this.remove(entry.getKey()));
            }

            @Override
            public int size() {
                return ConcurrentSkipListMap.this.size();
            }

            @Override
            public boolean contains(Object item) {
                if (!(item instanceof Map.Entry<?, ?> entry) || entry.getKey() == null) {
                    return false;
                }
                V found = ConcurrentSkipListMap.this.get(entry.getKey());
                return found != null && found.equals(entry.getValue());
            }

            @Override
            public boolean remove(Object item) {
                return item instanceof Map.Entry<?, ?> entry && entry.getKey() != null
                        && ConcurrentSkipListMap.this.remove(entry.getKey(), entry.getValue());
            }

            @Override
            public void clear() {
                ConcurrentSkipListMap.this.clear();
            }
        };
    }

    /** the keys in order, as a snapshot, for the key set */
    List<K> keys() {
        return new ArrayList<>(range.keySet());
    }

    private static <T> T nonNull(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /** walks a snapshot, sending remove to the live collection */
    static final class Walk<T> implements Iterator<T> {

        interface Removal<T> {
            void remove(T item);
        }

        private final List<T> items;
        private final Removal<T> removal;
        private int next;
        private boolean removable;

        Walk(List<T> items, Removal<T> removal) {
            this.items = items;
            this.removal = removal;
        }

        @Override
        public boolean hasNext() {
            return next < items.size();
        }

        @Override
        public T next() {
            if (next >= items.size()) {
                throw new NoSuchElementException();
            }
            removable = true;
            return items.get(next++);
        }

        @Override
        public void remove() {
            if (!removable) {
                throw new IllegalStateException();
            }
            removable = false;
            removal.remove(items.get(next - 1));
        }
    }
}
