package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.Matrix4;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Matrix4Test {

    private static final float EPSILON = 1.0e-5f;

    /** applies the matrix to a point the way the vertex shader does. */
    private static float[] transform(float[] m, float x, float y, float z) {
        return new float[] {
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14],
        };
    }

    @Test
    void identityLeavesPointsAlone() {
        assertArrayEquals(new float[] {2.0f, 3.0f, 4.0f},
                transform(Matrix4.identity(), 2.0f, 3.0f, 4.0f), EPSILON);
    }

    @Test
    void translateMovesPoints() {
        float[] m = Matrix4.identity();
        Matrix4.translate(m, 1.0f, 2.0f, 3.0f);
        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, transform(m, 0.0f, 0.0f, 0.0f), EPSILON);
        assertArrayEquals(new float[] {11.0f, 2.0f, 3.0f}, transform(m, 10.0f, 0.0f, 0.0f),
                EPSILON);
    }

    @Test
    void scaleThenTranslateAppliesInGlOrder() {
        // glScalef then glTranslatef: the translation happens in the scaled space
        float[] m = Matrix4.identity();
        Matrix4.scale(m, 2.0f, 2.0f, 2.0f);
        Matrix4.translate(m, 1.0f, 0.0f, 0.0f);
        assertArrayEquals(new float[] {2.0f, 0.0f, 0.0f}, transform(m, 0.0f, 0.0f, 0.0f), EPSILON);
    }

    @Test
    void rotateTurnsAboutZ() {
        float[] m = Matrix4.identity();
        Matrix4.rotate(m, 90.0f, 0.0f, 0.0f, 1.0f);
        assertArrayEquals(new float[] {0.0f, 1.0f, 0.0f}, transform(m, 1.0f, 0.0f, 0.0f), EPSILON);
    }

    @Test
    void rotateAboutADegenerateAxisIsIgnored() {
        float[] m = Matrix4.identity();
        Matrix4.rotate(m, 90.0f, 0.0f, 0.0f, 0.0f);
        assertArrayEquals(Matrix4.identity(), m, EPSILON);
    }

    @Test
    void orthoMapsTheViewportToClipSpace() {
        float[] m = Matrix4.identity();
        Matrix4.ortho(m, 0.0f, 800.0f, 600.0f, 0.0f, -1.0f, 1.0f);

        // top-left of a 2d screen projection lands at clip-space (-1, 1)
        float[] topLeft = transform(m, 0.0f, 0.0f, 0.0f);
        assertEquals(-1.0f, topLeft[0], EPSILON);
        assertEquals(1.0f, topLeft[1], EPSILON);

        float[] bottomRight = transform(m, 800.0f, 600.0f, 0.0f);
        assertEquals(1.0f, bottomRight[0], EPSILON);
        assertEquals(-1.0f, bottomRight[1], EPSILON);
    }

    @Test
    void frustumPutsTheNearPlaneAtMinusOne() {
        float[] m = Matrix4.identity();
        Matrix4.frustum(m, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 100.0f);
        // w is -z for a perspective matrix, so a point on the near plane normalises to z = -1
        float[] onNearPlane = transform(m, 0.0f, 0.0f, -1.0f);
        assertEquals(-1.0f, onNearPlane[2] / 1.0f, 1.0e-4f);
    }

    @Test
    void copyIsIndependentOfTheOriginal() {
        float[] original = Matrix4.identity();
        float[] copy = Matrix4.copy(original);
        Matrix4.translate(original, 5.0f, 0.0f, 0.0f);
        assertArrayEquals(Matrix4.identity(), copy, EPSILON);
    }
}
