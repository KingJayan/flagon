package org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/** a vector that can be read but not written */
public interface ReadableVector {

    float length();

    float lengthSquared();

    Vector store(FloatBuffer buffer);
}
