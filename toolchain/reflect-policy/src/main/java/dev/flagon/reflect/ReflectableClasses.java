package dev.flagon.reflect;

import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMember;
import org.teavm.extension.introspect.IntrospectParameter;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.spi.GlobMatch;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

import org.teavm.extension.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * carries the members of chosen classes through compilation, so reflection can still find them
 */
public final class ReflectableClasses extends SimpleReflectionPolicy {

    private IntrospectClass<?> externalizable;

    public static final String PATTERNS = "flagon.reflect.classes";
    public static final String PROXIES = "flagon.reflect.proxies";
    /** a classpath resource that lists one class per line */
    public static final String CONSTRUCTED = "META-INF/flagon/constructed.txt";
    public static final String CONVERTED = "flagon.reflect.converted";
    /** classes found by name and built through a static factory, like log4j plugins */
    public static final String PRESERVED = "META-INF/flagon/preserved.txt";

    private static final String UNCONSTRUCTED = "dev.flagon.serial.Unconstructed";
    private static final Set<String> SERIAL_STATICS = Set.of("serialVersionUID",
            "serialPersistentFields");
    private static final Set<String> SERIAL_METHODS = Set.of("writeObject", "readObject",
            "readObjectNoData", "writeReplace", "readResolve", "$flagon$reflection");

    @Override
    protected void setup() {
        keep(selectClasses(present(type -> type.hasAnnotation(Reflectable.class))));
        for (String pattern : patterns(PATTERNS)) {
            keep(selectClasses(present(type -> GlobMatch.match(pattern, type.name()))));
        }
        for (String pattern : patterns(PROXIES)) {
            selectClasses(present(type -> type.isInterface()
                    && GlobMatch.match(pattern, type.name())))
                    .foundByName()
                    .proxyable();
        }

        for (String pattern : patterns(CONVERTED)) {
            selectClasses(present(type -> GlobMatch.match(pattern, type.name())))
                    .foundByName()
                    .reflectableMethods(ReflectableClasses::convertsFromText);
        }

        Set<String> preserved = listed(PRESERVED);
        if (!preserved.isEmpty()) {
            selectClasses(present(type -> preserved.contains(type.name())))
                    .foundByName()
                    .reflectableMethods(method -> Modifier.isStatic(method.modifiers()));
        }

        IntrospectClass<?> serializable = environment().findClass("java.io.Serializable");
        externalizable = environment().findClass("java.io.Externalizable");
        if (serializable != null) {
            selectClasses(present(type -> !type.isInterface() && !jdk(type.name())
                    && serializable.isAssignableFrom(type)))
                    .foundByName()
                    .reflectableMembers(this::serializes);
        }
        selectClasses(present(type -> type.name().equals("java.lang.Throwable")))
                .reflectableMembers(member -> member.name().equals("message")
                        || member.name().equals("cause"));

        Set<String> constructed = listed(CONSTRUCTED);
        if (!constructed.isEmpty()) {
            selectClasses(present(type -> constructed.contains(type.name())))
                    .foundByName()
                    .reflectableMethods(IntrospectMethod::isConstructor);
        }
    }

    /** teavm passes null for a class it cannot find, and a missing class keeps nothing */
    private static Predicate<IntrospectClass<?>> present(Predicate<IntrospectClass<?>> test) {
        return type -> type != null && test.test(type);
    }

    /**
     * what java serialization reads and writes of a serializable class. of its constructors, only
     * the ones deserialization calls, since keeping one keeps its code
     */
    private boolean serializes(IntrospectMember member) {
        if (member instanceof IntrospectField field) {
            return !Modifier.isStatic(field.modifiers()) || SERIAL_STATICS.contains(field.name());
        }
        IntrospectMethod method = (IntrospectMethod) member;
        if (!method.isConstructor()) {
            return SERIAL_METHODS.contains(method.name());
        }
        IntrospectClass<?> owner = method.declaringClass();
        List<? extends IntrospectParameter> parameters = method.parameters();
        if (owner.isRecord()) {
            return true;
        }
        if (parameters.isEmpty()) {
            return externalizable != null && externalizable.isAssignableFrom(owner);
        }
        return parameters.size() == 1 && UNCONSTRUCTED.equals(parameters.get(0).type().name());
    }

    private static boolean jdk(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("sun.") || name.startsWith("org.teavm.");
    }

    /** valueOf(String) or a (String) constructor, which an option parser calls to read a value */
    private static boolean convertsFromText(IntrospectMethod method) {
        if (!Modifier.isPublic(method.modifiers()) || method.parameters().size() != 1
                || !"java.lang.String".equals(method.parameters().get(0).type().name())) {
            return false;
        }
        return method.isConstructor()
                || Modifier.isStatic(method.modifiers()) && "valueOf".equals(method.name());
    }

    private static void keep(ClassPolicy chosen) {
        chosen.foundByName().reflectableMembers(member -> true);
    }

    private List<String> patterns(String property) {
        List<String> patterns = new ArrayList<>();
        String listed = environment().property(property);
        if (listed == null) {
            return patterns;
        }
        for (String pattern : listed.split("[,\\s]+")) {
            if (!pattern.isEmpty()) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    private Set<String> listed(String resource) {
        Set<String> classes = new LinkedHashSet<>();
        Iterator<Resource> lists = environment().resources(resource);
        while (lists.hasNext()) {
            try (InputStream list = lists.next().open()) {
                for (String line : new String(list.readAllBytes(), StandardCharsets.UTF_8)
                        .split("\\R")) {
                    if (!line.isBlank()) {
                        classes.add(line.trim());
                    }
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException("cannot read " + resource, unreadable);
            }
        }
        return classes;
    }
}
