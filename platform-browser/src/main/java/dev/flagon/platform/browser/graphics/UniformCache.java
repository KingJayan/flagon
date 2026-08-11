package dev.flagon.platform.browser.graphics;

/**
 * remembers what each uniform location was last given, so setting it again costs nothing
 */
final class UniformCache {

    private int[][] values = new int[64][];
    private boolean[] known = new boolean[64];

    boolean changed(int location, int a) {
        int[] held = slot(location, 1);
        if (known[location] && held[0] == a) {
            return false;
        }
        held[0] = a;
        known[location] = true;
        return true;
    }

    boolean changed(int location, int a, int b) {
        int[] held = slot(location, 2);
        if (known[location] && held[0] == a && held[1] == b) {
            return false;
        }
        held[0] = a;
        held[1] = b;
        known[location] = true;
        return true;
    }

    boolean changed(int location, int a, int b, int c) {
        int[] held = slot(location, 3);
        if (known[location] && held[0] == a && held[1] == b && held[2] == c) {
            return false;
        }
        held[0] = a;
        held[1] = b;
        held[2] = c;
        known[location] = true;
        return true;
    }

    boolean changed(int location, int a, int b, int c, int d) {
        int[] held = slot(location, 4);
        if (known[location] && held[0] == a && held[1] == b && held[2] == c && held[3] == d) {
            return false;
        }
        held[0] = a;
        held[1] = b;
        held[2] = c;
        held[3] = d;
        known[location] = true;
        return true;
    }

    /** the vector and matrix form: the first {@code length} floats of {@code source}. */
    boolean changed(int location, float[] source, int length) {
        int[] held = slot(location, length);
        if (known[location]) {
            int at = 0;
            while (at < length && held[at] == Float.floatToRawIntBits(source[at])) {
                at++;
            }
            if (at == length) {
                return false;
            }
        }
        for (int at = 0; at < length; at++) {
            held[at] = Float.floatToRawIntBits(source[at]);
        }
        known[location] = true;
        return true;
    }

    /** the same for a run of ints. */
    boolean changed(int location, int[] source, int length) {
        int[] held = slot(location, length);
        if (known[location]) {
            int at = 0;
            while (at < length && held[at] == source[at]) {
                at++;
            }
            if (at == length) {
                return false;
            }
        }
        System.arraycopy(source, 0, held, 0, length);
        known[location] = true;
        return true;
    }

    /** the location stands for something else now, or for nothing. */
    void forget(int location) {
        if (location >= 0 && location < known.length) {
            known[location] = false;
        }
    }

    private int[] slot(int location, int size) {
        if (location >= values.length) {
            int grown = Math.max(location + 1, values.length * 2);
            int[][] wider = new int[grown][];
            System.arraycopy(values, 0, wider, 0, values.length);
            values = wider;
            boolean[] widerKnown = new boolean[grown];
            System.arraycopy(known, 0, widerKnown, 0, known.length);
            known = widerKnown;
        }
        int[] held = values[location];
        if (held == null || held.length != size) {
            held = new int[size];
            values[location] = held;
            known[location] = false;
        }
        return held;
    }
}