package dev.flagon.platform;

import java.util.SortedMap;
import java.util.TreeMap;

public final class Maps {

    private Maps() {
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, V> map) {
        SortedMap<K, V> copy = new TreeMap<>(map.comparator());
        copy.putAll(map);
        return copy;
    }
}
