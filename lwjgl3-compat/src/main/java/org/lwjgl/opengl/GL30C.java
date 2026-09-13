package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.Framebuffers;
import dev.flagon.lwjgl.internal.GLEnums;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsInfo;

import java.nio.IntBuffer;

/**
 * gl 3.0, all of which is core profile
 */
public class GL30C {

    protected GL30C() {
    }

    public static final int GL_MAJOR_VERSION = 0x821B;
    public static final int GL_MINOR_VERSION = 0x821C;
    public static final int GL_NUM_EXTENSIONS = 0x821D;
    public static final int GL_CONTEXT_FLAGS = 0x821E;
    public static final int GL_CONTEXT_CORE_PROFILE_BIT = 0x00000001;
    public static final int GL_CONTEXT_COMPATIBILITY_PROFILE_BIT = 0x00000002;
    public static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    public static final int GL_RENDERBUFFER = 0x8D41;
    public static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_DEPTH_STENCIL = 0x84F9;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_STENCIL_INDEX8 = 0x8D48;
    public static final int GL_RGBA32F = 0x8814;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_R8 = 0x8229;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_HALF_FLOAT = 0x140B;
    public static int glGenFramebuffers() {
        return Framebuffers.create();
    }
    public static void glGenFramebuffers(IntBuffer framebuffers) {
        Framebuffers.createInto(framebuffers);
    }
    public static void glBindFramebuffer(int target, int framebuffer) {
        Framebuffers.bind(target, framebuffer, "glBindFramebuffer");
    }
    public static void glDeleteFramebuffers(int framebuffer) {
        Framebuffers.delete(framebuffer);
    }
    public static void glDeleteFramebuffers(IntBuffer framebuffers) {
        Framebuffers.deleteAll(framebuffers);
    }
    public static boolean glIsFramebuffer(int framebuffer) {
        return Framebuffers.isFramebuffer(framebuffer);
    }
    public static void glFramebufferTexture2D(int target, int attachment, int textureTarget,
                                              int texture, int level) {
        Framebuffers.attachTexture(target, attachment, textureTarget, texture, level,
                "glFramebufferTexture2D");
    }
    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                                 int renderbuffer) {
        Framebuffers.attachRenderbuffer(target, attachment, renderbufferTarget, renderbuffer,
                "glFramebufferRenderbuffer");
    }
    public static int glCheckFramebufferStatus(int target) {
        return Framebuffers.status(target, "glCheckFramebufferStatus");
    }
    public static int glGenRenderbuffers() {
        return Framebuffers.createRenderbuffer();
    }
    public static void glGenRenderbuffers(IntBuffer renderbuffers) {
        Framebuffers.createRenderbuffersInto(renderbuffers);
    }
    public static void glBindRenderbuffer(int target, int renderbuffer) {
        Framebuffers.bindRenderbuffer(target, renderbuffer, "glBindRenderbuffer");
    }
    public static void glDeleteRenderbuffers(int renderbuffer) {
        Framebuffers.deleteRenderbuffer(renderbuffer);
    }
    public static void glDeleteRenderbuffers(IntBuffer renderbuffers) {
        Framebuffers.deleteRenderbuffers(renderbuffers);
    }
    public static void glRenderbufferStorage(int target, int format, int width, int height) {
        Framebuffers.renderbufferStorage(target, format, width, height, "glRenderbufferStorage");
    }
    public static void glGenerateMipmap(int target) {
        if (target != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    "glGenerateMipmap emulates GL_TEXTURE_2D only, got 0x"
                            + Integer.toHexString(target));
        }
        GLState.get().device().generateMipmap();
    }
    public static int glGenVertexArrays() {
        return GLState.get().createVertexArray();
    }
    public static void glGenVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            arrays.put(i, glGenVertexArrays());
        }
    }
    public static void glBindVertexArray(int array) {
        GLState.get().bindVertexArray(array);
    }
    public static void glDeleteVertexArrays(int array) {
        GLState.get().deleteVertexArray(array);
    }
    public static void glDeleteVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            glDeleteVertexArrays(arrays.get(i));
        }
    }
    public static boolean glIsVertexArray(int array) {
        return array != 0 && GLState.get().boundVertexArray() == array;
    }
    /** the core profile replaced the one space-separated GL_EXTENSIONS string with this. */
    public static String glGetStringi(int name, int index) {
        if (name != GL_EXTENSIONS) {
            throw new UnsupportedOperationException(
                    "glGetStringi emulates GL_EXTENSIONS only, got 0x" + Integer.toHexString(name));
        }
        String[] extensions = extensions();
        if (index < 0 || index >= extensions.length) {
            throw new IndexOutOfBoundsException(
                    "extension " + index + " of " + extensions.length);
        }
        return extensions[index];
    }
    public static int glGetInteger(int name) {
        return switch (name) {
            case GL_MAJOR_VERSION -> 3;
            case GL_MINOR_VERSION -> 2;
            case GL_NUM_EXTENSIONS -> extensions().length;
            case GL_CONTEXT_FLAGS -> 0;
            case GL_VERTEX_ARRAY_BINDING -> GLState.get().boundVertexArray();
            case GL_FRAMEBUFFER_BINDING -> GLState.get().boundFramebuffer();
            default -> throw new UnsupportedOperationException(
                    "GL30.glGetInteger does not emulate 0x" + Integer.toHexString(name));
        };
    }
    public static int glGetIntegeri(int name, int index) {
        return glGetInteger(name);
    }
    /**
     * webgl2 writes fragment outputs by their declared location, and the page cannot rename one
     * after linking. location 0 is the colour buffer every draw here targets, so binding it is
     * what the caller asked for and anything else would silently write nowhere.
     */
    public static void glBindFragDataLocation(int program, int colorNumber, CharSequence name) {
        if (colorNumber != 0) {
            throw new UnsupportedOperationException(
                    "glBindFragDataLocation can only bind colour 0; webgl2 draws to one colour "
                            + "buffer here, and the shader declares its own location");
        }
    }
    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("glVertexAttribIPointer: offset out of range: "
                    + offset);
        }
        GLState state = GLState.get();
        int buffer = state.boundBuffer(BufferTarget.ARRAY);
        if (buffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glVertexAttribIPointer was given an offset with no GL_ARRAY_BUFFER bound; the "
                            + "core profile has no client-side arrays");
        }
        AttributeType component = GLEnums.integerAttributeType(type, "glVertexAttribIPointer");
        state.attributes().slot(index)
                .pointAtIntegers(buffer, (int) offset, size, component, stride);
    }
    static String[] extensions() {
        String reported = GLState.get().device().information(GraphicsInfo.EXTENSIONS);
        if (reported == null || reported.isBlank()) {
            return new String[0];
        }
        return reported.trim().split("\\s+");
    }
}
