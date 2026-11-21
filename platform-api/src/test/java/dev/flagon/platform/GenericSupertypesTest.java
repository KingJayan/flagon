package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** the path gson takes from a List<X> to its Collection<E>, to find what X is */
class GenericSupertypesTest {

    @Test
    void aCollectionHandsItsElementUp() {
        TypeVariable<?>[] element = Classes.getTypeParameters(ArrayList.class);
        ParameterizedType list = (ParameterizedType) Arrays.stream(
                        Classes.getGenericInterfaces(ArrayList.class))
                .filter(type -> type instanceof ParameterizedType parameterized
                        && parameterized.getRawType() == List.class)
                .findFirst().orElseThrow();
        ParameterizedType above = (ParameterizedType) Classes.getGenericSuperclass(ArrayList.class);

        assertArrayEquals(element, list.getActualTypeArguments());
        assertSame(AbstractList.class, above.getRawType());
        assertEquals(ArrayList.class, element[0].getGenericDeclaration());
        assertEquals("java.util.AbstractList<E>", above.toString());
    }

    @Test
    void aMapHandsItsKeyAndValueUp() {
        ParameterizedType map = (ParameterizedType) Arrays.stream(
                        Classes.getGenericInterfaces(HashMap.class))
                .filter(type -> type instanceof ParameterizedType parameterized
                        && parameterized.getRawType() == Map.class)
                .findFirst().orElseThrow();

        assertArrayEquals(Classes.getTypeParameters(HashMap.class), map.getActualTypeArguments());
    }

    @Test
    void anythingElseKeepsItsRawSupertypes() {
        Type[] interfaces = Classes.getGenericInterfaces(String.class);

        assertArrayEquals(String.class.getInterfaces(), interfaces);
        assertSame(Object.class, Classes.getGenericSuperclass(String.class));
        assertArrayEquals(Thread.State.class.getTypeParameters(),
                Classes.getTypeParameters(Thread.State.class));
    }
}
