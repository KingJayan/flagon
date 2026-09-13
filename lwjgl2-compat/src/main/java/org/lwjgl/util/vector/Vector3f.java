package org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/**
 * a three component vector of floats
 */
public class Vector3f extends Vector implements ReadableVector3f {

    public float x;
    public float y;
    public float z;

    public Vector3f() {
    }

    public Vector3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3f(ReadableVector3f other) {
        set(other.getX(), other.getY(), other.getZ());
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f set(ReadableVector3f other) {
        set(other.getX(), other.getY(), other.getZ());
        return this;
    }

    /** @param result takes the sum, or a new vector holds it if this is null */
    public static Vector3f add(Vector3f left, Vector3f right, Vector3f result) {
        if (result == null) {
            return new Vector3f(left.x + right.x, left.y + right.y, left.z + right.z);
        }
        result.set(left.x + right.x, left.y + right.y, left.z + right.z);
        return result;
    }

    /** @param result takes the difference, or a new vector holds it if this is null */
    public static Vector3f sub(Vector3f left, Vector3f right, Vector3f result) {
        if (result == null) {
            return new Vector3f(left.x - right.x, left.y - right.y, left.z - right.z);
        }
        result.set(left.x - right.x, left.y - right.y, left.z - right.z);
        return result;
    }

    /** @param result takes the cross product, or a new vector holds it if this is null */
    public static Vector3f cross(Vector3f left, Vector3f right, Vector3f result) {
        float x = left.y * right.z - left.z * right.y;
        float y = right.x * left.z - right.z * left.x;
        float z = left.x * right.y - left.y * right.x;
        if (result == null) {
            return new Vector3f(x, y, z);
        }
        result.set(x, y, z);
        return result;
    }

    public static float dot(Vector3f left, Vector3f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z;
    }

    /** @return the angle between the two vectors, in radians */
    public static float angle(Vector3f a, Vector3f b) {
        float cosine = dot(a, b) / (a.length() * b.length());
        return (float) Math.acos(Math.min(1.0f, Math.max(-1.0f, cosine)));
    }

    @Override
    public Vector3f negate() {
        return new Vector3f(-x, -y, -z);
    }

    @Override
    public Vector3f scale(float factor) {
        x *= factor;
        y *= factor;
        z *= factor;
        return this;
    }

    @Override
    public Vector3f load(FloatBuffer buffer) {
        set(buffer.get(), buffer.get(), buffer.get());
        return this;
    }

    @Override
    public Vector3f store(FloatBuffer buffer) {
        buffer.put(x).put(y).put(z);
        return this;
    }

    @Override
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getZ() {
        return z;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vector3f vector)) {
            return false;
        }
        return x == vector.x && y == vector.y && z == vector.z;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 * 31 + Float.hashCode(y) * 31 + Float.hashCode(z);
    }

    @Override
    public String toString() {
        return "Vector3f[" + x + ", " + y + ", " + z + "]";
    }
}
