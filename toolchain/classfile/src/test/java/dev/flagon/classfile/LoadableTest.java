package dev.flagon.classfile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadableTest {

    private record Hiding(Set<String> hidden) implements Classes {

        @Override
        public byte[] read(String name) {
            if (hidden.contains(name)) {
                return null;
            }
            try (InputStream in = getClass().getClassLoader()
                    .getResourceAsStream(name.replace('.', '/') + ".class")) {
                return in == null ? null : in.readAllBytes();
            } catch (IOException unreadable) {
                return null;
            }
        }

        @Override
        public boolean has(String name) {
            return !hidden.contains(name)
                    && (read(name) != null || name.startsWith("java."));
        }
    }

    @Test
    void acceptsAClassTheClasspathCanRun() {
        assertTrue(new Loadable(new Hiding(Set.of())).test(Loadable.class.getName()));
    }

    @Test
    void refusesAClassThatNamesAMissingClass() {
        Loadable loadable = new Loadable(new Hiding(Set.of(Bytes.class.getName())));
        assertFalse(loadable.test(Loadable.class.getName()));
    }

    @Test
    void refusesAClassNothingCarries() {
        assertFalse(new Loadable(new Hiding(Set.of())).test("nothing.At.All"));
    }

    @Test
    void refusesAClassThatCallsAMissingMethod() {
        Hiding classpath = new Hiding(Set.of());
        Loadable loadable = new Loadable(new Classes() {
            @Override
            public byte[] read(String name) {
                return Bytes.class.getName().equals(name)
                        ? classpath.read(LoadableTest.class.getName())
                        : classpath.read(name);
            }

            @Override
            public boolean has(String name) {
                return classpath.has(name);
            }
        });
        assertFalse(loadable.test(Loadable.class.getName()));
    }
}
