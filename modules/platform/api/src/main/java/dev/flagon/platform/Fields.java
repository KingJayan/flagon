package dev.flagon.platform;

import java.lang.reflect.Field;

/**
 * the typed {@link Field} reads and writes the browser classlib leaves out, which the build
 * sends here. they widen the way the jdk does.
 */
public final class Fields {

    private Fields() {
    }

    public static boolean getBoolean(Field field, Object holder) throws IllegalAccessException {
        return (Boolean) read(field, holder, boolean.class);
    }

    public static byte getByte(Field field, Object holder) throws IllegalAccessException {
        return (Byte) read(field, holder, byte.class);
    }

    public static char getChar(Field field, Object holder) throws IllegalAccessException {
        return (Character) read(field, holder, char.class);
    }

    public static short getShort(Field field, Object holder) throws IllegalAccessException {
        return (Short) read(field, holder, short.class);
    }

    public static int getInt(Field field, Object holder) throws IllegalAccessException {
        return (Integer) read(field, holder, int.class);
    }

    public static long getLong(Field field, Object holder) throws IllegalAccessException {
        return (Long) read(field, holder, long.class);
    }

    public static float getFloat(Field field, Object holder) throws IllegalAccessException {
        return (Float) read(field, holder, float.class);
    }

    public static double getDouble(Field field, Object holder) throws IllegalAccessException {
        return (Double) read(field, holder, double.class);
    }

    public static void setBoolean(Field field, Object holder, boolean value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setByte(Field field, Object holder, byte value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setChar(Field field, Object holder, char value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setShort(Field field, Object holder, short value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setInt(Field field, Object holder, int value) throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setLong(Field field, Object holder, long value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setFloat(Field field, Object holder, float value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    public static void setDouble(Field field, Object holder, double value)
            throws IllegalAccessException {
        write(field, holder, value);
    }

    private static Object read(Field field, Object holder, Class<?> as)
            throws IllegalAccessException {
        if (!field.getType().isPrimitive()) {
            throw new IllegalArgumentException(field.getName() + " is not a primitive field");
        }
        return Primitives.widenOrThrow(field.get(holder), as, field.getName());
    }

    private static void write(Field field, Object holder, Object value)
            throws IllegalAccessException {
        Class<?> type = field.getType();
        if (!type.isPrimitive()) {
            throw new IllegalArgumentException(field.getName() + " is not a primitive field");
        }
        field.set(holder, Primitives.widenOrThrow(value, type, field.getName()));
    }
}
