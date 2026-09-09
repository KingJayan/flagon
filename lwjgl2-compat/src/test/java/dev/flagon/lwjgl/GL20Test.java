package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GL20Test {

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

    private static int program() {
        int vertex = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertex, "attribute vec3 a; void main() { gl_Position = vec4(a, 1.0); }");
        GL20.glCompileShader(vertex);

        int fragment = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragment, "void main() { gl_FragColor = vec4(1.0); }");
        GL20.glCompileShader(fragment);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        return program;
    }

    private static FloatBuffer floats(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    @Test
    void aShaderIsCompiledThroughItsOwnLifecycle() {
        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(shader, "void main() { gl_Position = vec4(0.0); }");
        GL20.glCompileShader(shader);

        assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
        assertEquals(List.of("createShader VERTEX " + shader), device.calls("createShader"));
        assertEquals(List.of("compileShader " + shader), device.calls("compileShader"));
    }

    @Test
    void aFailedCompileIsReportedRatherThanThrown() {
        device.compilesShaders = false;
        int shader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(shader, "void main() { gl_FragColor = vec4(1.0); }");
        GL20.glCompileShader(shader);

        assertEquals(GL11.GL_FALSE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
        assertEquals("compile failed", GL20.glGetShaderInfoLog(shader, 512));
    }

    @Test
    void shaderSourceReachesTheDeviceAsGlslEs() {
        int shader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(shader, "#version 120\nvoid main() { gl_FragColor = vec4(1.0); }");

        String source = device.sourceOf(shader);
        assertTrue(source.startsWith("precision mediump float;"), source);
        assertTrue(!source.contains("#version"), source);
    }

    @Test
    void theShaderTypeIsRememberedForTheStageItWasCreatedFor() {
        int shader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);

        assertEquals(GL20.GL_FRAGMENT_SHADER, GL20.glGetShaderi(shader, GL20.GL_SHADER_TYPE));
    }

    @Test
    void anUnknownShaderStageIsRejectedByName() {
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GL20.glCreateShader(0x8DD9));

        assertTrue(thrown.getMessage().contains("glCreateShader"), thrown.getMessage());
    }

    @Test
    void aProgramIsLinkedFromItsShaders() {
        int program = program();

        assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(program, GL20.GL_LINK_STATUS));
        assertEquals(2, device.calls("attachShader").size());
        assertEquals(List.of("linkProgram " + program), device.calls("linkProgram"));
    }

    @Test
    void aBoundProgramDrawsThroughItsOwnAttributes() {
        int program = program();
        GL20.glUseProgram(program);

        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, floats(0, 0, 0, 1, 0, 0, 1, 1, 0),
                GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);

        device.calls.clear();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        // nothing switched program: the one the application bound is the one that draws
        assertEquals(List.of(), device.calls("useProgram"));
        assertEquals(List.of("enableAttribute 0"), device.calls("enableAttribute"));
        assertEquals(List.of("attributePointer 0 3 FLOAT false 12 0"),
                device.calls("attributePointer"));
        assertEquals(List.of("drawArrays TRIANGLES 0 3"), device.calls("drawArrays"));
    }

    /** the fixed-function client arrays are separate state and must not leak into a program. */
    @Test
    void fixedFunctionArraysAreNotReadWhileAProgramIsBound() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, floats(0, 0, 0));

        GL20.glUseProgram(program());
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 3, false, 0, floats(1, 1, 1));

        device.calls.clear();
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        assertEquals(List.of("attributePointer 2 3 FLOAT false 12 0"),
                device.calls("attributePointer"));
    }

    @Test
    void returningToTheFixedFunctionPipelineRestoresIt() {
        GL20.glUseProgram(program());
        GL20.glUseProgram(0);

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, floats(0, 0, 0));
        device.calls.clear();
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);

        // the fixed-function shader binds itself back before drawing
        assertEquals(1, device.calls("useProgram").size());
        assertEquals(List.of("drawArrays POINTS 0 1"), device.calls("drawArrays"));
    }

    @Test
    void immediateModeIsRejectedWhileAProgramIsBound() {
        GL20.glUseProgram(program());

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glBegin(GL11.GL_TRIANGLES));

        assertTrue(thrown.getMessage().contains("glUseProgram(0)"), thrown.getMessage());
    }

    @Test
    void quadsAreStillExpandedForABoundProgram() {
        GL20.glUseProgram(program());
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, false, 0, floats(new float[12]));

        device.calls.clear();
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);

        assertEquals(List.of("drawElements TRIANGLES 6 UNSIGNED_INT 0"),
                device.calls("drawElements"));
    }

    @Test
    void anOffsetPointerWithoutABufferIsRejected() {
        GL20.glUseProgram(program());

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L));

        assertTrue(thrown.getMessage().contains("GL_ARRAY_BUFFER"), thrown.getMessage());
    }

    @Test
    void anAttributeBeyondTheHardwareLimitIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> GL20.glEnableVertexAttribArray(64));

        assertTrue(thrown.getMessage().contains("16"), thrown.getMessage());
    }

    @Test
    void uniformVectorsAreSetFromABuffer() {
        int location = GL20.glGetUniformLocation(program(), "u_color");
        GL20.glUniform3(location, floats(0.25f, 0.5f, 0.75f));

        assertEquals(List.of("uniformFloats " + location + " 3 1"), device.calls("uniformFloats"));
        assertArrayEquals(new float[] {0.25f, 0.5f, 0.75f}, device.lastUniformFloats);
    }

    @Test
    void aBufferOfSeveralVectorsSetsThemAll() {
        int location = GL20.glGetUniformLocation(program(), "u_offsets");
        GL20.glUniform2(location, floats(1, 2, 3, 4, 5, 6));

        assertEquals(List.of("uniformFloats " + location + " 2 3"), device.calls("uniformFloats"));
    }

    @Test
    void aMatrixUniformIsSetAtItsOwnSize() {
        int location = GL20.glGetUniformLocation(program(), "u_transform");
        GL20.glUniformMatrix4(location, false, floats(new float[16]));

        assertEquals(List.of("uniformMatrices " + location + " 4 false 1"),
                device.calls("uniformMatrices"));
    }

    /** gl reads a uniform buffer from its position, and leaves the position where it was. */
    @Test
    void aUniformBufferIsReadFromItsPositionAndNotConsumed() {
        FloatBuffer values = floats(9, 9, 0.25f, 0.5f, 0.75f);
        values.position(2);

        GL20.glUniform3(GL20.glGetUniformLocation(program(), "u_color"), values);

        assertEquals(2, values.position());
        assertArrayEquals(new float[] {0.25f, 0.5f, 0.75f}, device.lastUniformFloats);
    }

    @Test
    void anAttributeLocationCanBeBoundBeforeLinking() {
        int program = GL20.glCreateProgram();
        GL20.glBindAttribLocation(program, 3, "in_position");

        assertEquals(List.of("bindAttributeLocation " + program + " 3 in_position"),
                device.calls("bindAttributeLocation"));
    }

    @Test
    void aShaderCanBeDetachedAndDeletedAfterLinking() {
        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, shader);
        GL20.glDeleteShader(shader);

        assertEquals(List.of("detachShader " + program + " " + shader),
                device.calls("detachShader"));
        assertEquals(List.of("deleteShader " + shader), device.calls("deleteShader"));
    }
}
