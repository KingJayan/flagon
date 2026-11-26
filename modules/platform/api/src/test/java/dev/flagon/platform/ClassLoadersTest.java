package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassLoadersTest {

    private final ClassLoader loader = ClassLoadersTest.class.getClassLoader();

    @Test
    void findsAClassTheBuildKept() throws Exception {
        String name = ClassLoaders.class.getName();

        assertEquals(ClassLoaders.class, ClassLoaders.loadClass(loader, name));
    }

    @Test
    void saysWhyAClassNobodyCompiledIsNotThere() {
        ClassNotFoundException thrown = assertThrows(ClassNotFoundException.class,
            () -> ClassLoaders.loadClass(loader, "com.example.NeverCompiled"));

        assertTrue(thrown.getMessage().contains("nothing is loaded at runtime"));
        assertTrue(thrown.getMessage().contains("flagon.reflect.classes"));
    }

    @Test
    void findsNoCopiesOfAResource() {
        assertFalse(ClassLoaders.getResources(loader, "META-INF/services/anything")
                .hasMoreElements());
    }
}
