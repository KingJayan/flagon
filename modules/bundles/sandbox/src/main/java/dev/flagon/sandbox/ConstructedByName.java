package dev.flagon.sandbox;

/**
 * a class the build listed for the compiler, standing in for one out of a jar nobody can annotate
 */
public final class ConstructedByName {

    private final String label;

    public ConstructedByName() {
        this("unnamed");
    }

    public ConstructedByName(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
