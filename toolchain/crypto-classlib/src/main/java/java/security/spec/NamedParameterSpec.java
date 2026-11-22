package java.security.spec;

import java.util.Objects;

/** parameters known by a standard name */
public class NamedParameterSpec implements AlgorithmParameterSpec {

    public static final NamedParameterSpec X25519 = new NamedParameterSpec("X25519");
    public static final NamedParameterSpec X448 = new NamedParameterSpec("X448");
    public static final NamedParameterSpec ED25519 = new NamedParameterSpec("Ed25519");
    public static final NamedParameterSpec ED448 = new NamedParameterSpec("Ed448");

    private final String name;

    public NamedParameterSpec(String stdName) {
        this.name = Objects.requireNonNull(stdName, "stdName must not be null");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
