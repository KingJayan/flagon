package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.Primitive;

/**
 * gl 1.1 over webgl2.
 *
 * <p>calls that cannot be honoured throw {@link UnsupportedOperationException} naming the call,
 * rather than returning quietly — a rendering bug caused by an ignored state change is far more
 * expensive to find than a stack trace pointing at the call that caused it.
 */
public final class GL11 {

    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;

    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_QUADS = 0x0007;

    public static final int GL_NEVER = 0x0200;
    public static final int GL_LESS = 0x0201;
    public static final int GL_EQUAL = 0x0202;
    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_GREATER = 0x0204;
    public static final int GL_NOTEQUAL = 0x0205;
    public static final int GL_GEQUAL = 0x0206;
    public static final int GL_ALWAYS = 0x0207;

    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 0x0300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_DST_COLOR = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;

    public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x0400;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;

    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_ALPHA_TEST = 0x0BC0;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_LIGHTING = 0x0B50;
    public static final int GL_FOG = 0x0B60;

    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;

    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;

    public static final int GL_RGBA = 0x1908;
    public static final int GL_UNSIGNED_BYTE = 0x1401;

    private GL11() {
    }

    private static GLState state() {
        return GLState.get();
    }

    private static GraphicsDevice device() {
        return GLState.get().device();
    }

    // framebuffer

    public static void glClearColor(float red, float green, float blue, float alpha) {
        device().clearColor(red, green, blue, alpha);
    }

    public static void glClearDepth(double depth) {
        device().clearDepth(depth);
    }

    public static void glClear(int mask) {
        device().clear(
                (mask & GL_COLOR_BUFFER_BIT) != 0,
                (mask & GL_DEPTH_BUFFER_BIT) != 0,
                (mask & GL_STENCIL_BUFFER_BIT) != 0);
    }

    public static void glViewport(int x, int y, int width, int height) {
        device().viewport(x, y, width, height);
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        device().setColorMask(red, green, blue, alpha);
    }

    public static void glDepthMask(boolean enabled) {
        device().setDepthWrite(enabled);
    }

    public static void glDepthFunc(int func) {
        device().setDepthFunc(compareFunc(func, "glDepthFunc"));
    }

    public static void glBlendFunc(int source, int destination) {
        device().setBlendFunc(blendFactor(source, "glBlendFunc"),
                blendFactor(destination, "glBlendFunc"));
    }

    public static void glAlphaFunc(int func, float reference) {
        state().alphaFunc(compareFunc(func, "glAlphaFunc"), reference);
    }

    // capabilities

    public static void glEnable(int capability) {
        setCapability(capability, true);
    }

    public static void glDisable(int capability) {
        setCapability(capability, false);
    }

    private static void setCapability(int capability, boolean enabled) {
        switch (capability) {
            case GL_DEPTH_TEST -> device().setDepthTest(enabled);
            case GL_BLEND -> device().setBlend(enabled);
            case GL_CULL_FACE -> device().setCullFace(enabled);
            case GL_TEXTURE_2D -> state().setTextureEnabled(enabled);
            case GL_ALPHA_TEST -> state().setAlphaTestEnabled(enabled);
            default -> throw new UnsupportedOperationException(
                    (enabled ? "glEnable" : "glDisable") + " does not yet emulate capability 0x"
                            + Integer.toHexString(capability));
        }
    }

    // matrices

    public static void glMatrixMode(int mode) {
        state().matrixMode(mode);
    }

    public static void glLoadIdentity() {
        state().loadIdentity();
    }

    public static void glPushMatrix() {
        state().pushMatrix();
    }

    public static void glPopMatrix() {
        state().popMatrix();
    }

    public static void glTranslatef(float x, float y, float z) {
        state().translate(x, y, z);
    }

    public static void glRotatef(float degrees, float x, float y, float z) {
        state().rotate(degrees, x, y, z);
    }

    public static void glScalef(float x, float y, float z) {
        state().scale(x, y, z);
    }

    public static void glOrtho(double left, double right, double bottom, double top,
                               double near, double far) {
        state().ortho((float) left, (float) right, (float) bottom, (float) top,
                (float) near, (float) far);
    }

    public static void glFrustum(double left, double right, double bottom, double top,
                                 double near, double far) {
        state().frustum((float) left, (float) right, (float) bottom, (float) top,
                (float) near, (float) far);
    }

    // immediate mode

    public static void glBegin(int mode) {
        state().begin(primitive(mode));
    }

    public static void glEnd() {
        state().end();
    }

    public static void glVertex2f(float x, float y) {
        state().vertex(x, y, 0.0f);
    }

