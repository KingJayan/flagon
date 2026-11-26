package dev.flagon.shims;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFactsTest {

    static class Plain implements Serializable, Comparable<Plain> {
        static final String NAME = "plain";
        private static int counter;
        int count;
        protected transient String label;
        private final long id = 1;
        volatile double[] values;

        static {
            counter = 3;
        }

        Plain() {
        }

        Plain(int count) {
            this.count = count;
        }

        private Plain(String label) {
            this.label = label;
        }

        @Override
        public int compareTo(Plain other) {
            return Integer.compare(count, other.count);
        }

        synchronized void touch(List<String> into) {
            into.add(label + id + counter);
        }

        private void hidden() {
        }
    }

    private static final class Hidden implements Serializable {
        String text;
    }

    public abstract static class Base implements Serializable {
        public abstract void run();
    }

    public interface Contract extends Serializable {
        void apply();
    }

    public interface Marker extends Serializable {
    }

    record Pair<A>(A left, int right) implements Serializable {
    }

    sealed interface Shape permits Round, Square {
    }

    record Round() implements Shape {
    }

    record Square() implements Shape {
    }

    static class Box<T extends Comparable<T>> extends ArrayList<T>
            implements Supplier<Map<String, T[]>> {
        @Override
        public Map<String, T[]> get() {
            return null;
        }
    }

    @interface Tagged {
        int weight() default 7;

        String[] names() default {"a", "b c"};

        Class<?> kind() default String.class;

        TimeUnit unit() default TimeUnit.SECONDS;
    }

    @Test
    void computesTheJdksDefaultSerialVersionUid() throws IOException {
        for (Class<?> type : List.of(Plain.class, Hidden.class, Base.class, Contract.class,
                Marker.class)) {
            assertEquals(ObjectStreamClass.lookupAny(type).getSerialVersionUID(),
                    facts(type).defaultSerialId(), type.getName());
        }
    }

    @Test
    void writesWhatTeavmDropsForAReflectedClass() throws IOException {
        Set<String> classes = new LinkedHashSet<>();
        String box = ReflectionLines.write(facts(Box.class), true, false, classes);
        assertTrue(box.contains("S <T::Ljava/lang/Comparable<TT;>;>Ljava/util/ArrayList<TT;>;"),
                box);
        assertTrue(classes.containsAll(List.of("java/lang/Comparable", "java/util/ArrayList",
                "java/util/function/Supplier", "java/util/Map", "java/lang/String")),
                classes::toString);

        String pair = ReflectionLines.write(facts(Pair.class), true, true, new LinkedHashSet<>());
        assertTrue(pair.contains("R left Ljava/lang/Object; TA;"), pair);
        assertTrue(pair.contains("R right I -"), pair);
        assertTrue(!pair.contains("U "), "a record's serialVersionUID is 0");

        String shape = ReflectionLines.write(facts(Shape.class), true, false,
                new LinkedHashSet<>());
        assertTrue(shape.contains("P dev/flagon/shims/ClassFactsTest$Round"), shape);

        String tagged = ReflectionLines.write(facts(Tagged.class), true, false, classes);
        assertTrue(tagged.contains("D weight()I i7"), tagged);
        assertTrue(tagged.contains("D names()[Ljava/lang/String; [sa,sb%0020c]"), tagged);
        assertTrue(tagged.contains("D kind()Ljava/lang/Class; tLjava/lang/String;"), tagged);
        assertTrue(tagged.contains("D unit()Ljava/util/concurrent/TimeUnit; "
                + "eLjava/util/concurrent/TimeUnit;.SECONDS"), tagged);
        assertTrue(classes.contains("java/util/concurrent/TimeUnit"));

        class Inside {
        }
        String inside = ReflectionLines.write(facts(Inside.class), true, false,
                new LinkedHashSet<>());
        assertTrue(inside.contains("E dev/flagon/shims/ClassFactsTest "
                + "writesWhatTeavmDropsForAReflectedClass ()V"), inside);
    }

    @Test
    void readsClassNamesOutOfSignatures() {
        Set<String> names = new LinkedHashSet<>();
        new ReflectionLines.Names("<Left:Ljava/lang/Object;>Lpkg/Outer<TLeft;>.Inner<[TLeft;>;",
                names).classSignature();
        assertEquals(Set.of("java/lang/Object", "pkg/Outer$Inner"), names);
    }

    private static ClassFacts facts(Class<?> type) throws IOException {
        try (InputStream in = type.getResourceAsStream("/" + type.getName().replace('.', '/')
                + ".class")) {
            return ClassFacts.read(in.readAllBytes());
        }
    }
}
