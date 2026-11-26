package dev.flagon.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorMathTest {

    private static void assertClose(float expected, float actual) {
        assertEquals(expected, actual, 1.0e-5f);
    }

    @Test
    void aNewMatrixIsTheIdentity() {
        Matrix4f matrix = new Matrix4f();
        assertClose(1.0f, matrix.m00);
        assertClose(1.0f, matrix.m33);
        assertClose(0.0f, matrix.m01);
        assertClose(1.0f, matrix.determinant());
    }

    @Test
    void aTranslationMovesThePointItTransforms() {
        Matrix4f move = new Matrix4f();
        move.m30 = 2.0f;
        move.m31 = 3.0f;
        Vector4f moved = Matrix4f.transform(move, new Vector4f(1.0f, 1.0f, 0.0f, 1.0f), null);
        assertClose(3.0f, moved.x);
        assertClose(4.0f, moved.y);
        assertClose(1.0f, moved.w);
    }

    @Test
    void aProductAppliesTheRightMatrixFirst() {
        Matrix4f move = new Matrix4f();
        move.m30 = 1.0f;
        Matrix4f stretch = new Matrix4f();
        stretch.m00 = 2.0f;
        Matrix4f both = Matrix4f.mul(move, stretch, null);
        Vector4f point = Matrix4f.transform(both, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f), null);
        assertClose(3.0f, point.x);
    }

    @Test
    void aQuarterTurnAboutZSendsXToY() {
        Matrix4f turned = Matrix4f.rotate((float) Math.PI / 2.0f, new Vector3f(0.0f, 0.0f, 1.0f),
                new Matrix4f(), null);
        Vector4f point = Matrix4f.transform(turned, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f), null);
        assertClose(0.0f, point.x);
        assertClose(1.0f, point.y);
    }

    @Test
    void anInverseUndoesTheMatrixItCameFrom() {
        Matrix4f matrix = new Matrix4f();
        matrix.m00 = 2.0f;
        matrix.m11 = 4.0f;
        matrix.m30 = 5.0f;
        Vector4f point = new Vector4f(1.0f, 1.0f, 0.0f, 1.0f);
        Vector4f moved = Matrix4f.transform(matrix, point, null);
        Vector4f back = Matrix4f.transform(matrix.invert(), moved, null);
        assertClose(1.0f, back.x);
        assertClose(1.0f, back.y);
    }

    @Test
    void aMatrixWithNoInverseAnswersNull() {
        assertNull(new Matrix4f().setZero().invert());
    }

    @Test
    void theCrossProductPointsAwayFromBothVectors() {
        Vector3f across = Vector3f.cross(new Vector3f(1.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f), null);
        assertClose(0.0f, across.x);
        assertClose(0.0f, across.y);
        assertClose(1.0f, across.z);
    }

    @Test
    void aVectorAddsSubtractsAndScales() {
        Vector3f sum = Vector3f.add(new Vector3f(1.0f, 2.0f, 3.0f),
                new Vector3f(1.0f, 1.0f, 1.0f), null);
        assertClose(2.0f, sum.x);
        Vector3f difference = Vector3f.sub(sum, new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f());
        assertClose(1.0f, difference.x);
        assertClose(14.0f, new Vector3f(1.0f, 2.0f, 3.0f).lengthSquared());
        assertClose(6.0f, Vector3f.dot(new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(1.0f, 2.0f, 3.0f)));
    }

    @Test
    void normalisingLeavesAVectorOfLengthOne() {
        Vector3f direction = new Vector3f(0.0f, 3.0f, 4.0f);
        direction.normalise();
        assertClose(1.0f, direction.length());
        assertClose(0.8f, direction.z);
    }

    @Test
    void twoVectorsOfTheSameNumbersAreEqual() {
        assertTrue(new Vector3f(1.0f, 2.0f, 3.0f).equals(new Vector3f(1.0f, 2.0f, 3.0f)));
        assertEquals(new Vector3f(1.0f, 2.0f, 3.0f).hashCode(),
                new Vector3f(1.0f, 2.0f, 3.0f).hashCode());
    }
}
