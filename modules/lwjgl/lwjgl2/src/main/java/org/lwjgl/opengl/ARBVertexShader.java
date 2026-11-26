package org.lwjgl.opengl;

/**
 * the ARB_vertex_shader names for what gl 2.0 took into core
 */
public final class ARBVertexShader {

    public static final int GL_VERTEX_SHADER_ARB = 0x8B31;
    public static final int GL_MAX_VERTEX_ATTRIBS_ARB = 0x8869;

    private ARBVertexShader() {
    }

    public static int glGetAttribLocationARB(int program, CharSequence name) {
        return GL20.glGetAttribLocation(program, name);
    }

    public static void glBindAttribLocationARB(int program, int index, CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    public static void glEnableVertexAttribArrayARB(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    public static void glDisableVertexAttribArrayARB(int index) {
        GL20.glDisableVertexAttribArray(index);
    }
}
