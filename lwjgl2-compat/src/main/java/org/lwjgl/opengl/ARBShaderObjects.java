package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * the ARB_shader_objects names for what gl 2.0 took into core.
 *
 * <p>the extension holds programs and shaders in one handle space, so the calls that take either
 * one ask which of the two the handle is and go to the gl 2.0 call for it.
 */
public final class ARBShaderObjects {

    public static final int GL_OBJECT_TYPE_ARB = 0x8B4E;
    public static final int GL_OBJECT_SUBTYPE_ARB = 0x8B4F;
    public static final int GL_OBJECT_DELETE_STATUS_ARB = 0x8B80;
    public static final int GL_OBJECT_COMPILE_STATUS_ARB = 0x8B81;
    public static final int GL_OBJECT_LINK_STATUS_ARB = 0x8B82;
    public static final int GL_OBJECT_VALIDATE_STATUS_ARB = 0x8B83;
    public static final int GL_OBJECT_INFO_LOG_LENGTH_ARB = 0x8B84;
    public static final int GL_OBJECT_ATTACHED_OBJECTS_ARB = 0x8B85;

    private ARBShaderObjects() {
    }

    public static int glCreateProgramObjectARB() {
        return GL20.glCreateProgram();
    }

    public static int glCreateShaderObjectARB(int type) {
        return GL20.glCreateShader(type);
    }

    public static void glShaderSourceARB(int shader, ByteBuffer source) {
        GL20.glShaderSource(shader, source);
    }

    public static void glShaderSourceARB(int shader, CharSequence source) {
        GL20.glShaderSource(shader, source);
    }

    public static void glCompileShaderARB(int shader) {
        GL20.glCompileShader(shader);
    }

    public static void glAttachObjectARB(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    public static void glDetachObjectARB(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    public static void glLinkProgramARB(int program) {
        GL20.glLinkProgram(program);
    }

    public static void glValidateProgramARB(int program) {
        GL20.glValidateProgram(program);
    }

    public static void glUseProgramObjectARB(int program) {
        GL20.glUseProgram(program);
    }

    public static void glDeleteObjectARB(int object) {
        if (GL20.glIsShader(object)) {
            GL20.glDeleteShader(object);
            return;
        }
        GL20.glDeleteProgram(object);
    }

    public static int glGetObjectParameteriARB(int object, int name) {
        return GL20.glIsShader(object)
                ? GL20.glGetShaderi(object, name)
                : GL20.glGetProgrami(object, name);
    }

    public static String glGetInfoLogARB(int object, int maxLength) {
        return GL20.glIsShader(object)
                ? GL20.glGetShaderInfoLog(object, maxLength)
                : GL20.glGetProgramInfoLog(object, maxLength);
    }

    public static int glGetUniformLocationARB(int program, CharSequence name) {
        return GL20.glGetUniformLocation(program, name);
    }

    public static void glUniform1iARB(int location, int x) {
        GL20.glUniform1i(location, x);
    }

    public static void glUniform1fARB(int location, float x) {
        GL20.glUniform1f(location, x);
    }

    public static void glUniform1ARB(int location, FloatBuffer values) {
        GL20.glUniform1(location, values);
    }

    public static void glUniform1ARB(int location, IntBuffer values) {
        GL20.glUniform1(location, values);
    }

    public static void glUniform2ARB(int location, FloatBuffer values) {
        GL20.glUniform2(location, values);
    }

    public static void glUniform2ARB(int location, IntBuffer values) {
        GL20.glUniform2(location, values);
    }

    public static void glUniform3ARB(int location, FloatBuffer values) {
        GL20.glUniform3(location, values);
    }

    public static void glUniform3ARB(int location, IntBuffer values) {
        GL20.glUniform3(location, values);
    }

    public static void glUniform4ARB(int location, FloatBuffer values) {
        GL20.glUniform4(location, values);
    }

    public static void glUniform4ARB(int location, IntBuffer values) {
        GL20.glUniform4(location, values);
    }

    public static void glUniformMatrix2ARB(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix2(location, transpose, values);
    }

    public static void glUniformMatrix3ARB(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix3(location, transpose, values);
    }

    public static void glUniformMatrix4ARB(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix4(location, transpose, values);
    }
}
