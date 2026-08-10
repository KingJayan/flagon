package dev.flagon.lwjgl.internal;

/**
 * 4x4 float matrix in column-major order, matching what gl expects and what the shader is fed.
 * operations post-multiply, so {@code translate} then {@code rotate} behaves like the gl calls
 * of the same name issued in that order.
 */
public final class Matrix4 {

    private static final float[] SCRATCH = new float[16];

    private Matrix4() {
    }

    public static float[] identity() {
        return setIdentity(new float[16]);
    }

    public static float[] setIdentity(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = 0.0f;
        }
        m[0] = 1.0f;
        m[5] = 1.0f;
        m[10] = 1.0f;
        m[15] = 1.0f;
        return m;
    }

    /** {@code target = target * right}. */
    public static void multiply(float[] target, float[] right) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) {
                    sum += target[k * 4 + row] * right[column * 4 + k];
                }
                SCRATCH[column * 4 + row] = sum;
            }
        }
        System.arraycopy(SCRATCH, 0, target, 0, 16);
    }

    public static void translate(float[] target, float x, float y, float z) {
        for (int row = 0; row < 4; row++) {
            target[12 + row] += target[row] * x + target[4 + row] * y + target[8 + row] * z;
        }
    }

    public static void scale(float[] target, float x, float y, float z) {
        for (int row = 0; row < 4; row++) {
            target[row] *= x;
            target[4 + row] *= y;
            target[8 + row] *= z;
        }
    }

    /** rotates by {@code degrees} about the given axis, as {@code glRotatef} does. */
    public static void rotate(float[] target, float degrees, float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0f) {
            return;
        }
        float nx = x / length;
        float ny = y / length;
        float nz = z / length;
        double radians = Math.toRadians(degrees);
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        float ic = 1.0f - c;

        float[] rotation = new float[16];
        rotation[0] = nx * nx * ic + c;
        rotation[1] = ny * nx * ic + nz * s;
        rotation[2] = nz * nx * ic - ny * s;
        rotation[4] = nx * ny * ic - nz * s;
        rotation[5] = ny * ny * ic + c;
        rotation[6] = nz * ny * ic + nx * s;
        rotation[8] = nx * nz * ic + ny * s;
        rotation[9] = ny * nz * ic - nx * s;
        rotation[10] = nz * nz * ic + c;
        rotation[15] = 1.0f;
        multiply(target, rotation);
    }

    public static void ortho(float[] target, float left, float right, float bottom, float top,
                             float near, float far) {
        float[] m = new float[16];
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        m[15] = 1.0f;
        multiply(target, m);
    }

    public static void frustum(float[] target, float left, float right, float bottom, float top,
                               float near, float far) {
        float[] m = new float[16];
        m[0] = 2.0f * near / (right - left);
        m[5] = 2.0f * near / (top - bottom);
        m[8] = (right + left) / (right - left);
        m[9] = (top + bottom) / (top - bottom);
        m[10] = -(far + near) / (far - near);
        m[11] = -1.0f;
        m[14] = -2.0f * far * near / (far - near);
        multiply(target, m);
    }

    public static float[] normalMatrix(float[] m) {
        float a = m[0];
        float b = m[1];
        float c = m[2];
        float d = m[4];
        float e = m[5];
        float f = m[6];
        float g = m[8];
        float h = m[9];
        float i = m[10];

        float determinant = a * (e * i - f * h) - d * (b * i - c * h) + g * (b * f - c * e);
        if (determinant == 0.0f) {
            return new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        }
        float scale = 1.0f / determinant;
        // the transpose of the inverse, written out column-major
        return new float[] {
                (e * i - f * h) * scale, (g * f - d * i) * scale, (d * h - g * e) * scale,
                (h * c - b * i) * scale, (a * i - g * c) * scale, (g * b - a * h) * scale,
                (b * f - e * c) * scale, (d * c - a * f) * scale, (a * e - d * b) * scale,
        };
    }

    /** @return {@code m * point}, the four components of a point carried into the matrix's space */
    public static float[] transform(float[] m, float x, float y, float z, float w) {
        float[] out = new float[4];
        for (int row = 0; row < 4; row++) {
            out[row] = m[row] * x + m[4 + row] * y + m[8 + row] * z + m[12 + row] * w;
        }
        return out;
    }

    public static float[] copy(float[] source) {
        float[] target = new float[16];
        System.arraycopy(source, 0, target, 0, 16);
        return target;
    }
}
