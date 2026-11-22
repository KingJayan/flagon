package dev.flagon.platform;

/**
 * the widening conversions reflection applies when a primitive is read or written by type.
 */
final class Primitives {

    private Primitives() {
    }

    /** the value, a boxed primitive, widened to the primitive type, or null when it cannot be */
    static Object widen(Object value, Class<?> to) {
        if (value == null) {
            return null;
        }
        if (to == boolean.class) {
            return value instanceof Boolean ? value : null;
        }
        long whole;
        if (value instanceof Character character) {
            whole = character;
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long) {
            whole = ((Number) value).longValue();
        } else if (value instanceof Float single) {
            return to == float.class ? single : to == double.class ? (Object) single.doubleValue()
                    : null;
        } else if (value instanceof Double precise) {
            return to == double.class ? precise : null;
        } else {
            return null;
        }
        int rank = rank(value);
        if (to == char.class) {
            return value instanceof Character ? value : null;
        }
        if (value instanceof Character && rank(to) < rank(Integer.valueOf(0))) {
            return null;
        }
        if (rank(to) < rank) {
            return null;
        }
        if (to == byte.class) {
            return (byte) whole;
        } else if (to == short.class) {
            return (short) whole;
        } else if (to == int.class) {
            return (int) whole;
        } else if (to == long.class) {
            return whole;
        } else if (to == float.class) {
            return (float) whole;
        } else if (to == double.class) {
            return (double) whole;
        }
        return null;
    }

    static Object widenOrThrow(Object value, Class<?> to, String what) {
        Object widened = widen(value, to);
        if (widened == null) {
            throw new IllegalArgumentException(what + " cannot be read as " + to.getName());
        }
        return widened;
    }

    private static int rank(Object value) {
        if (value instanceof Byte) {
            return 1;
        } else if (value instanceof Short || value instanceof Character) {
            return 2;
        } else if (value instanceof Integer) {
            return 3;
        } else if (value instanceof Long) {
            return 4;
        }
        return 0;
    }

    private static int rank(Class<?> type) {
        if (type == byte.class) {
            return 1;
        } else if (type == short.class) {
            return 2;
        } else if (type == int.class) {
            return 3;
        } else if (type == long.class) {
            return 4;
        } else if (type == float.class) {
            return 5;
        } else if (type == double.class) {
            return 6;
        }
        return 0;
    }
}
