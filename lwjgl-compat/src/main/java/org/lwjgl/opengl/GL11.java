package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.ClientArray;
import dev.flagon.lwjgl.internal.DrawMode;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.VertexArrays;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;

    public static final int GL_VERTEX_ARRAY = 0x8074;
    public static final int GL_NORMAL_ARRAY = 0x8075;
    public static final int GL_COLOR_ARRAY = 0x8076;
    public static final int GL_INDEX_ARRAY = 0x8077;
    public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;

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

    // vertex arrays

    public static void glEnableClientState(int array) {
        clientArray(array, "glEnableClientState").setEnabled(true);
    }

    public static void glDisableClientState(int array) {
        clientArray(array, "glDisableClientState").setEnabled(false);
    }

    public static void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().position().pointAt(pointer, size, AttributeType.FLOAT, false, stride);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        pointIntoBuffer(state().arrays().position(), size, componentType(type, "glVertexPointer"),
                false, stride, offset, "glVertexPointer");
    }

    public static void glColorPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().color().pointAt(pointer, size, AttributeType.FLOAT, false, stride);
    }

    /** the byte form is always normalised: gl reads integer color components as 0..1. */
    public static void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {
        AttributeType type = unsigned ? AttributeType.UNSIGNED_BYTE : AttributeType.BYTE;
        state().arrays().color().pointAt(pointer, size, type, true, stride);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        AttributeType component = componentType(type, "glColorPointer");
        pointIntoBuffer(state().arrays().color(), size, component,
                component != AttributeType.FLOAT, stride, offset, "glColorPointer");
    }

    public static void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().texCoord().pointAt(pointer, size, AttributeType.FLOAT, false, stride);
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        pointIntoBuffer(state().arrays().texCoord(), size,
                componentType(type, "glTexCoordPointer"), false, stride, offset,
                "glTexCoordPointer");
    }

    public static void glDrawArrays(int mode, int first, int count) {
        state().drawArrays(primitive(mode), first, count);
    }

    public static void glDrawElements(int mode, int count, int type, long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("index offset out of range: " + offset);
        }
        state().drawElements(primitive(mode), count, indexType(type), (int) offset);
    }

    private static void pointIntoBuffer(ClientArray array, int size, AttributeType type,
                                        boolean normalized, int stride, long offset, String call) {
        int buffer = state().boundBuffer(BufferTarget.ARRAY);
        if (buffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    call + " was given an offset with no GL_ARRAY_BUFFER bound. that form names a "
                            + "location inside a buffer object; pass a Buffer instead to read from "
                            + "client memory");
        }
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(call + ": offset out of range: " + offset);
        }
        array.pointAt(buffer, (int) offset, size, type, normalized, stride);
    }

    private static ClientArray clientArray(int array, String call) {
        VertexArrays arrays = state().arrays();
        return switch (array) {
            case GL_VERTEX_ARRAY -> arrays.position();
            case GL_COLOR_ARRAY -> arrays.color();
            case GL_TEXTURE_COORD_ARRAY -> arrays.texCoord();
            default -> throw new UnsupportedOperationException(
                    call + " does not yet emulate array 0x" + Integer.toHexString(array));
        };
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
        requireRgbaBytes(format, type);
        device().textureImage(width, height, pixels);
    }

    /** the same upload from a buffer of bytes in r, g, b, a order, which is what real lwjgl takes */
    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type,
                                    ByteBuffer pixels) {
        requireTexture2D(target, "glTexImage2D");
        requireRgbaBytes(format, type);
        device().textureImage(width, height, packed(pixels, width * height));
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type,
                                    IntBuffer pixels) {
        requireTexture2D(target, "glTexImage2D");
        requireRgbaBytes(format, type);
        int[] copied = new int[width * height];
        pixels.duplicate().get(copied);
        device().textureImage(width, height, copied);
    }

    /** @return one 0xAARRGGBB int per pixel, reading the buffer without consuming it */
    private static int[] packed(ByteBuffer pixels, int count) {
        ByteBuffer reading = pixels.duplicate();
        int[] packed = new int[count];
        for (int pixel = 0; pixel < count; pixel++) {
            int red = reading.get() & 0xFF;
            int green = reading.get() & 0xFF;
            int blue = reading.get() & 0xFF;
            int alpha = reading.get() & 0xFF;
            packed[pixel] = alpha << 24 | red << 16 | green << 8 | blue;
        }
        return packed;
    }

    private static void requireRgbaBytes(int format, int type) {
        if (format != GL_RGBA || type != GL_UNSIGNED_BYTE) {
            throw new UnsupportedOperationException(
                    "glTexImage2D emulates GL_RGBA/GL_UNSIGNED_BYTE only");
        }
    }

    private static void requireTexture2D(int target, String call) {
        if (target != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    call + " emulates GL_TEXTURE_2D only, got 0x" + Integer.toHexString(target));
        }
    }

    // translation

    private static DrawMode primitive(int mode) {
        return switch (mode) {
            case GL_POINTS -> DrawMode.POINTS;
            case GL_LINES -> DrawMode.LINES;
            case GL_LINE_STRIP -> DrawMode.LINE_STRIP;
            case GL_LINE_LOOP -> DrawMode.LINE_LOOP;
            case GL_TRIANGLES -> DrawMode.TRIANGLES;
            case GL_TRIANGLE_STRIP -> DrawMode.TRIANGLE_STRIP;
            case GL_TRIANGLE_FAN -> DrawMode.TRIANGLE_FAN;
            case GL_QUADS -> DrawMode.QUADS;
            default -> throw new IllegalArgumentException(
                    "unknown primitive 0x" + Integer.toHexString(mode));
        };
    }

    private static AttributeType componentType(int type, String call) {
        return switch (type) {
            case GL_BYTE -> AttributeType.BYTE;
            case GL_UNSIGNED_BYTE -> AttributeType.UNSIGNED_BYTE;
            case GL_SHORT -> AttributeType.SHORT;
            case GL_UNSIGNED_SHORT -> AttributeType.UNSIGNED_SHORT;
            case GL_INT -> AttributeType.INT;
            case GL_UNSIGNED_INT -> AttributeType.UNSIGNED_INT;
            case GL_FLOAT -> AttributeType.FLOAT;
            default -> throw new UnsupportedOperationException(
                    call + " does not emulate component type 0x" + Integer.toHexString(type));
        };
    }

    private static IndexType indexType(int type) {
        return switch (type) {
            case GL_UNSIGNED_BYTE -> IndexType.UNSIGNED_BYTE;
            case GL_UNSIGNED_SHORT -> IndexType.UNSIGNED_SHORT;
            case GL_UNSIGNED_INT -> IndexType.UNSIGNED_INT;
            default -> throw new IllegalArgumentException(
                    "glDrawElements: indices must be unsigned, got 0x" + Integer.toHexString(type));
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
