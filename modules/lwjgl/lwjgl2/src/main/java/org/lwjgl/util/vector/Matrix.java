package org.lwjgl.util.vector;

import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * what every matrix here can do, whatever its size
 */
public abstract class Matrix implements Serializable {

    protected Matrix() {
    }

    public abstract Matrix setIdentity();

    public abstract Matrix setZero();

    public abstract Matrix invert();

    public abstract Matrix negate();

    public abstract Matrix transpose();

    public abstract float determinant();

    public abstract Matrix load(FloatBuffer buffer);

    public abstract Matrix store(FloatBuffer buffer);
}
