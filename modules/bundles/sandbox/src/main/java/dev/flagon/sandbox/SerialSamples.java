package dev.flagon.sandbox;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * an object graph the jvm and the browser must write to the same bytes. serial.bin holds what
 * the jvm writes; SerialSamplesTest checks it is still what the jvm writes.
 */
public final class SerialSamples {

    private SerialSamples() {
    }

    public record Point(int x, String label) implements Serializable {
    }

    public enum Mood {
        CALM,
        LOUD {
            @Override
            public String toString() {
                return "loud";
            }
        }
    }

    /** not serializable, so deserialization runs its constructor */
    public static class Base {
        int baseCount;

        Base() {
            baseCount = 42;
        }
    }

    public static class Custom implements Serializable {
        private static final long serialVersionUID = 3L;
        String name;
        transient int computed;

        Custom(String name) {
            this.name = name;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(name.length());
            out.writeUTF("tail");
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            computed = in.readInt();
            if (!in.readUTF().equals("tail")) {
                throw new IOException("lost the tail");
            }
        }
    }

    public static class External implements Externalizable {
        int value;

        public External() {
        }

        External(int value) {
            this.value = value;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(value);
            out.writeObject("ext");
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            value = in.readInt();
            in.readObject();
        }
    }

    public static class Replaced implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;

        Replaced(int id) {
            this.id = id;
        }

        private Object writeReplace() {
            return new Stand(id);
        }
    }

    public static class Stand implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;

        Stand(int id) {
            this.id = id;
        }

        private Object readResolve() {
            return new Replaced(id);
        }
    }

    public static class Tagged extends ArrayList<String> {
        private static final long serialVersionUID = 5L;
        String tag;
    }

    public static class Holder extends Base implements Serializable {
        private static final long serialVersionUID = 7L;
        int count;
        long big;
        double ratio;
        boolean flag;
        char letter;
        byte tiny;
        short small;
        float part;
        String text;
        transient String skipped;
        int[] numbers;
        String[] words;
        Object[] mixed;
        Point point;
        Point again;
        Mood mood;
        Mood loud;
        Holder self;
        List<String> list;
        LinkedList<Integer> linked;
        Map<String, Integer> ordered;
        TreeMap<String, String> sorted;
        LinkedHashSet<String> unique;
        TreeSet<Integer> tree;
        ArrayDeque<String> deque;
        List<Integer> fixed;
        List<String> empty;
        List<String> single;
        List<String> readOnly;
        Integer boxed;
        Long wide;
        UUID id;
        Date when;
        Custom custom;
        External external;
        Replaced replaced;
        Tagged tagged;
        Class<?> type;
    }

    /** the graph, made the same way each time */
    public static Holder graph() {
        Holder holder = new Holder();
        holder.count = 3;
        holder.big = 1L << 40;
        holder.ratio = 0.5;
        holder.flag = true;
        holder.letter = 'é';
        holder.tiny = -2;
        holder.small = 300;
        holder.part = 1.25f;
        holder.text = "héllo, wörld ☃";
        holder.skipped = "not written";
        holder.numbers = new int[] {1, 2, 3};
        holder.words = new String[] {"a", null, "c"};
        holder.point = new Point(4, "p");
        holder.mixed = new Object[] {1, "two", holder.point};
        holder.again = holder.point;
        holder.mood = Mood.CALM;
        holder.loud = Mood.LOUD;
        holder.self = holder;
        holder.list = new ArrayList<>(List.of("x", "y"));
        holder.linked = new LinkedList<>(List.of(7, 8));
        holder.ordered = new LinkedHashMap<>();
        holder.ordered.put("one", 1);
        holder.ordered.put("two", 2);
        holder.sorted = new TreeMap<>();
        holder.sorted.put("b", "2");
        holder.sorted.put("a", "1");
        holder.unique = new LinkedHashSet<>(List.of("u", "v"));
        holder.tree = new TreeSet<>(List.of(5, 1, 3));
        holder.deque = new ArrayDeque<>(List.of("d"));
        holder.fixed = List.of(1, 2, 3);
        holder.empty = Collections.emptyList();
        holder.single = Collections.singletonList("only");
        holder.readOnly = Collections.unmodifiableList(new ArrayList<>(List.of("r")));
        holder.boxed = 9;
        holder.wide = 10L;
        holder.id = new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
        holder.when = new Date(1_700_000_000_000L);
        holder.custom = new Custom("custom");
        holder.external = new External(11);
        holder.replaced = new Replaced(12);
        holder.tagged = new Tagged();
        holder.tagged.add("t");
        holder.tagged.tag = "tag";
        holder.type = Point.class;
        return holder;
    }

    /** what differs between a graph read back and one made fresh, or an empty list */
    public static List<String> differences(Holder read) {
        Holder made = graph();
        List<String> found = new ArrayList<>();
        check(found, "baseCount", 42, read.baseCount);
        check(found, "count", made.count, read.count);
        check(found, "big", made.big, read.big);
        check(found, "ratio", made.ratio, read.ratio);
        check(found, "flag", made.flag, read.flag);
        check(found, "letter", made.letter, read.letter);
        check(found, "tiny", made.tiny, read.tiny);
        check(found, "small", made.small, read.small);
        check(found, "part", made.part, read.part);
        check(found, "text", made.text, read.text);
        check(found, "skipped", null, read.skipped);
        check(found, "numbers", java.util.Arrays.toString(made.numbers),
                java.util.Arrays.toString(read.numbers));
        check(found, "words", java.util.Arrays.toString(made.words),
                java.util.Arrays.toString(read.words));
        check(found, "mixed", java.util.Arrays.toString(made.mixed),
                java.util.Arrays.toString(read.mixed));
        check(found, "point", made.point, read.point);
        check(found, "again is point", true, read.again == read.point);
        check(found, "mixed shares point", true, read.mixed[2] == read.point);
        check(found, "mood", Mood.CALM, read.mood);
        check(found, "loud", Mood.LOUD, read.loud);
        check(found, "self", true, read.self == read);
        check(found, "list", made.list, read.list);
        check(found, "linked", made.linked, read.linked);
        check(found, "ordered", made.ordered, read.ordered);
        check(found, "ordered order", made.ordered.keySet().toString(),
                read.ordered.keySet().toString());
        check(found, "sorted", made.sorted, read.sorted);
        check(found, "unique", made.unique, read.unique);
        check(found, "tree", made.tree, read.tree);
        check(found, "deque", List.copyOf(made.deque), List.copyOf(read.deque));
        check(found, "fixed", made.fixed, read.fixed);
        check(found, "empty", made.empty, read.empty);
        check(found, "single", made.single, read.single);
        check(found, "readOnly", made.readOnly, read.readOnly);
        check(found, "boxed", made.boxed, read.boxed);
        check(found, "wide", made.wide, read.wide);
        check(found, "id", made.id, read.id);
        check(found, "when", made.when, read.when);
        check(found, "custom name", "custom", read.custom.name);
        check(found, "custom computed", 6, read.custom.computed);
        check(found, "external", 11, read.external.value);
        check(found, "replaced", 12, read.replaced.id);
        check(found, "tagged", List.of("t"), read.tagged);
        check(found, "tagged tag", "tag", read.tagged.tag);
        check(found, "type", Point.class, read.type);
        return found;
    }

    private static void check(List<String> found, String name, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            found.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
