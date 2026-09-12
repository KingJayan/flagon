package org.lwjgl.util.vector;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * what every vector here can do, whatever its width
 */
public abstract class Vector implements Serializable, ReadableVector {

    protected Vector() {
    }

    @Override
    public final float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    /** scales the vector to length one, and leaves a zero vector alone */
    public final Vector normalise() {
        float length = length();
        if (length == 0.0f) {
            throw new IllegalStateException("a vector of zero length has no direction to keep");
        }
        return scale(1.0f / length);
    }

    public abstract Vector negate();

    public abstract Vector scale(float factor);

    public abstract Vector load(FloatBuffer buffer);

    @Override
    public abstract Vector store(FloatBuffer buffer);
}
