package dev.flagon.platform;

import java.lang.reflect.Field;

public final class Fields {

    private Fields() {
    }

    public static long getLong(Field field, Object holder) throws IllegalAccessException {
        return number(field, holder).longValue();
    }

    public static int getInt(Field field, Object holder) throws IllegalAccessException {
        return number(field, holder).intValue();
    }

    private static Number number(Field field, Object holder) throws IllegalAccessException {
        Object value = field.get(holder);
        if (value instanceof Number number) {
            return number;
        }
        throw new IllegalArgumentException(field.getName() + " holds no number");
    }
}
