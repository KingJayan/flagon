package java.net.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.function.BiPredicate;

public final class HttpHeaders {

    private static final HttpHeaders NONE =
            new HttpHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

    /** lookups go to the sorted map itself: teavm's unmodifiable view drops its comparator */
    private final TreeMap<String, List<String>> sorted;
    private final Map<String, List<String>> map;

    private HttpHeaders(TreeMap<String, List<String>> sorted) {
        this.sorted = sorted;
        this.map = Collections.unmodifiableMap(sorted);
    }

    public static HttpHeaders of(Map<String, List<String>> headers,
                                 BiPredicate<String, String> filter) {
        if (headers == null || filter == null) {
            throw new NullPointerException();
        }
        TreeMap<String, List<String>> kept = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("empty key");
            }
            if (kept.containsKey(name)) {
                throw new IllegalArgumentException("duplicate key: " + name);
            }
            List<String> values = new ArrayList<>();
            for (String value : entry.getValue()) {
                String trimmed = value.trim();
                if (filter.test(name, trimmed)) {
                    values.add(trimmed);
                }
            }
            if (!values.isEmpty()) {
                kept.put(name, Collections.unmodifiableList(values));
            }
        }
        return kept.isEmpty() ? NONE : new HttpHeaders(kept);
    }

    public Optional<String> firstValue(String name) {
        List<String> values = allValues(name);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public OptionalLong firstValueAsLong(String name) {
        Optional<String> value = firstValue(name);
        return value.isPresent() ? OptionalLong.of(Long.parseLong(value.get()))
                : OptionalLong.empty();
    }

    public List<String> allValues(String name) {
        List<String> values = sorted.get(name);
        return values == null ? List.of() : values;
    }

    public Map<String, List<String>> map() {
        return map;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HttpHeaders that) || map.size() != that.map.size()) {
            return false;
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!entry.getValue().equals(that.sorted.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            hash += entry.getKey().toLowerCase(java.util.Locale.ROOT).hashCode()
                    ^ entry.getValue().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return super.toString() + " { " + map + " }";
    }
}
