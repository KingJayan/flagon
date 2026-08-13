package dev.flagon.reflect;

import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.spi.GlobMatch;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * carries the members of chosen classes through compilation, so reflection can still find them
 */
public final class ReflectableClasses extends SimpleReflectionPolicy {

    public static final String PATTERNS = "flagon.reflect.classes";
    public static final String PROXIES = "flagon.reflect.proxies";
    public static final String CONSTRUCTED = "flagon.reflect.constructed";

    @Override
    protected void setup() {
        keep(selectClasses(withAnnotation(Reflectable.class)));
        for (String pattern : patterns(PATTERNS)) {
            keep(selectClasses(type -> GlobMatch.match(pattern, type.name())));
        }
        for (String pattern : patterns(PROXIES)) {
            selectClasses(type -> type.isInterface() && GlobMatch.match(pattern, type.name()))
                    .foundByName()
                    .proxyable();
        }

        Set<String> constructed = constructed();
        if (!constructed.isEmpty()) {
            selectClasses(type -> constructed.contains(type.name()))
                    .foundByName()
                    .reflectableMethods(IntrospectMethod::isConstructor);
        }
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

    /**
     * the classes a file names, one per line
     *
     * <p>a jar nobody wrote cannot be annotated, and an obfuscated one cannot be globbed by hand
     * either, so the build lists its classes instead. they keep their constructors, which is what
     * such a jar reflects for: a registry that builds an entry from the class it was handed.
     */
    private Set<String> constructed() {
        String named = environment().property(CONSTRUCTED);
        if (named == null || named.isBlank() || !Files.isRegularFile(Path.of(named))) {
            return Set.of();
        }
        Set<String> classes = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(Path.of(named), StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    classes.add(line.trim());
                }
            }
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + named, unreadable);
        }
        return classes;
    }
}
