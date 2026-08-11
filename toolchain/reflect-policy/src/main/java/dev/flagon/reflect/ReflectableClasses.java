package dev.flagon.reflect;

import org.teavm.extension.spi.GlobMatch;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * carries the members of chosen classes through compilation, so reflection can still find them
 */
public final class ReflectableClasses extends SimpleReflectionPolicy {

    public static final String PATTERNS = "flagon.reflect.classes";

    @Override
    protected void setup() {
        keep(selectClasses(withAnnotation(Reflectable.class)));
        for (String pattern : patterns()) {
            keep(selectClasses(type -> GlobMatch.match(pattern, type.name())));
        }
    }

    private static void keep(ClassPolicy chosen) {
        chosen.foundByName().reflectableMembers(member -> true);
    }

    private List<String> patterns() {
        List<String> patterns = new ArrayList<>();
        String listed = environment().property(PATTERNS);
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
}