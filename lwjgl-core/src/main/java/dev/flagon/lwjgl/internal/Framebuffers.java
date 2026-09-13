package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.FramebufferAttachment;
import dev.flagon.platform.graphics.RenderbufferFormat;

import java.nio.IntBuffer;

/**
 * framebuffer objects, which gl 3.0 took into core and ARB and EXT both name in their own way.
 */
public final class Framebuffers {

    public static final int FRAMEBUFFER = 0x8D40;
    public static final int READ_FRAMEBUFFER = 0x8CA8;
    public static final int DRAW_FRAMEBUFFER = 0x8CA9;
    public static final int RENDERBUFFER = 0x8D41;
    public static final int COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int DEPTH_ATTACHMENT = 0x8D00;
    public static final int STENCIL_ATTACHMENT = 0x8D20;
    public static final int DEPTH_STENCIL_ATTACHMENT = 0x821A;
    public static final int COMPLETE = 0x8CD5;
    public static final int INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int INCOMPLETE_DIMENSIONS = 0x8CD9;
    public static final int UNSUPPORTED = 0x8CDD;
    public static final int TEXTURE_2D = 0x0DE1;
    public static final int DEPTH_COMPONENT = 0x1902;
    public static final int DEPTH_STENCIL = 0x84F9;
    public static final int DEPTH_COMPONENT16 = 0x81A5;
    public static final int DEPTH_COMPONENT24 = 0x81A6;
    public static final int DEPTH24_STENCIL8 = 0x88F0;
    public static final int STENCIL_INDEX8 = 0x8D48;
    public static final int RGBA8 = 0x8058;

    private Framebuffers() {
    }

    public static int create() {
        return GLState.get().createFramebuffer();
    }

    public static void createInto(IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            framebuffers.put(i, create());
        }
    }

    public static void bind(int target, int framebuffer, String call) {
        requireTarget(target, call);
        GLState.get().bindFramebuffer(framebuffer);
    }

    public static void delete(int framebuffer) {
        GLState.get().deleteFramebuffer(framebuffer);
    }

    public static void deleteAll(IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            delete(framebuffers.get(i));
        }
    }

    public static boolean isFramebuffer(int framebuffer) {
        return framebuffer != 0 && GLState.get().boundFramebuffer() == framebuffer;
    }

    public static void attachTexture(int target, int attachment, int textureTarget, int texture,
                                     int level, String call) {
        requireTarget(target, call);
        if (textureTarget != TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    call + " attaches a GL_TEXTURE_2D here, got 0x"
                            + Integer.toHexString(textureTarget));
        }
        GLState.get().device().framebufferTexture(attachmentOf(attachment), texture, level);
    }

    public static void attachRenderbuffer(int target, int attachment, int renderbufferTarget,
                                          int renderbuffer, String call) {
        requireTarget(target, call);
        requireRenderbuffer(renderbufferTarget, call);
        GLState.get().device()
                .framebufferRenderbuffer(attachmentOf(attachment), renderbuffer);
    }

    public static int status(int target, String call) {
        requireTarget(target, call);
        return switch (GLState.get().device().framebufferStatus()) {
            case COMPLETE -> COMPLETE;
            case INCOMPLETE_ATTACHMENT -> INCOMPLETE_ATTACHMENT;
            case INCOMPLETE_MISSING_ATTACHMENT -> INCOMPLETE_MISSING_ATTACHMENT;
            case INCOMPLETE_DIMENSIONS -> INCOMPLETE_DIMENSIONS;
            case UNSUPPORTED, UNKNOWN -> UNSUPPORTED;
        };
    }

    public static int createRenderbuffer() {
        return GLState.get().device().createRenderbuffer();
    }

    public static void createRenderbuffersInto(IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            renderbuffers.put(i, createRenderbuffer());
        }
    }

    public static void bindRenderbuffer(int target, int renderbuffer, String call) {
        requireRenderbuffer(target, call);
        GLState.get().device().bindRenderbuffer(renderbuffer);
    }

    public static void deleteRenderbuffer(int renderbuffer) {
        GLState.get().device().deleteRenderbuffer(renderbuffer);
    }

    public static void deleteRenderbuffers(IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            deleteRenderbuffer(renderbuffers.get(i));
        }
    }

    public static void renderbufferStorage(int target, int format, int width, int height,
                                           String call) {
        requireRenderbuffer(target, call);
        GLState.get().device().renderbufferStorage(switch (format) {
            case RGBA8 -> RenderbufferFormat.RGBA8;
            case DEPTH_COMPONENT16 -> RenderbufferFormat.DEPTH16;
            case DEPTH_COMPONENT, DEPTH_COMPONENT24 -> RenderbufferFormat.DEPTH24;
            case DEPTH24_STENCIL8, DEPTH_STENCIL -> RenderbufferFormat.DEPTH24_STENCIL8;
            case STENCIL_INDEX8 -> RenderbufferFormat.STENCIL8;
            default -> throw new UnsupportedOperationException(
                    call + " does not emulate format 0x" + Integer.toHexString(format));
        }, width, height);
    }

    public static FramebufferAttachment attachmentOf(int attachment) {
        return switch (attachment) {
            case COLOR_ATTACHMENT0 -> FramebufferAttachment.COLOR;
            case DEPTH_ATTACHMENT -> FramebufferAttachment.DEPTH;
            case STENCIL_ATTACHMENT -> FramebufferAttachment.STENCIL;
            case DEPTH_STENCIL_ATTACHMENT -> FramebufferAttachment.DEPTH_STENCIL;
            default -> throw new UnsupportedOperationException(
                    "attachment 0x" + Integer.toHexString(attachment) + " is not emulated; the "
                            + "page draws to one colour buffer, so GL_COLOR_ATTACHMENT0 is the "
                            + "only colour attachment");
        };
    }

    private static void requireTarget(int target, String call) {
        if (target != FRAMEBUFFER && target != READ_FRAMEBUFFER && target != DRAW_FRAMEBUFFER) {
            throw new IllegalArgumentException(
                    call + " takes a framebuffer target, got 0x" + Integer.toHexString(target));
        }
    }

    private static void requireRenderbuffer(int target, String call) {
        if (target != RENDERBUFFER) {
            throw new IllegalArgumentException(
                    call + " takes GL_RENDERBUFFER, got 0x" + Integer.toHexString(target));
        }
    }
}
