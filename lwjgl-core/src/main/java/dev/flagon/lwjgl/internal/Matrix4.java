package dev.flagon.lwjgl.internal;

/**
 * 4x4 float matrix
 */
public final class Matrix4 {

    private static final float[] SCRATCH = new float[16];
    private static final float[] OPERAND = new float[16];

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

        float[] rotation = zeroed(OPERAND);
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
        float[] m = zeroed(OPERAND);
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
        float[] m = zeroed(OPERAND);
        m[0] = 2.0f * near / (right - left);
        m[5] = 2.0f * near / (top - bottom);
        m[8] = (right + left) / (right - left);
        m[9] = (top + bottom) / (top - bottom);
        m[10] = -(far + near) / (far - near);
        m[11] = -1.0f;
        m[14] = -2.0f * far * near / (far - near);
        multiply(target, m);
    }

    private static float[] zeroed(float[] m) {
        for (int i = 0; i < 16; i++) {
            m[i] = 0.0f;
        }
        return m;
    }

    public static float[] normalMatrix(float[] m) {
        return normalMatrix(m, new float[9]);
    }

    public static float[] normalMatrix(float[] m, float[] out) {
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
            setIdentity3(out);
            return out;
        }
        float scale = 1.0f / determinant;
        // the transpose of the inverse, written out column-major
        out[0] = (e * i - f * h) * scale;
        out[1] = (g * f - d * i) * scale;
        out[2] = (d * h - g * e) * scale;
        out[3] = (h * c - b * i) * scale;
        out[4] = (a * i - g * c) * scale;
        out[5] = (g * b - a * h) * scale;
        out[6] = (b * f - e * c) * scale;
        out[7] = (d * c - a * f) * scale;
        out[8] = (a * e - d * b) * scale;
        return out;
    }

    private static void setIdentity3(float[] out) {
        for (int i = 0; i < 9; i++) {
            out[i] = 0.0f;
        }
        out[0] = 1.0f;
        out[4] = 1.0f;
        out[8] = 1.0f;
    }

    /** @return {@code m * point}, the four components of a point carried into the matrix's space */
    public static float[] transform(float[] m, float x, float y, float z, float w) {
        float[] out = new float[4];
        for (int row = 0; row < 4; row++) {
            out[row] = m[row] * x + m[4 + row] * y + m[8 + row] * z + m[12 + row] * w;
        }
        return out;
    }

/** @return the inverse, or null when the matrix is singular and has none */
    public static float[] invert(float[] m) {
        float[] inverse = new float[16];
        inverse[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
                + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inverse[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
                - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inverse[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
                + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inverse[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
                - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inverse[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
                - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inverse[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
                + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inverse[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
                - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inverse[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
                + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inverse[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
                + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inverse[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
                - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inverse[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
                + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inverse[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
                - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inverse[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
                - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inverse[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
                + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inverse[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
                - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inverse[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
                + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];

        float determinant = m[0] * inverse[0] + m[1] * inverse[4] + m[2] * inverse[8]
                + m[3] * inverse[12];
        if (determinant == 0.0f) {
            return null;
        }
        float scale = 1.0f / determinant;
        for (int index = 0; index < 16; index++) {
            inverse[index] *= scale;
        }
        return inverse;
    }

    public static float[] copy(float[] source) {
        float[] target = new float[16];
        System.arraycopy(source, 0, target, 0, 16);
        return target;
    }
}
