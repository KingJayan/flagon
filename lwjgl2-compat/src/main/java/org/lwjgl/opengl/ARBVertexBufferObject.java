package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * the ARB_vertex_buffer_object names for what gl 1.5 took into core
 */
public final class ARBVertexBufferObject {

    public static final int GL_ARRAY_BUFFER_ARB = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER_ARB = 0x8893;
    public static final int GL_STREAM_DRAW_ARB = 0x88E0;
    public static final int GL_STATIC_DRAW_ARB = 0x88E4;
    public static final int GL_DYNAMIC_DRAW_ARB = 0x88E8;

    private ARBVertexBufferObject() {
    }

    public static int glGenBuffersARB() {
        return GL15.glGenBuffers();
    }

    public static void glGenBuffersARB(IntBuffer buffers) {
        GL15.glGenBuffers(buffers);
    }

    public static void glBindBufferARB(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    public static void glDeleteBuffersARB(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    public static void glDeleteBuffersARB(IntBuffer buffers) {
        GL15.glDeleteBuffers(buffers);
    }

    public static void glBufferDataARB(int target, ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    public static void glBufferDataARB(int target, FloatBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    public static void glBufferDataARB(int target, IntBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    public static void glBufferSubDataARB(int target, long offset, ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }
}
