package dev.flagon.platform;

import java.lang.reflect.Array;

/**
 * the {@link Array} calls the browser classlib leaves out, which the build sends here.
 */
public final class Arrays {

    private Arrays() {
    }

    /**
     * an array of arrays, built one level at a time, because the classlib makes only one level
     * at a time.
     *
     * @param lengths how long the array is at each level, outermost first
     */
    /** the one-level form, which the classlib does have, so this only hands it over */
    public static Object newInstance(Class<?> type, int length) {
        return Array.newInstance(type, length);
    }

    public static Object newInstance(Class<?> type, int... lengths) {
        if (lengths.length == 0) {
            throw new IllegalArgumentException("an array needs at least one length");
        }
        if (lengths.length == 1) {
            return Array.newInstance(type, lengths[0]);
        }
        int[] rest = new int[lengths.length - 1];
        System.arraycopy(lengths, 1, rest, 0, rest.length);
        Object first = newInstance(type, rest);
        Object out = Array.newInstance(first.getClass(), lengths[0]);
        Array.set(out, 0, first);
        for (int at = 1; at < lengths[0]; at++) {
            Array.set(out, at, newInstance(type, rest));
        }
        return out;
    }
}
