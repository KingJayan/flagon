package dev.flagon.reflect;

import org.teavm.extension.introspect.IntrospectClass;
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

    public static final String PATTERNS = "flagon.reflect.classes";
    public static final String PROXIES = "flagon.reflect.proxies";
    /** a classpath resource that lists one class per line */
    public static final String CONSTRUCTED = "META-INF/flagon/constructed.txt";
    public static final String CONVERTED = "flagon.reflect.converted";

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

        Set<String> constructed = constructed();
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

    private Set<String> constructed() {
        Set<String> classes = new LinkedHashSet<>();
        Iterator<Resource> lists = environment().resources(CONSTRUCTED);
        while (lists.hasNext()) {
            try (InputStream list = lists.next().open()) {
                for (String line : new String(list.readAllBytes(), StandardCharsets.UTF_8)
                        .split("\\R")) {
                    if (!line.isBlank()) {
                        classes.add(line.trim());
                    }
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException("cannot read " + CONSTRUCTED, unreadable);
            }
        }
        return classes;
    }
}
