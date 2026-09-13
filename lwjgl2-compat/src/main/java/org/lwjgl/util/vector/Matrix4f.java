package org.lwjgl.util.vector;

import dev.flagon.lwjgl.internal.Matrix4;

import java.nio.FloatBuffer;

/**
 * a four by four matrix of floats, held column by column the way gl reads one
 */
public class Matrix4f extends Matrix {

    public float m00;
    public float m01;
    public float m02;
    public float m03;
    public float m10;
    public float m11;
    public float m12;
    public float m13;
    public float m20;
    public float m21;
    public float m22;
    public float m23;
    public float m30;
    public float m31;
    public float m32;
    public float m33;

    public Matrix4f() {
        setIdentity();
    }

    public Matrix4f(Matrix4f other) {
        load(other);
    }

    @Override
    public Matrix4f setIdentity() {
        m00 = 1.0f;
        m11 = 1.0f;
        m22 = 1.0f;
        m33 = 1.0f;
        m01 = 0.0f;
        m02 = 0.0f;
        m03 = 0.0f;
        m10 = 0.0f;
        m12 = 0.0f;
        m13 = 0.0f;
        m20 = 0.0f;
        m21 = 0.0f;
        m23 = 0.0f;
        m30 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        return this;
    }

    @Override
    public Matrix4f setZero() {
        m00 = 0.0f;
        m11 = 0.0f;
        m22 = 0.0f;
        m33 = 0.0f;
        m01 = 0.0f;
        m02 = 0.0f;
        m03 = 0.0f;
        m10 = 0.0f;
        m12 = 0.0f;
        m13 = 0.0f;
        m20 = 0.0f;
        m21 = 0.0f;
        m23 = 0.0f;
        m30 = 0.0f;
        m31 = 0.0f;
        m32 = 0.0f;
        return this;
    }

    public Matrix4f load(Matrix4f other) {
        float[] values = other.toArray();
        fromArray(values);
        return this;
    }

    @Override
    public Matrix4f load(FloatBuffer buffer) {
        float[] values = new float[16];
        buffer.get(values);
        fromArray(values);
        return this;
    }

    @Override
    public Matrix4f store(FloatBuffer buffer) {
        buffer.put(toArray());
        return this;
    }

    /** @param result takes the product, or a new matrix holds it if this is null */
    public static Matrix4f mul(Matrix4f left, Matrix4f right, Matrix4f result) {
        float[] product = left.toArray();
        Matrix4.multiply(product, right.toArray());
        Matrix4f into = result == null ? new Matrix4f() : result;
        into.fromArray(product);
        return into;
    }

    /** @param result takes the transformed vector, or a new vector holds it if this is null */
    public static Vector4f transform(Matrix4f left, Vector4f right, Vector4f result) {
        float[] moved = Matrix4.transform(left.toArray(), right.x, right.y, right.z, right.w);
        Vector4f into = result == null ? new Vector4f() : result;
        into.set(moved[0], moved[1], moved[2], moved[3]);
        return into;
    }

    /**
     * @param angle the turn in radians
     * @param destination takes the turned matrix, or a new matrix holds it if this is null
     */
    public static Matrix4f rotate(float angle, Vector3f axis, Matrix4f source,
                                  Matrix4f destination) {
        float[] turned = source.toArray();
        Matrix4.rotate(turned, (float) Math.toDegrees(angle), axis.x, axis.y, axis.z);
        Matrix4f into = destination == null ? new Matrix4f() : destination;
        into.fromArray(turned);
        return into;
    }

    /** @param destination takes the moved matrix, or a new matrix holds it if this is null */
    public static Matrix4f translate(Vector3f by, Matrix4f source, Matrix4f destination) {
        float[] moved = source.toArray();
        Matrix4.translate(moved, by.x, by.y, by.z);
        Matrix4f into = destination == null ? new Matrix4f() : destination;
        into.fromArray(moved);
        return into;
    }

    /** @param destination takes the scaled matrix, or a new matrix holds it if this is null */
    public static Matrix4f scale(Vector3f by, Matrix4f source, Matrix4f destination) {
        float[] stretched = source.toArray();
        Matrix4.scale(stretched, by.x, by.y, by.z);
        Matrix4f into = destination == null ? new Matrix4f() : destination;
        into.fromArray(stretched);
        return into;
    }

    @Override
    public Matrix4f transpose() {
        float[] values = toArray();
        float[] turned = new float[16];
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                turned[row * 4 + column] = values[column * 4 + row];
            }
        }
        fromArray(turned);
        return this;
    }

    @Override
    public Matrix4f negate() {
        float[] values = toArray();
        for (int at = 0; at < values.length; at++) {
            values[at] = -values[at];
        }
        fromArray(values);
        return this;
    }

    @Override
    public float determinant() {
        float[] m = toArray();
        return m[0] * cofactor(m, 0, 0) - m[4] * cofactor(m, 1, 0)
                + m[8] * cofactor(m, 2, 0) - m[12] * cofactor(m, 3, 0);
    }

    /** @return this matrix inverted, or null if it has no inverse */
    @Override
    public Matrix4f invert() {
        float[] inverted = Matrix4.invert(toArray());
        if (inverted == null) {
            return null;
        }
        fromArray(inverted);
        return this;
    }

    /** @return the determinant of the matrix left when the column and the row are struck out */
    private static float cofactor(float[] m, int column, int row) {
        float[] smaller = new float[9];
        int at = 0;
        for (int c = 0; c < 4; c++) {
            if (c == column) {
                continue;
            }
            for (int r = 0; r < 4; r++) {
                if (r != row) {
                    smaller[at++] = m[c * 4 + r];
                }
            }
        }
        float sign = ((column + row) & 1) == 0 ? 1.0f : -1.0f;
        return sign * (smaller[0] * (smaller[4] * smaller[8] - smaller[7] * smaller[5])
                - smaller[3] * (smaller[1] * smaller[8] - smaller[7] * smaller[2])
                + smaller[6] * (smaller[1] * smaller[5] - smaller[4] * smaller[2]));
    }

    /** @return the values column by column, which is the order gl takes them in */
    public float[] toArray() {
        return new float[] {
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33,
        };
    }

    private void fromArray(float[] values) {
        m00 = values[0];
        m01 = values[1];
        m02 = values[2];
        m03 = values[3];
        m10 = values[4];
        m11 = values[5];
        m12 = values[6];
        m13 = values[7];
        m20 = values[8];
        m21 = values[9];
        m22 = values[10];
        m23 = values[11];
        m30 = values[12];
        m31 = values[13];
        m32 = values[14];
        m33 = values[15];
    }

    @Override
    public String toString() {
        return "Matrix4f" + java.util.Arrays.toString(toArray());
    }
}
