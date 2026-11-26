package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionExtrasTest {

    /** what the build writes for a class, set by hand to the jvm's own signature */
    static class Box<T extends Comparable<T>> extends ArrayList<T>
            implements Supplier<Map<String, T[]>> {

        public static Object[] $flagon$reflection() {
            return new Object[] {
                "S <T::Ljava/lang/Comparable<TT;>;>Ljava/util/ArrayList<TT;>;"
                    + "Ljava/util/function/Supplier<Ljava/util/Map<Ljava/lang/String;[TT;>;>;\n"
                    + "P java/lang/String",
                Comparable.class, ArrayList.class, Supplier.class, Map.class, String.class};
        }

        @Override
        public Map<String, T[]> get() {
            return null;
        }
    }

    static class Holder {
        int count = 5;
        char letter = 'x';
        double scale;
        String text = "t";
    }

    @Test
    void readsGenericSupertypesTheWayTheJvmDoes() {
        assertEquals(Box.class.getGenericSuperclass().getTypeName(),
                Classes.getGenericSuperclass(Box.class).getTypeName());
        assertEquals(Box.class.getGenericInterfaces()[0].getTypeName(),
                Classes.getGenericInterfaces(Box.class)[0].getTypeName());

        TypeVariable<?> variable = Classes.getTypeParameters(Box.class)[0];
        assertEquals("T", variable.getName());
        assertEquals("java.lang.Comparable<T>", variable.getBounds()[0].getTypeName());
        Type argument = ((java.lang.reflect.ParameterizedType)
                Classes.getGenericSuperclass(Box.class)).getActualTypeArguments()[0];
        assertEquals(variable, argument);
    }

    @Test
    void hidesTheMethodThatCarriesTheData() {
        for (Method method : Classes.getDeclaredMethods(Box.class)) {
            assertTrue(!method.getName().equals(ClassData.METHOD));
        }
    }

    @Test
    void readsPermittedSubclassesOnlyWhenWritten() {
        assertArrayEquals(new Class<?>[] {String.class}, Classes.getPermittedSubclasses(Box.class));
        assertNull(Classes.getPermittedSubclasses(Holder.class));
    }

    @Test
    void readsFieldsWithTheJdksWidening() throws ReflectiveOperationException {
        Holder holder = new Holder();
        Field count = Holder.class.getDeclaredField("count");
        Field letter = Holder.class.getDeclaredField("letter");
        Field scale = Holder.class.getDeclaredField("scale");
        assertEquals(5L, Fields.getLong(count, holder));
        assertEquals(5.0, Fields.getDouble(count, holder));
        assertEquals('x', Fields.getInt(letter, holder));
        assertThrows(IllegalArgumentException.class, () -> Fields.getShort(letter, holder));
        assertThrows(IllegalArgumentException.class, () -> Fields.getByte(count, holder));
        assertThrows(IllegalArgumentException.class,
                () -> Fields.getInt(Holder.class.getDeclaredField("text"), holder));

        Fields.setByte(count, holder, (byte) 3);
        assertEquals(3, holder.count);
        Fields.setFloat(scale, holder, 1.5f);
        assertEquals(1.5, holder.scale);
        assertThrows(IllegalArgumentException.class, () -> Fields.setLong(count, holder, 1L));
    }

    @Test
    void readsArraysWithTheJdksWidening() {
        int[] numbers = {4, 5};
        Arrays.setShort(numbers, 1, (short) 8);
        assertEquals(8, numbers[1]);
        assertEquals(4L, Arrays.getLong(numbers, 0));
        assertThrows(IllegalArgumentException.class, () -> Arrays.getChar(numbers, 0));
        assertThrows(IllegalArgumentException.class, () -> Arrays.getInt(new Object[] {1}, 0));
        assertThrows(IllegalArgumentException.class, () -> Arrays.setLong(new int[1], 0, 1L));
        long[] wide = new long[1];
        Arrays.setInt(wide, 0, 7);
        assertEquals(7L, wide[0]);
    }
}