    public static void glVertex3f(float x, float y, float z) {
        state().vertex(x, y, z);
    }

    public static void glColor3f(float red, float green, float blue) {
        state().color(red, green, blue, 1.0f);
    }

    public static void glColor4f(float red, float green, float blue, float alpha) {
        state().color(red, green, blue, alpha);
    }

    public static void glTexCoord2f(float s, float t) {
        state().texCoord(s, t);
    }

    // textures

    public static int glGenTextures() {
        return device().createTexture();
    }

    public static void glDeleteTextures(int texture) {
        device().deleteTexture(texture);
    }

    public static void glBindTexture(int target, int texture) {
        requireTexture2D(target, "glBindTexture");
        device().bindTexture(0, texture);
    }

    public static void glTexParameteri(int target, int name, int value) {
        requireTexture2D(target, "glTexParameteri");
        switch (name) {
            case GL_TEXTURE_MIN_FILTER, GL_TEXTURE_MAG_FILTER ->
                    device().setTextureFilter(value == GL_LINEAR);
            default -> throw new UnsupportedOperationException(
                    "glTexParameteri does not yet emulate parameter 0x" + Integer.toHexString(name));
        }
    }

    /** uploads rgba pixels, one int per pixel in 0xAARRGGBB order. */
    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type, int[] pixels) {
        requireTexture2D(target, "glTexImage2D");
        if (format != GL_RGBA || type != GL_UNSIGNED_BYTE) {
            throw new UnsupportedOperationException(
                    "glTexImage2D emulates GL_RGBA/GL_UNSIGNED_BYTE only");
        }
        device().textureImage(width, height, pixels);
    }

    private static void requireTexture2D(int target, String call) {
        if (target != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    call + " emulates GL_TEXTURE_2D only, got 0x" + Integer.toHexString(target));
        }
    }

    // translation

    private static Primitive primitive(int mode) {
        return switch (mode) {
            case GL_POINTS -> Primitive.POINTS;
            case GL_LINES -> Primitive.LINES;
            case GL_LINE_STRIP -> Primitive.LINE_STRIP;
            case GL_LINE_LOOP -> Primitive.LINE_LOOP;
            case GL_TRIANGLES -> Primitive.TRIANGLES;
            case GL_TRIANGLE_STRIP -> Primitive.TRIANGLE_STRIP;
            case GL_TRIANGLE_FAN -> Primitive.TRIANGLE_FAN;
            case GL_QUADS -> throw new UnsupportedOperationException(
                    "GL_QUADS has no webgl equivalent and needs index expansion; not yet emulated");
            default -> throw new IllegalArgumentException(
                    "unknown primitive 0x" + Integer.toHexString(mode));
        };
    }

    private static CompareFunc compareFunc(int func, String call) {
        return switch (func) {
            case GL_NEVER -> CompareFunc.NEVER;
            case GL_LESS -> CompareFunc.LESS;
            case GL_EQUAL -> CompareFunc.EQUAL;
            case GL_LEQUAL -> CompareFunc.LESS_OR_EQUAL;
            case GL_GREATER -> CompareFunc.GREATER;
            case GL_NOTEQUAL -> CompareFunc.NOT_EQUAL;
            case GL_GEQUAL -> CompareFunc.GREATER_OR_EQUAL;
            case GL_ALWAYS -> CompareFunc.ALWAYS;
            default -> throw new IllegalArgumentException(
                    call + ": unknown comparison 0x" + Integer.toHexString(func));
        };
    }

    private static BlendFactor blendFactor(int factor, String call) {
        return switch (factor) {
            case GL_ZERO -> BlendFactor.ZERO;
            case GL_ONE -> BlendFactor.ONE;
            case GL_SRC_COLOR -> BlendFactor.SRC_COLOR;
            case GL_ONE_MINUS_SRC_COLOR -> BlendFactor.ONE_MINUS_SRC_COLOR;
            case GL_DST_COLOR -> BlendFactor.DST_COLOR;
            case GL_ONE_MINUS_DST_COLOR -> BlendFactor.ONE_MINUS_DST_COLOR;
            case GL_SRC_ALPHA -> BlendFactor.SRC_ALPHA;
            case GL_ONE_MINUS_SRC_ALPHA -> BlendFactor.ONE_MINUS_SRC_ALPHA;
            case GL_DST_ALPHA -> BlendFactor.DST_ALPHA;
            case GL_ONE_MINUS_DST_ALPHA -> BlendFactor.ONE_MINUS_DST_ALPHA;
            default -> throw new IllegalArgumentException(
                    call + ": unknown blend factor 0x" + Integer.toHexString(factor));
        };
    }
}
