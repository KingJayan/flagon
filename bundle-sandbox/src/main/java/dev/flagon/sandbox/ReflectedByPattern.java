package dev.flagon.sandbox;

/**
 * a class opted in by name from the build, the way a jar that cannot be annotated is
 */
public final class ReflectedByPattern {

    public String greeting = "named by pattern";

    public String greet() {
        return greeting;
    }
}
