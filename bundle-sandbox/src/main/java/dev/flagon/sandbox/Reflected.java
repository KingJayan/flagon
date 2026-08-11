package dev.flagon.sandbox;

import dev.flagon.reflect.Reflectable;

/**
 * a class opted into member reflection by annotation, for source the bundle owns
 */
@Reflectable
public final class Reflected {

    private String label;
    private int count;

    public Reflected() {
        this("unnamed", 1);
    }

    public Reflected(String label, int count) {
        this.label = label;
        this.count = count;
    }

    public String label() {
        return label;
    }

    public int twice() {
        return count * 2;
    }
}
