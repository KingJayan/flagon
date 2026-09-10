package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLEnums;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.FramebufferAttachment;
import dev.flagon.platform.graphics.RenderbufferFormat;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsInfo;

import java.nio.IntBuffer;

/** vertex array objects, the indexed string queries and the integer attribute pointers. */
public final class GL30 {

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

    private GL30() {
    }

    public static int glGenFramebuffers() {
        return GLState.get().createFramebuffer();
    }

    public static void glGenFramebuffers(IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            framebuffers.put(i, glGenFramebuffers());
        }
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        requireFramebufferTarget(target, "glBindFramebuffer");
        GLState.get().bindFramebuffer(framebuffer);
    }

    public static void glDeleteFramebuffers(int framebuffer) {
        GLState.get().deleteFramebuffer(framebuffer);
    }

    public static void glDeleteFramebuffers(IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            glDeleteFramebuffers(framebuffers.get(i));
        }
    }

    public static boolean glIsFramebuffer(int framebuffer) {
        return framebuffer != 0 && GLState.get().boundFramebuffer() == framebuffer;
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textureTarget,
                                              int texture, int level) {
        requireFramebufferTarget(target, "glFramebufferTexture2D");
        if (textureTarget != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    "glFramebufferTexture2D attaches a GL_TEXTURE_2D here, got 0x"
                            + Integer.toHexString(textureTarget));
        }
        GLState.get().device().framebufferTexture(attachmentOf(attachment), texture, level);
    }

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                                 int renderbuffer) {
        requireFramebufferTarget(target, "glFramebufferRenderbuffer");
        if (renderbufferTarget != GL_RENDERBUFFER) {
            throw new IllegalArgumentException(
                    "glFramebufferRenderbuffer takes GL_RENDERBUFFER, got 0x"
                            + Integer.toHexString(renderbufferTarget));
        }
        GLState.get().device()
                .framebufferRenderbuffer(attachmentOf(attachment), renderbuffer);
    }

    public static int glCheckFramebufferStatus(int target) {
        requireFramebufferTarget(target, "glCheckFramebufferStatus");
        return switch (GLState.get().device().framebufferStatus()) {
            case COMPLETE -> GL_FRAMEBUFFER_COMPLETE;
            case INCOMPLETE_ATTACHMENT -> GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
            case INCOMPLETE_MISSING_ATTACHMENT -> GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
            case INCOMPLETE_DIMENSIONS -> GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS;
            case UNSUPPORTED, UNKNOWN -> GL_FRAMEBUFFER_UNSUPPORTED;
        };
    }

    public static int glGenRenderbuffers() {
        return GLState.get().device().createRenderbuffer();
    }

    public static void glGenRenderbuffers(IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            renderbuffers.put(i, glGenRenderbuffers());
        }
    }

    public static void glBindRenderbuffer(int target, int renderbuffer) {
        if (target != GL_RENDERBUFFER) {
            throw new IllegalArgumentException(
                    "glBindRenderbuffer takes GL_RENDERBUFFER, got 0x"
                            + Integer.toHexString(target));
        }
        GLState.get().device().bindRenderbuffer(renderbuffer);
    }

    public static void glDeleteRenderbuffers(int renderbuffer) {
        GLState.get().device().deleteRenderbuffer(renderbuffer);
    }

    public static void glDeleteRenderbuffers(IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            glDeleteRenderbuffers(renderbuffers.get(i));
        }
    }

    public static void glRenderbufferStorage(int target, int format, int width, int height) {
        if (target != GL_RENDERBUFFER) {
            throw new IllegalArgumentException(
                    "glRenderbufferStorage takes GL_RENDERBUFFER, got 0x"
                            + Integer.toHexString(target));
        }
        GLState.get().device().renderbufferStorage(switch (format) {
            case GL_RGBA8 -> RenderbufferFormat.RGBA8;
            case GL_DEPTH_COMPONENT16 -> RenderbufferFormat.DEPTH16;
            case GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24 -> RenderbufferFormat.DEPTH24;
            case GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL -> RenderbufferFormat.DEPTH24_STENCIL8;
            case GL_STENCIL_INDEX8 -> RenderbufferFormat.STENCIL8;
            default -> throw new UnsupportedOperationException(
                    "glRenderbufferStorage does not emulate format 0x"
                            + Integer.toHexString(format));
        }, width, height);
    }

    public static void glGenerateMipmap(int target) {
        if (target != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    "glGenerateMipmap emulates GL_TEXTURE_2D only, got 0x"
                            + Integer.toHexString(target));
        }
        GLState.get().device().generateMipmap();
    }

    private static FramebufferAttachment attachmentOf(int attachment) {
        if (attachment == GL_DEPTH_ATTACHMENT) {
            return FramebufferAttachment.DEPTH;
        }
        if (attachment == GL_STENCIL_ATTACHMENT) {
            return FramebufferAttachment.STENCIL;
        }
        if (attachment == GL_DEPTH_STENCIL_ATTACHMENT) {
            return FramebufferAttachment.DEPTH_STENCIL;
        }
        if (attachment == GL_COLOR_ATTACHMENT0) {
            return FramebufferAttachment.COLOR;
        }
        throw new UnsupportedOperationException(
                "attachment 0x" + Integer.toHexString(attachment) + " is not emulated; the page "
                        + "draws to one colour buffer, so GL_COLOR_ATTACHMENT0 is the only colour "
                        + "attachment");
    }

    private static void requireFramebufferTarget(int target, String call) {
        if (target != GL_FRAMEBUFFER && target != GL_DRAW_FRAMEBUFFER
                && target != GL_READ_FRAMEBUFFER) {
            throw new IllegalArgumentException(
                    call + " takes a framebuffer target, got 0x" + Integer.toHexString(target));
        }
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

    private static String[] extensions() {
        String reported = GLState.get().device().information(GraphicsInfo.EXTENSIONS);
        if (reported == null || reported.isBlank()) {
            return new String[0];
        }
        return reported.trim().split("\\s+");
    }
}
