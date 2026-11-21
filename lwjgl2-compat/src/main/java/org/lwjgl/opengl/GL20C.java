package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.ClientArray;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.GlslTranslator;
import dev.flagon.lwjgl.internal.UniformDefaults;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.ShaderStage;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * gl 2.0 shader objects, all of which is core profile
 */
public class GL20C {

    protected GL20C() {
    }

    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_SHADER_TYPE = 0x8B4F;
    public static final int GL_DELETE_STATUS = 0x8B80;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_VALIDATE_STATUS = 0x8B83;
    public static final int GL_INFO_LOG_LENGTH = 0x8B84;
    public static final int GL_ATTACHED_SHADERS = 0x8B85;
    public static final int GL_CURRENT_PROGRAM = 0x8B8D;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    public static final int GL_MAX_VARYING_FLOATS = 0x8B4B;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    static GLState state() {
        return GLState.get();
    }
    static GraphicsDevice device() {
        return GLState.get().device();
    }
    // shader objects

    public static int glCreateShader(int type) {
        ShaderStage stage = stage(type);
        int shader = device().createShader(stage);
        state().trackShader(shader, stage);
        return shader;
    }
    public static void glShaderSource(int shader, CharSequence source) {
        String written = source.toString();
        String translated = GlslTranslator.toGlslEs(written, state().shaderStage(shader));
        state().markFixedFunction(shader, GlslTranslator.readsFixedFunction(written));
        state().sourced(shader, translated, UniformDefaults.of(GlslTranslator.withoutComments(
                written)));
        device().shaderSource(shader, translated);
    }
    public static void glShaderSource(int shader, CharSequence[] sources) {
        StringBuilder joined = new StringBuilder();
        for (CharSequence source : sources) {
            joined.append(source);
        }
        glShaderSource(shader, joined);
    }
    /** the buffer holds the source as bytes, which is how gl has always taken it. */
    public static void glShaderSource(int shader, ByteBuffer source) {
        glShaderSource(shader, text(source));
    }
    public static void glCompileShader(int shader) {
        device().compileShader(shader);
    }
    public static void glDeleteShader(int shader) {
        device().deleteShader(shader);
        state().forgetShader(shader);
    }
    public static boolean glIsShader(int shader) {
        return device().isShader(shader);
    }
    public static int glGetShaderi(int shader, int name) {
        return glGetShader(shader, name);
    }
    public static int glGetShader(int shader, int name) {
        return switch (name) {
            case GL_SHADER_TYPE -> state().shaderStage(shader) == ShaderStage.VERTEX
                    ? GL_VERTEX_SHADER
                    : GL_FRAGMENT_SHADER;
            case GL_COMPILE_STATUS ->
                    device().isShaderCompiled(shader) ? GL11.GL_TRUE : GL11.GL_FALSE;
            case GL_DELETE_STATUS -> device().isShader(shader) ? GL11.GL_FALSE : GL11.GL_TRUE;
            // gl counts the terminating null; a caller sizes a buffer from this
            case GL_INFO_LOG_LENGTH -> logLength(device().shaderInfoLog(shader));
            default -> throw new UnsupportedOperationException(
                    "glGetShader does not emulate parameter 0x" + Integer.toHexString(name));
        };
    }
    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return clip(device().shaderInfoLog(shader), maxLength);
    }
    // program objects

    public static int glCreateProgram() {
        return device().createProgram();
    }
    public static void glAttachShader(int program, int shader) {
        state().attachShader(program, shader);
        device().attachShader(program, shader);
    }
    public static void glDetachShader(int program, int shader) {
        state().detachShader(program, shader);
        device().detachShader(program, shader);
    }
    public static void glLinkProgram(int program) {
        state().completeProgram(program);
        device().linkProgram(program);
        state().linkedProgram(program);
    }
    public static void glValidateProgram(int program) {
        device().validateProgram(program);
    }
    public static void glUseProgram(int program) {
        state().useProgram(program);
    }
    public static void glDeleteProgram(int program) {
        state().forgetProgram(program);
        device().deleteProgram(program);
    }
    public static boolean glIsProgram(int program) {
        return device().isProgram(program);
    }
    public static int glGetProgrami(int program, int name) {
        return glGetProgram(program, name);
    }
    public static int glGetProgram(int program, int name) {
        return switch (name) {
            case GL_LINK_STATUS -> device().isProgramLinked(program) ? GL11.GL_TRUE : GL11.GL_FALSE;
            case GL_VALIDATE_STATUS ->
                    device().isProgramValid(program) ? GL11.GL_TRUE : GL11.GL_FALSE;
            case GL_DELETE_STATUS -> device().isProgram(program) ? GL11.GL_FALSE : GL11.GL_TRUE;
            case GL_INFO_LOG_LENGTH -> logLength(device().programInfoLog(program));
            default -> throw new UnsupportedOperationException(
                    "glGetProgram does not emulate parameter 0x" + Integer.toHexString(name));
        };
    }
    public static String glGetProgramInfoLog(int program, int maxLength) {
        return clip(device().programInfoLog(program), maxLength);
    }
    // uniforms

    public static int glGetUniformLocation(int program, CharSequence name) {
        return device().uniformLocation(program, name.toString());
    }
    public static int glGetUniformLocation(int program, ByteBuffer name) {
        return glGetUniformLocation(program, text(name));
    }
    public static void glUniform1f(int location, float x) {
        device().uniform1f(location, x);
    }
    public static void glUniform2f(int location, float x, float y) {
        device().uniform2f(location, x, y);
    }
    public static void glUniform3f(int location, float x, float y, float z) {
        device().uniform3f(location, x, y, z);
    }
    public static void glUniform4f(int location, float x, float y, float z, float w) {
        device().uniform4f(location, x, y, z, w);
    }
    public static void glUniform1i(int location, int x) {
        device().uniform1i(location, x);
    }
    public static void glUniform2i(int location, int x, int y) {
        device().uniform2i(location, x, y);
    }
    public static void glUniform3i(int location, int x, int y, int z) {
        device().uniform3i(location, x, y, z);
    }
    public static void glUniform4i(int location, int x, int y, int z, int w) {
        device().uniform4i(location, x, y, z, w);
    }
    public static void glUniform1(int location, FloatBuffer values) {
        floats(location, 1, values);
    }
    public static void glUniform2(int location, FloatBuffer values) {
        floats(location, 2, values);
    }
    public static void glUniform3(int location, FloatBuffer values) {
        floats(location, 3, values);
    }
    public static void glUniform4(int location, FloatBuffer values) {
        floats(location, 4, values);
    }
    public static void glUniform1(int location, IntBuffer values) {
        ints(location, 1, values);
    }
    public static void glUniform2(int location, IntBuffer values) {
        ints(location, 2, values);
    }
    public static void glUniform3(int location, IntBuffer values) {
        ints(location, 3, values);
    }
    public static void glUniform4(int location, IntBuffer values) {
        ints(location, 4, values);
    }
    public static void glUniformMatrix2(int location, boolean transpose, FloatBuffer values) {
        matrices(location, 2, transpose, values);
    }
    public static void glUniformMatrix3(int location, boolean transpose, FloatBuffer values) {
        matrices(location, 3, transpose, values);
    }
    public static void glUniformMatrix4(int location, boolean transpose, FloatBuffer values) {
        matrices(location, 4, transpose, values);
    }
    static void floats(int location, int components, FloatBuffer values) {
        float[] data = read(values);
        device().uniformFloats(location, components, data, data.length / components);
    }
    static void ints(int location, int components, IntBuffer values) {
        int[] data = read(values);
        device().uniformInts(location, components, data, data.length / components);
    }
    static void matrices(int location, int size, boolean transpose, FloatBuffer values) {
        float[] data = read(values);
        device().uniformMatrices(location, size, transpose, data, data.length / (size * size));
    }
    // generic vertex attributes

    public static int glGetAttribLocation(int program, CharSequence name) {
        return device().attributeLocation(program, name.toString());
    }
    public static int glGetAttribLocation(int program, ByteBuffer name) {
        return glGetAttribLocation(program, text(name));
    }
    public static void glBindAttribLocation(int program, int index, CharSequence name) {
        requireAttribute(index);
        device().bindAttributeLocation(program, index, name.toString());
    }
    public static void glBindAttribLocation(int program, int index, ByteBuffer name) {
        glBindAttribLocation(program, index, text(name));
    }
    public static void glEnableVertexAttribArray(int index) {
        attribute(index).setEnabled(true);
    }
    public static void glDisableVertexAttribArray(int index) {
        attribute(index).setEnabled(false);
    }
    public static void glVertexAttribPointer(int index, int size, boolean normalized, int stride,
                                             FloatBuffer pointer) {
        attribute(index).pointAt(pointer, size, AttributeType.FLOAT, normalized, stride);
    }
    public static void glVertexAttribPointer(int index, int size, boolean unsigned,
                                             boolean normalized, int stride, ByteBuffer pointer) {
        AttributeType type = unsigned ? AttributeType.UNSIGNED_BYTE : AttributeType.BYTE;
        attribute(index).pointAt(pointer, size, type, normalized, stride);
    }
    public static void glVertexAttribPointer(int index, int size, boolean unsigned,
                                             boolean normalized, int stride, ShortBuffer pointer) {
        AttributeType type = unsigned ? AttributeType.UNSIGNED_SHORT : AttributeType.SHORT;
        attribute(index).pointAt(pointer, size, type, normalized, stride);
    }
    public static void glVertexAttribPointer(int index, int size, boolean unsigned,
                                             boolean normalized, int stride, IntBuffer pointer) {
        AttributeType type = unsigned ? AttributeType.UNSIGNED_INT : AttributeType.INT;
        attribute(index).pointAt(pointer, size, type, normalized, stride);
    }
    /** the offset form names a location inside the buffer bound to GL_ARRAY_BUFFER. */
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, long offset) {
        int buffer = state().boundBuffer(BufferTarget.ARRAY);
        if (buffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glVertexAttribPointer was given an offset with no GL_ARRAY_BUFFER bound. that "
                            + "form names a location inside a buffer object; pass a Buffer instead "
                            + "to read from client memory");
        }
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("glVertexAttribPointer: offset out of range: "
                    + offset);
        }
        attribute(index).pointAt(buffer, (int) offset, size, componentType(type), normalized,
                stride);
    }
    static ClientArray attribute(int index) {
        requireAttribute(index);
        return state().attributes().slot(index);
    }
    static void requireAttribute(int index) {
        int max = state().attributes().count();
        if (index < 0 || index >= max) {
            throw new IllegalArgumentException(
                    "vertex attribute " + index + " is outside the " + max + " this context "
                            + "supports; GL_MAX_VERTEX_ATTRIBS is a hardware limit");
        }
    }
    // translation

    static ShaderStage stage(int type) {
        return switch (type) {
            case GL_VERTEX_SHADER -> ShaderStage.VERTEX;
            case GL_FRAGMENT_SHADER -> ShaderStage.FRAGMENT;
            default -> throw new UnsupportedOperationException(
                    "glCreateShader emulates GL_VERTEX_SHADER and GL_FRAGMENT_SHADER only, got 0x"
                            + Integer.toHexString(type));
        };
    }
    static AttributeType componentType(int type) {
        return switch (type) {
            case GL11.GL_BYTE -> AttributeType.BYTE;
            case GL11.GL_UNSIGNED_BYTE -> AttributeType.UNSIGNED_BYTE;
            case GL11.GL_SHORT -> AttributeType.SHORT;
            case GL11.GL_UNSIGNED_SHORT -> AttributeType.UNSIGNED_SHORT;
            case GL11.GL_INT -> AttributeType.INT;
            case GL11.GL_UNSIGNED_INT -> AttributeType.UNSIGNED_INT;
            case GL11.GL_FLOAT -> AttributeType.FLOAT;
            default -> throw new UnsupportedOperationException(
                    "glVertexAttribPointer does not emulate component type 0x"
                            + Integer.toHexString(type));
        };
    }
    static int logLength(String log) {
        return log.isEmpty() ? 0 : log.length() + 1;
    }
    static String clip(String log, int maxLength) {
        return log.length() <= maxLength ? log : log.substring(0, maxLength);
    }
    static String text(ByteBuffer buffer) {
        StringBuilder characters = new StringBuilder(buffer.remaining());
        for (int at = buffer.position(); at < buffer.limit(); at++) {
            byte value = buffer.get(at);
            if (value == 0) {
                break;
            }
            characters.append((char) (value & 0xFF));
        }
        return characters.toString();
    }
    // gl reads from position to limit and leaves the buffer where it found it
    static float[] read(FloatBuffer values) {
        float[] data = new float[values.remaining()];
        for (int at = 0; at < data.length; at++) {
            data[at] = values.get(values.position() + at);
        }
        return data;
    }
    static int[] read(IntBuffer values) {
        int[] data = new int[values.remaining()];
        for (int at = 0; at < data.length; at++) {
            data[at] = values.get(values.position() + at);
        }
        return data;
    }
}
