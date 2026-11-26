package dev.flagon.platform;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * teavm keeps no generic supertypes, so a List knows nothing of its Collection<E>. gson walks
 * that path to find an element type, so for the jdk's collections and maps, where each type hands
 * its own parameters straight up, the generic supertypes are made here.
 */
final class GenericSupertypes {

    private static final List<String> COLLECTIONS = List.of(
            "java.lang.Iterable", "java.util.Collection", "java.util.SequencedCollection",
            "java.util.List", "java.util.Set", "java.util.SequencedSet", "java.util.SortedSet",
            "java.util.NavigableSet", "java.util.Queue", "java.util.Deque",
            "java.util.AbstractCollection", "java.util.AbstractList",
            "java.util.AbstractSequentialList", "java.util.AbstractSet", "java.util.AbstractQueue",
            "java.util.ArrayList", "java.util.LinkedList", "java.util.ArrayDeque",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet",
            "java.util.EnumSet", "java.util.PriorityQueue", "java.util.Vector", "java.util.Stack",
            "java.util.concurrent.BlockingQueue", "java.util.concurrent.BlockingDeque",
            "java.util.concurrent.CopyOnWriteArrayList",
            "java.util.concurrent.CopyOnWriteArraySet",
            "java.util.concurrent.ConcurrentLinkedQueue",
            "java.util.concurrent.ConcurrentLinkedDeque",
            "java.util.concurrent.ArrayBlockingQueue", "java.util.concurrent.LinkedBlockingQueue",
            "java.util.concurrent.LinkedBlockingDeque",
            "java.util.concurrent.ConcurrentSkipListSet");
    private static final List<String> MAPS = List.of(
            "java.util.Map", "java.util.SequencedMap", "java.util.SortedMap",
            "java.util.NavigableMap", "java.util.AbstractMap", "java.util.HashMap",
            "java.util.LinkedHashMap", "java.util.TreeMap", "java.util.WeakHashMap",
            "java.util.IdentityHashMap", "java.util.EnumMap", "java.util.Hashtable",
            "java.util.Dictionary", "java.util.concurrent.ConcurrentMap",
            "java.util.concurrent.ConcurrentNavigableMap", "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.ConcurrentSkipListMap");
    private static final String[] ELEMENT = {"E"};
    private static final String[] KEY_AND_VALUE = {"K", "V"};

    private static final Map<Class<?>, TypeVariable<?>[]> PARAMETERS = new HashMap<>();

    private GenericSupertypes() {
    }

    /** the type's own parameters when it is one made here, or null */
    static TypeVariable<?>[] parameters(Class<?> type) {
        String[] names = namesOf(type);
        if (names == null) {
            return null;
        }
        TypeVariable<?>[] made = PARAMETERS.get(type);
        if (made == null) {
            made = new TypeVariable<?>[names.length];
            for (int at = 0; at < names.length; at++) {
                made[at] = new Variable(type, names[at]);
            }
            PARAMETERS.put(type, made);
        }
        return made.clone();
    }

    static Type superclass(Class<?> type) {
        return of(type, type.getSuperclass());
    }

    /** in the order getInterfaces gives them, as gson matches the two up by index */
    static Type[] interfaces(Class<?> type) {
        Class<?>[] raw = type.getInterfaces();
        Type[] generic = new Type[raw.length];
        for (int at = 0; at < raw.length; at++) {
            generic[at] = of(type, raw[at]);
        }
        return generic;
    }

    private static Type of(Class<?> type, Class<?> supertype) {
        String[] names = namesOf(type);
        if (supertype == null || names == null || namesOf(supertype) != names) {
            return supertype;
        }
        return new Parameterized(supertype, parameters(type));
    }

    private static String[] namesOf(Class<?> type) {
        if (type == null) {
            return null;
        }
        if (COLLECTIONS.contains(type.getName())) {
            return ELEMENT;
        }
        return MAPS.contains(type.getName()) ? KEY_AND_VALUE : null;
    }

    private record Parameterized(Class<?> raw, Type[] arguments) implements ParameterizedType {

        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ParameterizedType that && raw.equals(that.getRawType())
                    && that.getOwnerType() == null
                    && Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ raw.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder written = new StringBuilder(raw.getName()).append('<');
            for (int at = 0; at < arguments.length; at++) {
                Type argument = arguments[at];
                written.append(at == 0 ? "" : ", ")
                        .append(argument instanceof Class<?> named ? named.getName() : argument);
            }
            return written.append('>').toString();
        }
    }

    private record Variable(Class<?> declaration, String name) implements TypeVariable<Class<?>> {

        @Override
        public Type[] getBounds() {
            return new Type[] {Object.class};
        }

        @Override
        public Class<?> getGenericDeclaration() {
            return declaration;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return new AnnotatedType[0];
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            Objects.requireNonNull(annotationClass);
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
