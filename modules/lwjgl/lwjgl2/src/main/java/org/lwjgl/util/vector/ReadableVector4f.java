package org.lwjgl.util.vector;

/** a four component vector that can be read but not written */
public interface ReadableVector4f extends ReadableVector3f {

    float getW();
}
