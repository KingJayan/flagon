package dev.flagon.platform;

import java.lang.reflect.Array;

/**
 * the {@link Array} calls the browser classlib leaves out, which the build sends here.
 */
public final class Arrays {

    private Arrays() {
    }

    /** the one-level form, which the classlib does have, so this only hands it over */
    public static Object newInstance(Class<?> type, int length) {
        return Array.newInstance(type, length);
    }

    /**
     * an array of arrays, built one level at a time, because the classlib makes only one level
     * at a time.
     *
     * @param lengths how long the array is at each level, outermost first
     */
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

    public static boolean getBoolean(Object array, int index) {
        return (Boolean) read(array, index, boolean.class);
    }

    public static byte getByte(Object array, int index) {
        return (Byte) read(array, index, byte.class);
    }

    public static char getChar(Object array, int index) {
        return (Character) read(array, index, char.class);
    }

    public static short getShort(Object array, int index) {
        return (Short) read(array, index, short.class);
    }

    public static int getInt(Object array, int index) {
        return (Integer) read(array, index, int.class);
    }

    public static long getLong(Object array, int index) {
        return (Long) read(array, index, long.class);
    }

    public static float getFloat(Object array, int index) {
        return (Float) read(array, index, float.class);
    }

    public static double getDouble(Object array, int index) {
        return (Double) read(array, index, double.class);
    }

    public static void setBoolean(Object array, int index, boolean value) {
        write(array, index, value);
    }

    public static void setByte(Object array, int index, byte value) {
        write(array, index, value);
    }

    public static void setChar(Object array, int index, char value) {
        write(array, index, value);
    }

    public static void setShort(Object array, int index, short value) {
        write(array, index, value);
    }

    public static void setInt(Object array, int index, int value) {
        write(array, index, value);
    }

    public static void setLong(Object array, int index, long value) {
        write(array, index, value);
    }

    public static void setFloat(Object array, int index, float value) {
        write(array, index, value);
    }

    public static void setDouble(Object array, int index, double value) {
        write(array, index, value);
    }

    private static Object read(Object array, int index, Class<?> as) {
        Class<?> type = array.getClass().getComponentType();
        if (type == null || !type.isPrimitive()) {
            throw new IllegalArgumentException("not an array of primitives");
        }
        return Primitives.widenOrThrow(Array.get(array, index), as, "the element");
    }

    private static void write(Object array, int index, Object value) {
        Class<?> type = array.getClass().getComponentType();
        if (type == null || !type.isPrimitive()) {
            throw new IllegalArgumentException("not an array of primitives");
        }
        Array.set(array, index, Primitives.widenOrThrow(value, type, "the value"));
    }
}
