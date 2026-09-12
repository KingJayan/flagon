package org.lwjgl.util.vector;

import java.nio.FloatBuffer;

/**
 * a four component vector of floats
 */
public class Vector4f extends Vector implements ReadableVector4f {

    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f() {
    }

    public Vector4f(float x, float y, float z, float w) {
        set(x, y, z, w);
    }

    public Vector4f(ReadableVector4f other) {
        set(other.getX(), other.getY(), other.getZ(), other.getW());
    }

    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /** @param result takes the sum, or a new vector holds it if this is null */
    public static Vector4f add(Vector4f left, Vector4f right, Vector4f result) {
        if (result == null) {
            return new Vector4f(left.x + right.x, left.y + right.y,
                    left.z + right.z, left.w + right.w);
        }
        result.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
        return result;
    }

    /** @param result takes the difference, or a new vector holds it if this is null */
    public static Vector4f sub(Vector4f left, Vector4f right, Vector4f result) {
        if (result == null) {
            return new Vector4f(left.x - right.x, left.y - right.y,
                    left.z - right.z, left.w - right.w);
        }
        result.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
        return result;
    }

    public static float dot(Vector4f left, Vector4f right) {
        return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
    }

    @Override
    public Vector4f negate() {
        return new Vector4f(-x, -y, -z, -w);
    }

    @Override
    public Vector4f scale(float factor) {
        x *= factor;
        y *= factor;
        z *= factor;
        w *= factor;
        return this;
    }

    @Override
    public Vector4f load(FloatBuffer buffer) {
        set(buffer.get(), buffer.get(), buffer.get(), buffer.get());
        return this;
    }

    @Override
    public Vector4f store(FloatBuffer buffer) {
        buffer.put(x).put(y).put(z).put(w);
        return this;
    }

    @Override
    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
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

    @Override
    public float getW() {
        return w;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vector4f vector)) {
            return false;
        }
        return x == vector.x && y == vector.y && z == vector.z && w == vector.w;
    }

    @Override
    public int hashCode() {
        return ((Float.hashCode(x) * 31 + Float.hashCode(y)) * 31
                + Float.hashCode(z)) * 31 + Float.hashCode(w);
    }

    @Override
    public String toString() {
        return "Vector4f[" + x + ", " + y + ", " + z + ", " + w + "]";
    }
}
