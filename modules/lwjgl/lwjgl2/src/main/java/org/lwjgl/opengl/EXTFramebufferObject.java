package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.Framebuffers;

import java.nio.IntBuffer;

/**
 * the EXT_framebuffer_object names for what gl 3.0 took into core
 */
public final class EXTFramebufferObject {

    public static final int GL_FRAMEBUFFER_EXT = 0x8D40;
    public static final int GL_RENDERBUFFER_EXT = 0x8D41;
    public static final int GL_FRAMEBUFFER_BINDING_EXT = 0x8CA6;
    public static final int GL_RENDERBUFFER_BINDING_EXT = 0x8CA7;
    public static final int GL_COLOR_ATTACHMENT0_EXT = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT_EXT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT_EXT = 0x8D20;
    public static final int GL_FRAMEBUFFER_COMPLETE_EXT = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED_EXT = 0x8CDD;
    public static final int GL_DEPTH_COMPONENT16_EXT = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24_EXT = 0x81A6;
    public static final int GL_STENCIL_INDEX8_EXT = 0x8D48;

    private EXTFramebufferObject() {
    }

    public static int glGenFramebuffersEXT() {
        return Framebuffers.create();
    }

    public static void glGenFramebuffersEXT(IntBuffer framebuffers) {
        Framebuffers.createInto(framebuffers);
    }

    public static void glBindFramebufferEXT(int target, int framebuffer) {
        Framebuffers.bind(target, framebuffer, "glBindFramebufferEXT");
    }

    public static void glDeleteFramebuffersEXT(int framebuffer) {
        Framebuffers.delete(framebuffer);
    }

    public static void glDeleteFramebuffersEXT(IntBuffer framebuffers) {
        Framebuffers.deleteAll(framebuffers);
    }

    public static boolean glIsFramebufferEXT(int framebuffer) {
        return Framebuffers.isFramebuffer(framebuffer);
    }

    public static int glCheckFramebufferStatusEXT(int target) {
        return Framebuffers.status(target, "glCheckFramebufferStatusEXT");
    }

    public static void glFramebufferTexture2DEXT(int target, int attachment, int textureTarget,
                                              int texture, int level) {
        Framebuffers.attachTexture(target, attachment, textureTarget, texture, level,
                "glFramebufferTexture2DEXT");
    }

    public static void glFramebufferRenderbufferEXT(int target, int attachment,
                                                 int renderbufferTarget, int renderbuffer) {
        Framebuffers.attachRenderbuffer(target, attachment, renderbufferTarget, renderbuffer,
                "glFramebufferRenderbufferEXT");
    }

    public static int glGenRenderbuffersEXT() {
        return Framebuffers.createRenderbuffer();
    }

    public static void glGenRenderbuffersEXT(IntBuffer renderbuffers) {
        Framebuffers.createRenderbuffersInto(renderbuffers);
    }

    public static void glBindRenderbufferEXT(int target, int renderbuffer) {
        Framebuffers.bindRenderbuffer(target, renderbuffer, "glBindRenderbufferEXT");
    }

    public static void glDeleteRenderbuffersEXT(int renderbuffer) {
        Framebuffers.deleteRenderbuffer(renderbuffer);
    }

    public static void glDeleteRenderbuffersEXT(IntBuffer renderbuffers) {
        Framebuffers.deleteRenderbuffers(renderbuffers);
    }

    public static void glRenderbufferStorageEXT(int target, int format, int width, int height) {
        Framebuffers.renderbufferStorage(target, format, width, height,
                "glRenderbufferStorageEXT");
    }

    public static void glGenerateMipmapEXT(int target) {
        GL11.glGenerateMipmap(target);
    }
}
