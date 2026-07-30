package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.Buffers;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** buffer objects. */
public final class GL15 {

    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;

    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_STREAM_COPY = 0x88E2;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_STATIC_READ = 0x88E5;
    public static final int GL_STATIC_COPY = 0x88E6;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_DYNAMIC_READ = 0x88E9;
    public static final int GL_DYNAMIC_COPY = 0x88EA;

    private GL15() {
    }

    public static int glGenBuffers() {
        return GLState.get().createBuffer();
    }

    public static void glGenBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, glGenBuffers());
        }
    }

    public static void glDeleteBuffers(int buffer) {
        GLState.get().deleteBuffer(buffer);
    }

    public static void glDeleteBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            glDeleteBuffers(buffers.get(i));
        }
    }

    public static void glBindBuffer(int target, int buffer) {
        GLState.get().bindBuffer(target(target, "glBindBuffer"), buffer);
    }

    /** reserves storage without filling it, for a buffer written later by glBufferSubData. */
    public static void glBufferData(int target, long size, int usage) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("buffer size out of range: " + size);
        }
        GLState.get().device().bufferData(target(target, "glBufferData"), (int) size,
                usage(usage, "glBufferData"));
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        upload(target, data, usage);
    }

    public static void glBufferData(int target, ShortBuffer data, int usage) {
        upload(target, data, usage);
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        upload(target, data, usage);
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        upload(target, data, usage);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer data) {
        replace(target, offset, data);
    }

    public static void glBufferSubData(int target, long offset, ShortBuffer data) {
        replace(target, offset, data);
    }

    public static void glBufferSubData(int target, long offset, IntBuffer data) {
        replace(target, offset, data);
    }

    public static void glBufferSubData(int target, long offset, FloatBuffer data) {
        replace(target, offset, data);
    }

    // gl uploads the elements between position and limit, and the layer below wants flat bytes
    private static void upload(int target, Buffer data, int usage) {
        byte[] bytes = Buffers.readRemaining(data);
        GLState.get().device().bufferData(target(target, "glBufferData"), bytes, bytes.length,
                usage(usage, "glBufferData"));
    }

    private static void replace(int target, long offset, Buffer data) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("buffer offset out of range: " + offset);
        }
        byte[] bytes = Buffers.readRemaining(data);
        GLState.get().device().bufferSubData(target(target, "glBufferSubData"), (int) offset,
                bytes, bytes.length);
    }

    private static BufferTarget target(int target, String call) {
        return switch (target) {
            case GL_ARRAY_BUFFER -> BufferTarget.ARRAY;
            case GL_ELEMENT_ARRAY_BUFFER -> BufferTarget.ELEMENT;
            default -> throw new UnsupportedOperationException(
                    call + " emulates GL_ARRAY_BUFFER and GL_ELEMENT_ARRAY_BUFFER only, got 0x"
                            + Integer.toHexString(target));
        };
    }

    /**
     * webgl has no read or copy usage: nothing on the page can map a buffer back, so the read
     * half of each hint has no meaning. the draw frequency is the part that guides the driver,
     * and it is the part that survives.
     */
    private static BufferUsage usage(int usage, String call) {
        return switch (usage) {
            case GL_STREAM_DRAW, GL_STREAM_READ, GL_STREAM_COPY -> BufferUsage.STREAM;
            case GL_STATIC_DRAW, GL_STATIC_READ, GL_STATIC_COPY -> BufferUsage.STATIC;
            case GL_DYNAMIC_DRAW, GL_DYNAMIC_READ, GL_DYNAMIC_COPY -> BufferUsage.DYNAMIC;
            default -> throw new IllegalArgumentException(
                    call + ": unknown usage 0x" + Integer.toHexString(usage));
        };
    }
}
