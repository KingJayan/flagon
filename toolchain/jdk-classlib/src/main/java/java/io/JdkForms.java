package java.io;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * the jdk classes the browser classlib writes as the jvm does. their insides differ from the
 * jdk's, so each is written from its public state, in the form its jdk class would write.
 * {@link #TABLE} gives each form's descriptor; dev.flagon.shims tests check it against the jdk.
 */
final class JdkForms {

    /** name, serialVersionUID, flags, superclass, then fields as signature:name; \ joins rows */
    static final String TABLE = """
            java.lang.Number -8742448824652078965 02 -
            java.lang.Integer 1360826667806852920 02 java.lang.Number I:value
            java.lang.Long 4290774380558885855 02 java.lang.Number J:value
            java.lang.Short 7515723908773894738 02 java.lang.Number S:value
            java.lang.Byte -7183698231559129828 02 java.lang.Number B:value
            java.lang.Double -9172774392245257468 02 java.lang.Number D:value
            java.lang.Float -2671257302660747028 02 java.lang.Number F:value
            java.lang.Character 3786198910865385080 02 - C:value
            java.lang.Boolean -3665804199014368530 02 - Z:value
            java.util.ArrayList 8683452581122892189 03 - I:size
            java.util.LinkedList 876323262645176354 03 -
            java.util.ArrayDeque 2340985798034038923 03 -
            java.util.HashMap 362498820763181265 03 - F:loadFactor I:threshold
            java.util.LinkedHashMap 3801124242820219131 02 java.util.HashMap Z:accessOrder
            java.util.HashSet -5024744406713321676 03 -
            java.util.LinkedHashSet -2851667679971038690 02 java.util.HashSet
            java.util.TreeMap 919286545866124006 03 - Ljava/util/Comparator;:comparator
            java.util.TreeSet -2479143000061671589 03 -
            java.util.Date 7523967970034938905 03 -
            java.util.UUID -4856846361193249489 02 - J:leastSigBits J:mostSigBits
            java.util.CollSer 6309168927139932177 03 - I:tag
            java.util.Collections$EmptyList 8842843931221139166 02 -
            java.util.Collections$EmptySet 1582296315990362920 02 -
            java.util.Collections$EmptyMap 6428348081105594320 02 -
            java.util.Collections$SingletonList 3093736618740652951 02 - \
                Ljava/lang/Object;:element
            java.util.Collections$SingletonSet 3193687207550431679 02 - Ljava/lang/Object;:element
            java.util.Collections$SingletonMap -6979724477215052911 02 - Ljava/lang/Object;:k \
                Ljava/lang/Object;:v
            java.util.Collections$UnmodifiableCollection 1820017752578914078 02 - \
                Ljava/util/Collection;:c
            java.util.Collections$UnmodifiableList -283967356065247728 02 \
                java.util.Collections$UnmodifiableCollection Ljava/util/List;:list
            java.util.Collections$UnmodifiableRandomAccessList -2542308836966382001 02 \
                java.util.Collections$UnmodifiableList
            java.util.Collections$UnmodifiableSet -9215047833775013803 02 \
                java.util.Collections$UnmodifiableCollection
            java.util.Collections$UnmodifiableMap -1034234728574286014 02 - Ljava/util/Map;:m
            java.lang.StackTraceElement 6992337162326171013 02 - B:format I:lineNumber \
                Ljava/lang/String;:classLoaderName Ljava/lang/String;:declaringClass \
                Ljava/lang/String;:fileName Ljava/lang/String;:methodName \
                Ljava/lang/String;:moduleName Ljava/lang/String;:moduleVersion
            java.lang.Throwable -3042686055658047285 03 - Ljava/lang/Throwable;:cause \
                Ljava/lang/String;:detailMessage [Ljava/lang/StackTraceElement;:stackTrace \
                Ljava/util/List;:suppressedExceptions
            java.lang.Exception -3387516993124229948 02 java.lang.Throwable
            java.lang.Error 4980196508277280342 02 java.lang.Throwable
            java.lang.RuntimeException -7034897190745766939 02 java.lang.Exception
            java.lang.IllegalArgumentException -5365630128856068164 02 java.lang.RuntimeException
            java.lang.IllegalStateException -1848914673093119416 02 java.lang.RuntimeException
            java.lang.NullPointerException 5162710183389028792 02 java.lang.RuntimeException
            java.lang.UnsupportedOperationException -1242599979055084673 02 \
                java.lang.RuntimeException
            java.lang.IndexOutOfBoundsException 234122996006267687 02 java.lang.RuntimeException
            java.lang.ArithmeticException 2256477558314496007 02 java.lang.RuntimeException
            java.lang.ClassCastException -9223365651070458532 02 java.lang.RuntimeException
            java.lang.NumberFormatException -2848938806368998894 02 \
                java.lang.IllegalArgumentException
            java.lang.InterruptedException 6700697376100628473 02 java.lang.Exception
            java.util.NoSuchElementException 6769829250639411880 02 java.lang.RuntimeException
            java.util.ConcurrentModificationException -3666751008965953603 02 \
                java.lang.RuntimeException
            java.io.IOException 7818375828146090155 02 java.lang.Exception
            """;

    private static final int IMM_LIST = 1;
    private static final int IMM_SET = 2;
    private static final int IMM_MAP = 3;

    private static final Map<String, Level> BY_NAME = new HashMap<>();
    private static final Map<String, ObjectStreamClass> DESCRIBED = new HashMap<>();
    private static final Map<Class<?>, String> NAMES = new HashMap<>();
    private static final Map<String, Supplier<Throwable>> THROWABLES = new HashMap<>();

    static {
        for (String line : TABLE.split("\n")) {
            if (!line.isBlank()) {
                Level level = new Level(line.trim().split("\\s+"));
                BY_NAME.put(level.name, level);
            }
        }
        name(Integer.class, "java.lang.Integer");
        name(Long.class, "java.lang.Long");
        name(Short.class, "java.lang.Short");
        name(Byte.class, "java.lang.Byte");
        name(Double.class, "java.lang.Double");
        name(Float.class, "java.lang.Float");
        name(Character.class, "java.lang.Character");
        name(Boolean.class, "java.lang.Boolean");
        name(Number.class, "java.lang.Number");
        name(ArrayList.class, "java.util.ArrayList");
        name(LinkedList.class, "java.util.LinkedList");
        name(ArrayDeque.class, "java.util.ArrayDeque");
        name(HashMap.class, "java.util.HashMap");
        name(LinkedHashMap.class, "java.util.LinkedHashMap");
        name(HashSet.class, "java.util.HashSet");
        name(LinkedHashSet.class, "java.util.LinkedHashSet");
        name(TreeMap.class, "java.util.TreeMap");
        name(TreeSet.class, "java.util.TreeSet");
        name(Date.class, "java.util.Date");
        name(UUID.class, "java.util.UUID");
        name(StackTraceElement.class, "java.lang.StackTraceElement");
        name(Collections.emptyList().getClass(), "java.util.Collections$EmptyList");
        name(Collections.emptySet().getClass(), "java.util.Collections$EmptySet");
        name(Collections.emptyMap().getClass(), "java.util.Collections$EmptyMap");
        name(Collections.singletonList("").getClass(), "java.util.Collections$SingletonList");
        name(Collections.singleton("").getClass(), "java.util.Collections$SingletonSet");
        name(Collections.singletonMap("", "").getClass(), "java.util.Collections$SingletonMap");
        name(Collections.unmodifiableCollection(new ArrayList<>()).getClass(),
                "java.util.Collections$UnmodifiableCollection");
        name(Collections.unmodifiableList(new LinkedList<>()).getClass(),
                "java.util.Collections$UnmodifiableList");
        // the jdk writes the random access kind as the plain one, and reads it back by kind
        name(Collections.unmodifiableList(new ArrayList<>()).getClass(),
                "java.util.Collections$UnmodifiableList");
        name(Collections.unmodifiableSet(new HashSet<>()).getClass(),
                "java.util.Collections$UnmodifiableSet");
        name(Collections.unmodifiableMap(new HashMap<>()).getClass(),
                "java.util.Collections$UnmodifiableMap");
        for (Object immutable : new Object[] {List.of(1, 2), List.of(1, 2, 3), Set.of(1),
            Set.of(1, 2), Set.of(1, 2, 3), Map.of(1, 1), Map.of(1, 1, 2, 2),
            Map.of(1, 1, 2, 2, 3, 3)}) {
            name(immutable.getClass(), "java.util.CollSer");
        }
        throwable(Throwable.class, "java.lang.Throwable", Throwable::new);
        throwable(Exception.class, "java.lang.Exception", Exception::new);
        throwable(Error.class, "java.lang.Error", Error::new);
        throwable(RuntimeException.class, "java.lang.RuntimeException", RuntimeException::new);
        throwable(IllegalArgumentException.class, "java.lang.IllegalArgumentException",
                IllegalArgumentException::new);
        throwable(IllegalStateException.class, "java.lang.IllegalStateException",
                IllegalStateException::new);
        throwable(NullPointerException.class, "java.lang.NullPointerException",
                NullPointerException::new);
        throwable(UnsupportedOperationException.class,
                "java.lang.UnsupportedOperationException", UnsupportedOperationException::new);
        throwable(IndexOutOfBoundsException.class, "java.lang.IndexOutOfBoundsException",
                IndexOutOfBoundsException::new);
        throwable(ArithmeticException.class, "java.lang.ArithmeticException",
                ArithmeticException::new);
        throwable(ClassCastException.class, "java.lang.ClassCastException",
                ClassCastException::new);
        throwable(NumberFormatException.class, "java.lang.NumberFormatException",
                NumberFormatException::new);
        throwable(InterruptedException.class, "java.lang.InterruptedException",
                InterruptedException::new);
        throwable(NoSuchElementException.class, "java.util.NoSuchElementException",
                NoSuchElementException::new);
        throwable(ConcurrentModificationException.class,
                "java.util.ConcurrentModificationException", ConcurrentModificationException::new);
        throwable(IOException.class, "java.io.IOException", IOException::new);
    }

    private JdkForms() {
    }

    private static void name(Class<?> cl, String jdkName) {
        NAMES.putIfAbsent(cl, jdkName);
    }

    private static void throwable(Class<? extends Throwable> cl, String jdkName,
                                  Supplier<Throwable> make) {
        name(cl, jdkName);
        THROWABLES.put(jdkName, make);
    }

    /** the descriptor a class here is written with, or null when it has no jdk form */
    static ObjectStreamClass describe(Class<?> cl) {
        String jdkName = NAMES.get(cl);
        if (jdkName == null) {
            return null;
        }
        ObjectStreamClass desc = descriptor(jdkName);
        if (desc.cl == null) {
            desc.cl = cl;
        }
        return desc;
    }

    /** the class a jdk name reads as here, or null when the name is no jdk form */
    static Class<?> classOf(String jdkName) {
        if (jdkName.equals("java.lang.Enum")) {
            return Enum.class;
        }
        if (!BY_NAME.containsKey(jdkName)) {
            return null;
        }
        for (Map.Entry<Class<?>, String> named : NAMES.entrySet()) {
            if (named.getValue().equals(jdkName)) {
                return named.getKey();
            }
        }
        return Object.class;
    }

    static Level levelNamed(String jdkName) {
        return BY_NAME.get(jdkName);
    }

    static String whyNot(Class<?> cl) {
        String name = cl.getName();
        return name.startsWith("java.") || name.startsWith("javax.")
                ? ": this jdk class has no serial form in the browser classlib. see "
                        + "docs/COMPATIBILITY.md" : "";
    }

    private static ObjectStreamClass descriptor(String jdkName) {
        ObjectStreamClass desc = DESCRIBED.get(jdkName);
        if (desc != null) {
            return desc;
        }
        Level level = BY_NAME.get(jdkName);
        desc = new ObjectStreamClass();
        DESCRIBED.put(jdkName, desc);
        desc.name = level.name;
        desc.suid = level.suid;
        desc.flags = level.flags;
        desc.fields = level.fields;
        desc.form = level;
        desc.serializable = true;
        desc.superDesc = level.superName == null ? null : descriptor(level.superName);
        for (Map.Entry<Class<?>, String> named : NAMES.entrySet()) {
            if (named.getValue().equals(jdkName) && !jdkName.equals("java.util.CollSer")) {
                desc.cl = named.getKey();
            }
        }
        return desc;
    }

    static final class Level {

        final String name;
        final long suid;
        final byte flags;
        final String superName;
        final ObjectStreamField[] fields;

        Level(String[] parts) {
            name = parts[0];
            suid = Long.parseLong(parts[1]);
            flags = (byte) Integer.parseInt(parts[2], 16);
            superName = parts[3].equals("-") ? null : parts[3];
            List<ObjectStreamField> found = new ArrayList<>();
            for (int at = 4; at < parts.length; at++) {
                int colon = parts[at].lastIndexOf(':');
                found.add(new ObjectStreamField(parts[at].substring(colon + 1),
                        parts[at].substring(0, colon), false));
            }
            fields = found.toArray(new ObjectStreamField[0]);
            java.util.Arrays.sort(fields);
        }

        /** this level's data for the object, as its jdk class writes it */
        void write(ObjectOutputStream out, ObjectStreamClass level, Object obj) throws IOException {
            switch (name) {
                case "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
                     "java.lang.Double", "java.lang.Float", "java.lang.Character",
                     "java.lang.Boolean" -> out.writeValues(level, new Object[] {obj});
                case "java.util.ArrayList" -> {
                    Collection<?> items = (Collection<?>) obj;
                    out.writeValues(level, new Object[] {items.size()});
                    out.writeInt(items.size());
                    writeAll(out, items);
                }
                case "java.util.LinkedList", "java.util.ArrayDeque" -> {
                    Collection<?> items = (Collection<?>) obj;
                    out.writeInt(items.size());
                    writeAll(out, items);
                }
                case "java.util.HashMap" -> {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    int capacity = capacity(map.size());
                    out.writeValues(level, new Object[] {0.75f,
                        map.isEmpty() ? 0 : (int) (capacity * 0.75f)});
                    out.writeInt(capacity);
                    out.writeInt(map.size());
                    writeAll(out, ObjectOutputStream.entries(map));
                }
                case "java.util.LinkedHashMap" -> out.writeValues(level, new Object[] {false});
                case "java.util.HashSet" -> {
                    Set<?> set = (Set<?>) obj;
                    out.writeInt(capacity(set.size()));
                    out.writeFloat(0.75f);
                    out.writeInt(set.size());
                    writeAll(out, set);
                }
                case "java.util.TreeMap" -> {
                    TreeMap<?, ?> map = (TreeMap<?, ?>) obj;
                    out.writeValues(level, new Object[] {map.comparator()});
                    out.writeInt(map.size());
                    writeAll(out, ObjectOutputStream.entries(map));
                }
                case "java.util.TreeSet" -> {
                    TreeSet<?> set = (TreeSet<?>) obj;
                    out.writeObject(set.comparator());
                    out.writeInt(set.size());
                    writeAll(out, set);
                }
                case "java.util.Date" -> out.writeLong(((Date) obj).getTime());
                case "java.util.UUID" -> {
                    UUID id = (UUID) obj;
                    out.writeValues(level, new Object[] {id.getLeastSignificantBits(),
                        id.getMostSignificantBits()});
                }
                case "java.util.CollSer" -> writeImmutable(out, level, obj);
                case "java.util.Collections$SingletonList", "java.util.Collections$SingletonSet" ->
                    out.writeValues(level, new Object[] {((Collection<?>) obj).iterator().next()});
                case "java.util.Collections$SingletonMap" -> {
                    Map.Entry<?, ?> entry = ((Map<?, ?>) obj).entrySet().iterator().next();
                    out.writeValues(level, new Object[] {entry.getKey(), entry.getValue()});
                }
                case "java.util.Collections$UnmodifiableCollection",
                     "java.util.Collections$UnmodifiableList" ->
                    out.writeValues(level, new Object[] {out.copyOf(obj, () -> wrapped(obj))});
                case "java.util.Collections$UnmodifiableMap" -> out.writeValues(level,
                        new Object[] {out.copyOf(obj, () -> new LinkedHashMap<>((Map<?, ?>) obj))});
                case "java.lang.StackTraceElement" -> {
                    StackTraceElement at = (StackTraceElement) obj;
                    out.writeValues(level, values(level, "format", (byte) 0,
                            "lineNumber", at.getLineNumber(), "declaringClass", at.getClassName(),
                            "fileName", at.getFileName(), "methodName", at.getMethodName()));
                }
                case "java.lang.Throwable" -> {
                    Throwable thrown = (Throwable) obj;
                    Throwable cause = thrown.getCause();
                    Throwable[] suppressed = thrown.getSuppressed();
                    out.writeValues(level, values(level, "cause", cause == null ? thrown : cause,
                            "detailMessage", message(thrown), "stackTrace", thrown.getStackTrace(),
                            "suppressedExceptions", suppressed.length == 0
                                    ? Collections.unmodifiableList(new ArrayList<>())
                                    : new ArrayList<>(List.of(suppressed))));
                }
                default -> {
                }
            }
        }

        /** an object of a jdk class, read whole */
        Object read(ObjectInputStream in, ObjectStreamClass desc)
                throws IOException, ClassNotFoundException {
            Supplier<Throwable> thrown = THROWABLES.get(name);
            Object made = thrown != null ? thrown.get() : switch (name) {
                case "java.util.ArrayList" -> new ArrayList<>();
                case "java.util.LinkedList" -> new LinkedList<>();
                case "java.util.ArrayDeque" -> new ArrayDeque<>();
                case "java.util.HashMap" -> new HashMap<>();
                case "java.util.LinkedHashMap" -> new LinkedHashMap<>();
                case "java.util.HashSet" -> new HashSet<>();
                case "java.util.LinkedHashSet" -> new LinkedHashSet<>();
                case "java.util.Date" -> new Date(0);
                default -> null;
            };
            if (made != null) {
                in.fill(in.reserve(), made);
                in.readSerialData(made, desc);
                return made;
            }
            int handle = in.reserve();
            Object value = readValue(in, desc, handle);
            in.fill(handle, value);
            return value;
        }

        /** this level's data, read into an object made before it */
        @SuppressWarnings({"unchecked", "rawtypes"})
        void fill(ObjectInputStream in, ObjectStreamClass level, Object target)
                throws IOException, ClassNotFoundException {
            Map<String, Object> values = in.readValues(level);
            switch (name) {
                case "java.util.ArrayList", "java.util.LinkedList", "java.util.ArrayDeque" -> {
                    int size = in.readInt();
                    for (int at = 0; at < size; at++) {
                        ((Collection) target).add(in.readNested());
                    }
                }
                case "java.util.HashMap" -> {
                    in.readInt();
                    int size = in.readInt();
                    for (int at = 0; at < size; at++) {
                        ((Map) target).put(in.readNested(), in.readNested());
                    }
                }
                case "java.util.HashSet" -> {
                    in.readInt();
                    in.readFloat();
                    int size = in.readInt();
                    for (int at = 0; at < size; at++) {
                        ((Collection) target).add(in.readNested());
                    }
                }
                case "java.util.Date" -> ((Date) target).setTime(in.readLong());
                case "java.lang.Throwable" -> fillThrowable((Throwable) target, values);
                default -> {
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object readValue(ObjectInputStream in, ObjectStreamClass desc, int handle)
                throws IOException, ClassNotFoundException {
            Map<String, Object> values = new HashMap<>();
            for (ObjectStreamClass level : desc.hierarchy()) {
                if (level.hasWriteObjectData()) {
                    values.putAll(in.readValues(level));
                    in.setBlockDataMode(true);
                    readCustom(in, level.name, values, handle);
                    in.skipCustomData();
                } else {
                    values.putAll(in.readValues(level));
                }
            }
            return switch (name) {
                case "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
                     "java.lang.Double", "java.lang.Float", "java.lang.Character",
                     "java.lang.Boolean" -> values.get("value");
                case "java.util.UUID" -> new UUID((Long) values.get("mostSigBits"),
                        (Long) values.get("leastSigBits"));
                case "java.util.TreeMap", "java.util.TreeSet", "java.util.CollSer" ->
                    values.get("made");
                case "java.util.Collections$EmptyList" -> Collections.emptyList();
                case "java.util.Collections$EmptySet" -> Collections.emptySet();
                case "java.util.Collections$EmptyMap" -> Collections.emptyMap();
                case "java.util.Collections$SingletonList" ->
                    Collections.singletonList(values.get("element"));
                case "java.util.Collections$SingletonSet" ->
                    Collections.singleton(values.get("element"));
                case "java.util.Collections$SingletonMap" ->
                    Collections.singletonMap(values.get("k"), values.get("v"));
                case "java.util.Collections$UnmodifiableCollection" ->
                    Collections.unmodifiableCollection((Collection) values.get("c"));
                case "java.util.Collections$UnmodifiableList",
                     "java.util.Collections$UnmodifiableRandomAccessList" ->
                    Collections.unmodifiableList((List) values.get("list"));
                case "java.util.Collections$UnmodifiableSet" ->
                    Collections.unmodifiableSet((Set) values.get("c"));
                case "java.util.Collections$UnmodifiableMap" ->
                    Collections.unmodifiableMap((Map) values.get("m"));
                case "java.lang.StackTraceElement" -> new StackTraceElement(
                        (String) values.get("declaringClass"), (String) values.get("methodName"),
                        (String) values.get("fileName"), (Integer) values.get("lineNumber"));
                default -> throw new InvalidClassException(name, "not read here");
            };
        }

        /** the block data of a class whose value is made from it */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void readCustom(ObjectInputStream in, String level,
                                       Map<String, Object> values, int handle)
                throws IOException, ClassNotFoundException {
            switch (level) {
                case "java.util.TreeMap" -> {
                    TreeMap map = new TreeMap((Comparator) values.get("comparator"));
                    in.fill(handle, map);
                    int size = in.readInt();
                    for (int at = 0; at < size; at++) {
                        map.put(in.readNested(), in.readNested());
                    }
                    values.put("made", map);
                }
                case "java.util.TreeSet" -> {
                    TreeSet set = new TreeSet((Comparator) in.readNested());
                    in.fill(handle, set);
                    int size = in.readInt();
                    for (int at = 0; at < size; at++) {
                        set.add(in.readNested());
                    }
                    values.put("made", set);
                }
                case "java.util.CollSer" -> {
                    int length = in.readInt();
                    Object[] items = new Object[length];
                    for (int at = 0; at < length; at++) {
                        items[at] = in.readNested();
                    }
                    values.put("made", immutable((Integer) values.get("tag") & 0xFF, items));
                }
                default -> {
                }
            }
        }

        private static Object immutable(int tag, Object[] items) throws InvalidObjectException {
            switch (tag) {
                case IMM_LIST, 4:
                    return Collections.unmodifiableList(new ArrayList<>(List.of(items)));
                case IMM_SET:
                    return Set.of(items);
                case IMM_MAP:
                    Map<Object, Object> map = new LinkedHashMap<>();
                    for (int at = 0; at + 1 < items.length; at += 2) {
                        map.put(items[at], items[at + 1]);
                    }
                    return Collections.unmodifiableMap(map);
                default:
                    throw new InvalidObjectException("unknown collection tag " + tag);
            }
        }

        private static void writeImmutable(ObjectOutputStream out, ObjectStreamClass level,
                                           Object obj) throws IOException {
            List<Object> items = new ArrayList<>();
            int tag;
            if (obj instanceof Map<?, ?> map) {
                tag = IMM_MAP;
                items.addAll(ObjectOutputStream.entries(map));
            } else if (obj instanceof Set<?> set) {
                tag = IMM_SET;
                items.addAll(set);
            } else {
                tag = IMM_LIST;
                items.addAll((Collection<?>) obj);
            }
            out.writeValues(level, new Object[] {tag});
            out.writeInt(items.size());
            writeAll(out, items);
        }

        private static Collection<?> wrapped(Object obj) {
            if (obj instanceof Set<?> set) {
                return new LinkedHashSet<>(set);
            }
            return new ArrayList<>((Collection<?>) obj);
        }

        private static void writeAll(ObjectOutputStream out, Collection<?> items)
                throws IOException {
            for (Object item : items) {
                out.writeObject(item);
            }
        }

        /** the bucket count a jdk HashMap has after this many puts from empty */
        private static int capacity(int size) {
            int capacity = 16;
            while (size > capacity * 0.75f) {
                capacity *= 2;
            }
            return capacity;
        }

        /** values in a level's field order, from name and value pairs; the rest are null */
        private static Object[] values(ObjectStreamClass level, Object... pairs) {
            Object[] values = new Object[level.fields.length];
            for (int at = 0; at < values.length; at++) {
                for (int pair = 0; pair < pairs.length; pair += 2) {
                    if (level.fields[at].getName().equals(pairs[pair])) {
                        values[at] = pairs[pair + 1];
                    }
                }
            }
            return values;
        }

        private static String message(Throwable thrown) {
            Field kept = throwableField("message");
            try {
                return kept != null ? (String) kept.get(thrown) : thrown.getMessage();
            } catch (IllegalAccessException denied) {
                return thrown.getMessage();
            }
        }

        @SuppressWarnings("unchecked")
        private static void fillThrowable(Throwable target, Map<String, Object> values) {
            set(target, "message", values.get("detailMessage"));
            Object cause = values.get("cause");
            set(target, "cause", cause == target ? null : cause);
            Object trace = values.get("stackTrace");
            if (trace instanceof StackTraceElement[] elements) {
                target.setStackTrace(elements);
            }
            Object suppressed = values.get("suppressedExceptions");
            if (suppressed instanceof List<?> list) {
                for (Object each : list) {
                    target.addSuppressed((Throwable) each);
                }
            }
        }

        private static void set(Throwable target, String field, Object value) {
            Field kept = throwableField(field);
            if (kept == null) {
                return;
            }
            try {
                kept.set(target, value);
            } catch (IllegalAccessException denied) {
                // the build keeps these fields, so this is not reached
            }
        }

        private static Field throwableField(String field) {
            try {
                Field found = Throwable.class.getDeclaredField(field);
                found.setAccessible(true);
                return found;
            } catch (NoSuchFieldException absent) {
                return null;
            }
        }
    }
}
