package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flagon.lwjgl.internal.GLState;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;

class CoreProfileTest {

    private RecordingGraphicsDevice device;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        GLState.attach(device);
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    @Test
    void bindsAVertexArray() {
        int array = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(array);
        assertEquals(array, GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING));
        assertTrue(GL30.glIsVertexArray(array));
        GL30.glDeleteVertexArrays(array);
        assertEquals(0, GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING));
        assertFalse(GL30.glIsVertexArray(array));
    }

    @Test
    void reportsThreePointTwo() {
        assertEquals(3, GL30.glGetInteger(GL30.GL_MAJOR_VERSION));
        assertEquals(2, GL30.glGetInteger(GL30.GL_MINOR_VERSION));
    }

    @Test
    void splitsTheExtensionStringIntoIndexedNames() {
        device.extensions = "EXT_one EXT_two";
        assertEquals(2, GL30.glGetInteger(GL30.GL_NUM_EXTENSIONS));
        assertEquals("EXT_one", GL30.glGetStringi(GL30.GL_EXTENSIONS, 0));
        assertEquals("EXT_two", GL30.glGetStringi(GL30.GL_EXTENSIONS, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> GL30.glGetStringi(GL30.GL_EXTENSIONS, 2));
    }

    @Test
    void capabilitiesCarryTheReportedExtensions() {
        device.extensions = "EXT_one EXT_two";
        GLCapabilities capabilities = GL.createCapabilities();
        assertTrue(capabilities.OpenGL32);
        assertTrue(capabilities.hasExtension("EXT_two"));
        assertFalse(capabilities.hasExtension("EXT_three"));
        assertEquals(capabilities, GL.getCapabilities());
    }

    @Test
    void pointsAnAttributeAtIntegersWithoutConverting() {
        int program = program();
        int buffer = device.createBuffer();
        GL30.glBindVertexArray(GL30.glGenVertexArrays());
        bindArrayBuffer(buffer);
        GL30.glVertexAttribIPointer(0, 2, GL_INT, 8, 0L);
        GLState.get().attributes().slot(0).setEnabled(true);
        GL31.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 4);
        assertTrue(recorded("integerAttributePointer 0 2 INT 8 0"));
        assertTrue(recorded("drawArraysInstanced TRIANGLES 0 3 4"));
        assertEquals(program, GLState.get().program());
    }

    @Test
    void stepsAnAttributePerInstance() {
        program();
        int buffer = device.createBuffer();
        bindArrayBuffer(buffer);
        GL30.glVertexAttribIPointer(1, 1, GL_INT, 4, 0L);
        GLState.get().attributes().slot(1).setEnabled(true);
        GL33.glVertexAttribDivisor(1, 1);
        GL31.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 2);
        assertTrue(recorded("attributeDivisor 1 1"));
    }

    @Test
    void refusesANegativeDivisor() {
        assertThrows(IllegalArgumentException.class, () -> GL33.glVertexAttribDivisor(0, -1));
    }

    @Test
    void refusesAnInstancedDrawWithNoProgram() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL31.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 2));
    }

    @Test
    void refusesABaseVertexItCannotHonour() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL32.glDrawElementsBaseVertex(GL_TRIANGLES, 3, GL_UNSIGNED_SHORT, 0L, 4));
    }

    @Test
    void refusesAFragmentOutputItCannotRename() {
        assertThrows(UnsupportedOperationException.class,
                () -> GL30.glBindFragDataLocation(1, 1, "colour"));
    }

    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_INT = 0x1404;
    private static final int GL_UNSIGNED_SHORT = 0x1403;

    private int program() {
        int program = device.createProgram();
        GLState.get().useProgram(program);
        return program;
    }

    private void bindArrayBuffer(int buffer) {
        GLState.get().bindBuffer(dev.flagon.platform.graphics.BufferTarget.ARRAY, buffer);
    }

    private boolean recorded(String call) {
        List<String> calls = device.calls;
        return calls.contains(call);
    }
}
