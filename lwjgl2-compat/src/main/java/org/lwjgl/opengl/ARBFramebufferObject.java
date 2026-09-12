package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.Framebuffers;

import java.nio.IntBuffer;

/**
 * the ARB_framebuffer_object names for what gl 3.0 took into core
 */
public final class ARBFramebufferObject {

    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_RENDERBUFFER = 0x8D41;
    public static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    public static final int GL_RENDERBUFFER_BINDING = 0x8CA7;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_STENCIL_INDEX8 = 0x8D48;

    private ARBFramebufferObject() {
    }

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

    public static int glCheckFramebufferStatus(int target) {
        return Framebuffers.status(target, "glCheckFramebufferStatus");
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textureTarget,
                                              int texture, int level) {
        Framebuffers.attachTexture(target, attachment, textureTarget, texture, level,
                "glFramebufferTexture2D");
    }

    public static void glFramebufferRenderbuffer(int target, int attachment,
                                                 int renderbufferTarget, int renderbuffer) {
        Framebuffers.attachRenderbuffer(target, attachment, renderbufferTarget, renderbuffer,
                "glFramebufferRenderbuffer");
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
        Framebuffers.renderbufferStorage(target, format, width, height,
                "glRenderbufferStorage");
    }

    public static void glGenerateMipmap(int target) {
        GL11.glGenerateMipmap(target);
    }
}
