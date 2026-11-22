package dev.flagon.serial;

/**
 * the parameter type of the constructor the build gives each serializable class. it runs only
 * the constructor of the first superclass that is not serializable, as deserialization must.
 */
public final class Unconstructed {

    private Unconstructed() {
    }
}
