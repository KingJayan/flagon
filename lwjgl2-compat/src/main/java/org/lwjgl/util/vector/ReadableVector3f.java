package org.lwjgl.util.vector;

/** a three component vector that can be read but not written */
public interface ReadableVector3f extends ReadableVector {

    float getX();

    float getY();

    float getZ();
}
