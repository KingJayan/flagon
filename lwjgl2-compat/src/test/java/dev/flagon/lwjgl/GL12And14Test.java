package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GL12And14Test {

    private RecordingGraphicsDevice device;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        GLState.attach(device);
        device.calls.clear();
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    private static FloatBuffer floats(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    private static IntBuffer ints(int... values) {
        IntBuffer buffer = BufferUtils.createIntBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    private void bindPositions(int count) {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, floats(new float[count * 3]));
    }

    @Test
    void drawRangeElementsDrawsWhatDrawElementsWould() {
        bindPositions(4);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, GL15.glGenBuffers());
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ints(0, 1, 2), GL15.GL_STATIC_DRAW);

        GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 0, 2, 3, GL11.GL_UNSIGNED_INT, 0L);

        assertEquals(List.of("drawElements TRIANGLES 3 UNSIGNED_INT 0"),
                device.calls("drawElements"));
    }

    @Test
    void aBackwardsRangeIsRefusedRatherThanDrawn() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 8, 2, 3, GL11.GL_UNSIGNED_INT,
                        0L));
        assertTrue(failure.getMessage().contains("below start"), failure.getMessage());
    }

    @Test
    void blendFuncSeparateReachesTheDeviceWithAllFourFactors() {
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);

        assertEquals(List.of("blendFuncSeparate SRC_ALPHA ONE_MINUS_SRC_ALPHA ONE ZERO"),
                device.calls("blendFuncSeparate"));
        assertEquals(GL11.GL_ONE, GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA));
        assertEquals(GL11.GL_ZERO, GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
    }

    @Test
    void theConstantBlendFactorsAreAvailableWithTheircolor() {
        GL11.glBlendFunc(GL14.GL_CONSTANT_COLOR, GL14.GL_ONE_MINUS_CONSTANT_ALPHA);
        GL14.glBlendColor(0.25f, 0.5f, 0.75f, 1.0f);

        assertEquals(List.of("blendFunc CONSTANT_COLOR ONE_MINUS_CONSTANT_ALPHA"),
                device.calls("blendFunc"));
        assertEquals(List.of("blendColor 0.25 0.5 0.75 1.0"), device.calls("blendColor"));
        assertEquals(GL14.GL_CONSTANT_COLOR, GL11.glGetInteger(GL11.GL_BLEND_SRC));
    }

    @Test
    void blendEquationsReachTheDeviceAndAnswerAQuery() {
        GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);

        assertEquals(List.of("blendEquation REVERSE_SUBTRACT"), device.calls("blendEquation"));
        assertEquals(GL14.GL_FUNC_REVERSE_SUBTRACT, GL11.glGetInteger(GL14.GL_BLEND_EQUATION_RGB));
        assertEquals(GL14.GL_FUNC_REVERSE_SUBTRACT,
                GL11.glGetInteger(GL14.GL_BLEND_EQUATION_ALPHA));
    }

    @Test
    void aSeparateEquationLeavesTheTwoChannelsDifferent() {
        GL14.glBlendEquationSeparate(GL14.GL_MIN, GL14.GL_MAX);

        assertEquals(List.of("blendEquationSeparate MIN MAX"),
                device.calls("blendEquationSeparate"));
        assertEquals(GL14.GL_MIN, GL11.glGetInteger(GL14.GL_BLEND_EQUATION_RGB));
        assertEquals(GL14.GL_MAX, GL11.glGetInteger(GL14.GL_BLEND_EQUATION_ALPHA));
    }

    @Test
    void anUnknownEquationNamesItself() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GL14.glBlendEquation(0x1234));
        assertTrue(failure.getMessage().contains("blend equation"), failure.getMessage());
    }

    @Test
    void multiDrawArraysIssuesOneDrawPerRange() {
        bindPositions(9);

        GL14.glMultiDrawArrays(GL11.GL_TRIANGLES, ints(0, 3), ints(3, 6));

        assertEquals(List.of("drawArrays TRIANGLES 0 3", "drawArrays TRIANGLES 3 6"),
                device.calls("drawArrays"));
    }

    @Test
    void multiDrawArraysWantsAsManyCountsAsStarts() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GL14.glMultiDrawArrays(GL11.GL_TRIANGLES, ints(0, 3), ints(3)));
        assertTrue(failure.getMessage().contains("glMultiDrawArrays"), failure.getMessage());
    }

    @Test
    void thePointSizeRangeClampsWhatTheShaderIsTold() {
        GL14.glPointParameterf(GL14.GL_POINT_SIZE_MAX, 4.0f);
        GL11.glPointSize(16.0f);
        bindPositions(1);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertTrue(device.calls.stream().anyMatch(call -> call.startsWith("uniform1f")
                && call.endsWith(" 4.0")), device.calls.toString());
    }

    @Test
    void pointAttenuationSaysWhyItIsNotHere() {
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL14.glPointParameter(GL14.GL_POINT_DISTANCE_ATTENUATION,
                        floats(1.0f, 0.0f, 0.0f)));
        assertTrue(failure.getMessage().contains("vertex shader"), failure.getMessage());
    }

    @Test
    void aDisplayListReplaysTheBlendingCallsInsideIt() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL14.glBlendEquation(GL14.GL_FUNC_SUBTRACT);
        GL14.glBlendColor(1.0f, 0.0f, 0.0f, 1.0f);
        GL11.glEndList();
        assertTrue(device.calls("blendEquation").isEmpty());

        GL11.glCallList(list);

        assertEquals(List.of("blendEquation SUBTRACT"), device.calls("blendEquation"));
        assertEquals(List.of("blendColor 1.0 0.0 0.0 1.0"), device.calls("blendColor"));
    }
}
