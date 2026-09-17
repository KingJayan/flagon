package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.ClientArray;
import dev.flagon.lwjgl.internal.DrawMode;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.Lighting;
import dev.flagon.lwjgl.internal.Matrix4;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendEquation;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.FramebufferAttachment;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsError;
import dev.flagon.platform.graphics.GraphicsInfo;
import dev.flagon.platform.graphics.GraphicsLimit;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.StencilOp;
import dev.flagon.platform.graphics.Winding;
import dev.flagon.platform.graphics.TextureFilter;
import dev.flagon.platform.graphics.TextureWrap;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * the core-profile half of gl 1.1, the half lwjgl 3 code imports on its own
 */
public class GL11C {

    protected GL11C() {
    }

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
    public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
    public static final int GL_DST_COLOR = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
    public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x0400;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_FRONT = 0x0404;
    public static final int GL_BACK = 0x0405;
    public static final int GL_FRONT_AND_BACK = 0x0408;
    public static final int GL_CW = 0x0900;
    public static final int GL_CCW = 0x0901;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_ALPHA_TEST = 0x0BC0;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_PROXY_TEXTURE_2D = 0x8064;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP = 0x2900;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_TEXTURE_MIN_LOD = 0x813A;
    public static final int GL_TEXTURE_MAX_LOD = 0x813B;
    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    public static final int GL_GENERATE_MIPMAP = 0x8191;
    public static final int GL_TEXTURE_WIDTH = 0x1000;
    public static final int GL_TEXTURE_HEIGHT = 0x1001;
    public static final int GL_TEXTURE_INTERNAL_FORMAT = 0x1003;
    public static final int GL_TEXTURE_BORDER = 0x1005;
    public static final int GL_UNPACK_SWAP_BYTES = 0x0CF0;
    public static final int GL_UNPACK_LSB_FIRST = 0x0CF1;
    public static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    public static final int GL_UNPACK_SKIP_ROWS = 0x0CF3;
    public static final int GL_UNPACK_SKIP_PIXELS = 0x0CF4;
    public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    public static final int GL_PACK_SWAP_BYTES = 0x0D00;
    public static final int GL_PACK_LSB_FIRST = 0x0D01;
    public static final int GL_PACK_ROW_LENGTH = 0x0D02;
    public static final int GL_PACK_SKIP_ROWS = 0x0D03;
    public static final int GL_PACK_SKIP_PIXELS = 0x0D04;
    public static final int GL_PACK_ALIGNMENT = 0x0D05;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_RGB = 0x1907;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_INVALID_ENUM = 0x0500;
    public static final int GL_INVALID_VALUE = 0x0501;
    public static final int GL_INVALID_OPERATION = 0x0502;
    public static final int GL_STACK_OVERFLOW = 0x0503;
    public static final int GL_STACK_UNDERFLOW = 0x0504;
    public static final int GL_OUT_OF_MEMORY = 0x0505;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_STENCIL_TEST = 0x0B90;
    public static final int GL_KEEP = 0x1E00;
    public static final int GL_REPLACE = 0x1E01;
    public static final int GL_INCR = 0x1E02;
    public static final int GL_DECR = 0x1E03;
    public static final int GL_INVERT = 0x150A;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_POINT = 0x1B00;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_DONT_CARE = 0x1100;
    public static final int GL_FASTEST = 0x1101;
    public static final int GL_NICEST = 0x1102;
    public static final int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;
    public static final int GL_POINT_SMOOTH_HINT = 0x0C51;
    public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
    public static final int GL_POLYGON_SMOOTH_HINT = 0x0C53;
    public static final int GL_FOG_HINT = 0x0C54;
    public static final int GL_LIGHTING = 0x0B50;
    public static final int GL_COLOR_MATERIAL = 0x0B57;
    public static final int GL_NORMALIZE = 0x0BA1;
    public static final int GL_LIGHT0 = 0x4000;
    public static final int GL_LIGHT1 = 0x4001;
    public static final int GL_LIGHT2 = 0x4002;
    public static final int GL_LIGHT3 = 0x4003;
    public static final int GL_LIGHT4 = 0x4004;
    public static final int GL_LIGHT5 = 0x4005;
    public static final int GL_LIGHT6 = 0x4006;
    public static final int GL_LIGHT7 = 0x4007;
    public static final int GL_AMBIENT = 0x1200;
    public static final int GL_DIFFUSE = 0x1201;
    public static final int GL_SPECULAR = 0x1202;
    public static final int GL_POSITION = 0x1203;
    public static final int GL_CONSTANT_ATTENUATION = 0x1207;
    public static final int GL_LINEAR_ATTENUATION = 0x1208;
    public static final int GL_QUADRATIC_ATTENUATION = 0x1209;
    public static final int GL_EMISSION = 0x1600;
    public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
    public static final int GL_FOG = 0x0B60;
    public static final int GL_FOG_DENSITY = 0x0B62;
    public static final int GL_FOG_START = 0x0B63;
    public static final int GL_FOG_END = 0x0B64;
    public static final int GL_FOG_MODE = 0x0B65;
    public static final int GL_EXP = 0x0800;
    public static final int GL_EXP2 = 0x0801;
    public static final int GL_CURRENT_COLOR = 0x0B00;
    public static final int GL_MATRIX_MODE = 0x0BA0;
    public static final int GL_VIEWPORT = 0x0BA2;
    public static final int GL_MODELVIEW_MATRIX = 0x0BA6;
    public static final int GL_PROJECTION_MATRIX = 0x0BA7;
    public static final int GL_BLEND_DST = 0x0BE0;
    public static final int GL_BLEND_SRC = 0x0BE1;
    public static final int GL_CULL_FACE_MODE = 0x0B45;
    public static final int GL_FRONT_FACE = 0x0B46;
    public static final int GL_DEPTH_WRITEMASK = 0x0B72;
    public static final int GL_DEPTH_CLEAR_VALUE = 0x0B73;
    public static final int GL_DEPTH_FUNC = 0x0B74;
    public static final int GL_SCISSOR_BOX = 0x0C10;
    public static final int GL_COLOR_CLEAR_VALUE = 0x0C22;
    public static final int GL_COLOR_WRITEMASK = 0x0C23;
    public static final int GL_TEXTURE_BINDING_2D = 0x8069;
    static GLState state() {
        return GLState.get();
    }
    static GraphicsDevice device() {
        return GLState.get().device();
    }
    /**
     * a call the display list being recorded should hold instead of running
     */
    static boolean recording() {
        return state().isRecording();
    }
    // framebuffer

    public static void glClearColor(float red, float green, float blue, float alpha) {
        if (recording() && state().record(() -> state().clearColor(red, green, blue, alpha))) {
            return;
        }
        state().clearColor(red, green, blue, alpha);
    }
    public static void glClearDepth(double depth) {
        if (recording() && state().record(() -> state().clearDepth(depth))) {
            return;
        }
        state().clearDepth(depth);
    }
    public static void glClear(int mask) {
        if (recording() && state().record(() -> clear(mask))) {
            return;
        }
        clear(mask);
    }
    static void clear(int mask) {
        device().clear(
                (mask & GL_COLOR_BUFFER_BIT) != 0,
                (mask & GL_DEPTH_BUFFER_BIT) != 0,
                (mask & GL_STENCIL_BUFFER_BIT) != 0);
    }
    public static void glViewport(int x, int y, int width, int height) {
        if (recording() && state().record(() -> state().viewport(x, y, width, height))) {
            return;
        }
        state().viewport(x, y, width, height);
    }
    /** gl measures the scissor box from the bottom left, as it measures the viewport. */
    public static void glScissor(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("glScissor: negative size " + width + "x" + height);
        }
        if (recording() && state().record(() -> state().setScissor(x, y, width, height))) {
            return;
        }
        state().setScissor(x, y, width, height);
    }
    public static void glCullFace(int face) {
        CullFace faces = switch (face) {
            case GL_FRONT -> CullFace.FRONT;
            case GL_BACK -> CullFace.BACK;
            case GL_FRONT_AND_BACK -> CullFace.FRONT_AND_BACK;
            default -> throw new UnsupportedOperationException(
                    "glCullFace: unknown face 0x" + Integer.toHexString(face));
        };
        if (recording() && state().record(() -> state().setCulledFaces(faces))) {
            return;
        }
        state().setCulledFaces(faces);
    }
    public static void glFrontFace(int winding) {
        Winding front = switch (winding) {
            case GL_CW -> Winding.CLOCKWISE;
            case GL_CCW -> Winding.COUNTER_CLOCKWISE;
            default -> throw new UnsupportedOperationException(
                    "glFrontFace: unknown winding 0x" + Integer.toHexString(winding));
        };
        if (recording() && state().record(() -> state().setFrontFace(front))) {
            return;
        }
        state().setFrontFace(front);
    }
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        if (recording() && state().record(() -> state().setColorMask(red, green, blue, alpha))) {
            return;
        }
        state().setColorMask(red, green, blue, alpha);
    }
    public static void glDepthMask(boolean enabled) {
        if (recording() && state().record(() -> state().setDepthWrite(enabled))) {
            return;
        }
        state().setDepthWrite(enabled);
    }
    public static void glDepthFunc(int func) {
        CompareFunc compare = compareFunc(func, "glDepthFunc");
        if (recording() && state().record(() -> state().setDepthFunc(compare))) {
            return;
        }
        state().setDepthFunc(compare);
    }
    public static void glBlendFunc(int source, int destination) {
        BlendFactor from = blendFactor(source, "glBlendFunc");
        BlendFactor to = blendFactor(destination, "glBlendFunc");
        if (recording() && state().record(() -> state().setBlendFunc(from, to))) {
            return;
        }
        state().setBlendFunc(from, to);
    }
    // capabilities

    public static void glEnable(int capability) {
        if (recording() && state().record(() -> setCapability(capability, true))) {
            return;
        }
        setCapability(capability, true);
    }
    public static void glDisable(int capability) {
        if (recording() && state().record(() -> setCapability(capability, false))) {
            return;
        }
        setCapability(capability, false);
    }
    public static boolean glIsEnabled(int capability) {
        return state().isEnabled(capability);
    }
    static void setCapability(int capability, boolean enabled) {
        switch (capability) {
            case GL_DEPTH_TEST -> state().setDepthTest(enabled);
            case GL_BLEND -> state().setBlend(enabled);
            case GL_CULL_FACE -> state().setCullFace(enabled);
            case GL_SCISSOR_TEST -> state().setScissorTest(enabled);
            case GL_STENCIL_TEST -> device().setStencilTest(enabled);
            case GL_POLYGON_OFFSET_FILL -> device().setPolygonOffsetFill(enabled);
            case GL_TEXTURE_2D -> state().setTextureEnabled(enabled);
            case GL_ALPHA_TEST -> state().setAlphaTestEnabled(enabled);
            case GL_LIGHTING -> state().lighting().setEnabled(enabled);
            case GL_COLOR_MATERIAL -> state().lighting().setColorMaterial(enabled);
            case GL_FOG -> state().lighting().setFogEnabled(enabled);
            case GL_NORMALIZE, GL12.GL_RESCALE_NORMAL ->
                    state().setNormalRescaling(capability, enabled);
            case GL_LIGHT0, GL_LIGHT1, GL_LIGHT2, GL_LIGHT3,
                 GL_LIGHT4, GL_LIGHT5, GL_LIGHT6, GL_LIGHT7 ->
                    state().lighting().setLightEnabled(capability - GL_LIGHT0, enabled);
            default -> throw new UnsupportedOperationException(
                    (enabled ? "glEnable" : "glDisable") + " does not yet emulate capability 0x"
                            + Integer.toHexString(capability));
        }
    }
    // stencil

    public static void glStencilFunc(int func, int reference, int mask) {
        CompareFunc compare = compareFunc(func, "glStencilFunc");
        if (recording()
                && state().record(() -> device().setStencilFunc(compare, reference, mask))) {
            return;
        }
        device().setStencilFunc(compare, reference, mask);
    }
    public static void glStencilOp(int stencilFail, int depthFail, int pass) {
        StencilOp onStencilFail = stencilOp(stencilFail);
        StencilOp onDepthFail = stencilOp(depthFail);
        StencilOp onPass = stencilOp(pass);
        if (recording()
                && state().record(() -> device().setStencilOp(onStencilFail, onDepthFail,
                        onPass))) {
            return;
        }
        device().setStencilOp(onStencilFail, onDepthFail, onPass);
    }
    public static void glStencilMask(int mask) {
        if (recording() && state().record(() -> device().setStencilMask(mask))) {
            return;
        }
        device().setStencilMask(mask);
    }
    public static void glClearStencil(int value) {
        if (recording() && state().record(() -> device().clearStencil(value))) {
            return;
        }
        device().clearStencil(value);
    }
    static StencilOp stencilOp(int op) {
        return switch (op) {
            case GL_KEEP -> StencilOp.KEEP;
            case GL_ZERO -> StencilOp.ZERO;
            case GL_REPLACE -> StencilOp.REPLACE;
            case GL_INCR -> StencilOp.INCREMENT;
            case GL_DECR -> StencilOp.DECREMENT;
            case GL_INVERT -> StencilOp.INVERT;
            default -> throw new UnsupportedOperationException(
                    "glStencilOp does not emulate operation 0x" + Integer.toHexString(op));
        };
    }
    // rasterisation

    public static void glPolygonOffset(float factor, float units) {
        if (recording() && state().record(() -> device().setPolygonOffset(factor, units))) {
            return;
        }
        device().setPolygonOffset(factor, units);
    }
    public static void glLineWidth(float width) {
        if (width <= 0.0f) {
            throw new IllegalArgumentException("glLineWidth: width must be positive, got " + width);
        }
        if (recording() && state().record(() -> device().setLineWidth(width))) {
            return;
        }
        device().setLineWidth(width);
    }
    /** the size is a shader uniform here, which is where gl es puts it. */
    public static void glPointSize(float size) {
        if (size <= 0.0f) {
            throw new IllegalArgumentException("glPointSize: size must be positive, got " + size);
        }
        if (recording() && state().record(() -> state().pointSize(size))) {
            return;
        }
        state().pointSize(size);
    }
    public static void glHint(int target, int mode) {
        switch (target) {
            case GL_PERSPECTIVE_CORRECTION_HINT, GL_POINT_SMOOTH_HINT, GL_LINE_SMOOTH_HINT,
                 GL_POLYGON_SMOOTH_HINT, GL_FOG_HINT -> {
            }
            default -> throw new UnsupportedOperationException(
                    "glHint does not know target 0x" + Integer.toHexString(target));
        }
        switch (mode) {
            case GL_DONT_CARE, GL_FASTEST, GL_NICEST -> {
            }
            default -> throw new IllegalArgumentException(
                    "glHint: unknown mode 0x" + Integer.toHexString(mode));
        }
    }
    /** webgl rasterises filled polygons only; there is no wireframe mode to switch to. */
    public static void glPolygonMode(int face, int mode) {
        if (mode != GL_FILL) {
            throw new UnsupportedOperationException(
                    "glPolygonMode emulates GL_FILL only; webgl has no GL_LINE or GL_POINT polygon "
                            + "mode. draw the outline with GL_LINES instead");
        }
    }
    public static void glFinish() {
        device().finish();
    }
    public static void glFlush() {
        device().flush();
    }
    // reading back

    /**
     * gl reads the framebuffer from the bottom left up, so the first row returned is the lowest.
     */
    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    ByteBuffer pixels) {
        requireRgbaBytes(format, type);
        byte[] read = readPixels(x, y, width, height);
        if (pixels.remaining() < read.length) {
            throw new IllegalArgumentException("glReadPixels needs room for " + read.length
                    + " bytes, got " + pixels.remaining());
        }
        pixels.duplicate().put(read);
    }
    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    IntBuffer pixels) {
        requireRgbaBytes(format, type);
        byte[] read = readPixels(x, y, width, height);
        int count = width * height;
        if (pixels.remaining() < count) {
            throw new IllegalArgumentException("glReadPixels needs room for " + count
                    + " pixels, got " + pixels.remaining());
        }
        IntBuffer target = pixels.duplicate();
        for (int pixel = 0; pixel < count; pixel++) {
            int offset = pixel * 4;
            target.put((read[offset + 3] & 0xFF) << 24
                    | (read[offset] & 0xFF) << 16
                    | (read[offset + 1] & 0xFF) << 8
                    | (read[offset + 2] & 0xFF));
        }
    }
    static byte[] readPixels(int x, int y, int width, int height) {
        state().refuseWhileRecording("glReadPixels");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "glReadPixels: negative size " + width + "x" + height);
        }
        return device().readPixels(x, y, width, height);
    }
    /** copies from the framebuffer into the bound texture, without a round trip through the cpu. */
    public static void glCopyTexSubImage2D(int target, int level, int x, int y,
                                           int framebufferX, int framebufferY,
                                           int width, int height) {
        requireTexture2D(target, "glCopyTexSubImage2D");
        state().refuseWhileRecording("glCopyTexSubImage2D");
        requireLevel(level);
        device().copyTextureSubImage(x, y, framebufferX, framebufferY, width, height);
    }
    // queries

    public static int glGetError() {
        GraphicsError error = device().takeError();
        return switch (error) {
            case NONE -> GL_NO_ERROR;
            case INVALID_ENUM -> GL_INVALID_ENUM;
            case INVALID_VALUE -> GL_INVALID_VALUE;
            case INVALID_OPERATION, CONTEXT_LOST -> GL_INVALID_OPERATION;
            case INVALID_FRAMEBUFFER_OPERATION -> GL_INVALID_FRAMEBUFFER_OPERATION;
            case OUT_OF_MEMORY -> GL_OUT_OF_MEMORY;
        };
    }
    /** @return what the host says about itself; these strings describe WebGL2, not a desktop gl */
    public static String glGetString(int name) {
        return device().information(switch (name) {
            case GL_VENDOR -> GraphicsInfo.VENDOR;
            case GL_RENDERER -> GraphicsInfo.RENDERER;
            case GL_VERSION -> GraphicsInfo.VERSION;
            case GL_SHADING_LANGUAGE_VERSION -> GraphicsInfo.SHADING_LANGUAGE_VERSION;
            case GL_EXTENSIONS -> GraphicsInfo.EXTENSIONS;
            default -> throw new UnsupportedOperationException(
                    "glGetString does not emulate 0x" + Integer.toHexString(name));
        });
    }
    public static int glGetInteger(int name) {
        return switch (name) {
            case GL_MAX_TEXTURE_SIZE -> device().limit(GraphicsLimit.MAX_TEXTURE_SIZE);
            case GL_MAX_TEXTURE_UNITS, GL_MAX_TEXTURE_IMAGE_UNITS ->
                    device().limit(GraphicsLimit.MAX_TEXTURE_UNITS);
            case GL_MAX_VERTEX_ATTRIBS -> device().limit(GraphicsLimit.MAX_VERTEX_ATTRIBUTES);
            // a hint about what one draw call should carry; webgl has no such ceiling of its own
            case GL12.GL_MAX_ELEMENTS_VERTICES, GL12.GL_MAX_ELEMENTS_INDICES -> Integer.MAX_VALUE;
            case GL_MATRIX_MODE -> state().matrixMode();
            case GL_TEXTURE_BINDING_2D -> state().boundTexture();
            case GL_DEPTH_FUNC -> compareEnum(state().depthFunc());
            case GL_BLEND_SRC -> blendEnum(state().blendSource());
            case GL_BLEND_DST -> blendEnum(state().blendDestination());
            case GL14.GL_BLEND_SRC_ALPHA -> blendEnum(state().blendSourceAlpha());
            case GL14.GL_BLEND_DST_ALPHA -> blendEnum(state().blendDestinationAlpha());
            case GL14.GL_BLEND_EQUATION_RGB -> equationEnum(state().blendEquation());
            case GL14.GL_BLEND_EQUATION_ALPHA -> equationEnum(state().blendEquationAlpha());
            case GL_CULL_FACE_MODE -> switch (state().culledFaces()) {
                case FRONT -> GL_FRONT;
                case BACK -> GL_BACK;
                case FRONT_AND_BACK -> GL_FRONT_AND_BACK;
            };
            case GL_FRONT_FACE -> state().frontFace() == Winding.CLOCKWISE ? GL_CW : GL_CCW;
            default -> throw new UnsupportedOperationException(
                    "glGetInteger does not emulate 0x" + Integer.toHexString(name));
        };
    }
    /** the vector forms fill the buffer from its current position, without moving it. */
    public static void glGetInteger(int name, IntBuffer values) {
        int[] read = switch (name) {
            case GL_VIEWPORT -> state().viewportBox();
            case GL_SCISSOR_BOX -> state().scissorBox();
            default -> new int[] {glGetInteger(name)};
        };
        put(values, read, name);
    }
    public static float glGetFloat(int name) {
        return switch (name) {
            case GL_DEPTH_CLEAR_VALUE -> (float) state().clearDepthValue();
            default -> {
                float[] vector = floatVector(name);
                if (vector.length != 1) {
                    throw new UnsupportedOperationException(
                            "glGetFloat: 0x" + Integer.toHexString(name) + " is a vector of "
                                    + vector.length + "; read it into a FloatBuffer");
                }
                yield vector[0];
            }
        };
    }
    public static void glGetFloat(int name, FloatBuffer values) {
        float[] read = floatVector(name);
        if (values.remaining() < read.length) {
            throw new IllegalArgumentException("glGetFloat: 0x" + Integer.toHexString(name)
                    + " needs room for " + read.length + " floats, got " + values.remaining());
        }
        values.duplicate().put(read);
    }
    static float[] floatVector(int name) {
        return switch (name) {
            case GL_MODELVIEW_MATRIX -> state().modelView().clone();
            case GL_PROJECTION_MATRIX -> state().projection().clone();
            case GL_CURRENT_COLOR -> state().currentColor();
            case GL_COLOR_CLEAR_VALUE -> state().clearColorValue();
            case GL_DEPTH_CLEAR_VALUE -> new float[] {(float) state().clearDepthValue()};
            default -> throw new UnsupportedOperationException(
                    "glGetFloat does not emulate 0x" + Integer.toHexString(name));
        };
    }
    public static boolean glGetBoolean(int name) {
        return switch (name) {
            case GL_DEPTH_WRITEMASK -> state().depthWrite();
            default -> throw new UnsupportedOperationException(
                    "glGetBoolean does not emulate 0x" + Integer.toHexString(name));
        };
    }
    static void put(IntBuffer values, int[] read, int name) {
        if (values.remaining() < read.length) {
            throw new IllegalArgumentException("glGetInteger: 0x" + Integer.toHexString(name)
                    + " needs room for " + read.length + " ints, got " + values.remaining());
        }
        values.duplicate().put(read);
    }
    static int compareEnum(CompareFunc func) {
        return switch (func) {
            case NEVER -> GL_NEVER;
            case LESS -> GL_LESS;
            case EQUAL -> GL_EQUAL;
            case LESS_OR_EQUAL -> GL_LEQUAL;
            case GREATER -> GL_GREATER;
            case NOT_EQUAL -> GL_NOTEQUAL;
            case GREATER_OR_EQUAL -> GL_GEQUAL;
            case ALWAYS -> GL_ALWAYS;
        };
    }
    static int equationEnum(BlendEquation equation) {
        return switch (equation) {
            case ADD -> GL14.GL_FUNC_ADD;
            case SUBTRACT -> GL14.GL_FUNC_SUBTRACT;
            case REVERSE_SUBTRACT -> GL14.GL_FUNC_REVERSE_SUBTRACT;
            case MIN -> GL14.GL_MIN;
            case MAX -> GL14.GL_MAX;
        };
    }
    static int blendEnum(BlendFactor factor) {
        return switch (factor) {
            case ZERO -> GL_ZERO;
            case ONE -> GL_ONE;
            case SRC_COLOR -> GL_SRC_COLOR;
            case ONE_MINUS_SRC_COLOR -> GL_ONE_MINUS_SRC_COLOR;
            case DST_COLOR -> GL_DST_COLOR;
            case ONE_MINUS_DST_COLOR -> GL_ONE_MINUS_DST_COLOR;
            case SRC_ALPHA -> GL_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> GL_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> GL_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> GL_ONE_MINUS_DST_ALPHA;
            case SRC_ALPHA_SATURATE -> GL_SRC_ALPHA_SATURATE;
            case CONSTANT_COLOR -> GL14.GL_CONSTANT_COLOR;
            case ONE_MINUS_CONSTANT_COLOR -> GL14.GL_ONE_MINUS_CONSTANT_COLOR;
            case CONSTANT_ALPHA -> GL14.GL_CONSTANT_ALPHA;
            case ONE_MINUS_CONSTANT_ALPHA -> GL14.GL_ONE_MINUS_CONSTANT_ALPHA;
        };
    }
    static void loadMatrix(float[] matrix) {
        if (recording() && state().record(() -> state().loadMatrix(matrix))) {
            return;
        }
        state().loadMatrix(matrix);
    }
    static void multMatrix(float[] matrix) {
        if (recording() && state().record(() -> state().multMatrix(matrix))) {
            return;
        }
        state().multMatrix(matrix);
    }
    /** copied rather than referenced: the caller may reuse the array before a list replays it. */
    static float[] require16(float[] matrix, String call) {
        if (matrix.length < 16) {
            throw new IllegalArgumentException(
                    call + " needs 16 floats, got " + matrix.length);
        }
        float[] copy = new float[16];
        System.arraycopy(matrix, 0, copy, 0, 16);
        return copy;
    }
    static float[] read16(FloatBuffer matrix, String call) {
        if (matrix.remaining() < 16) {
            throw new IllegalArgumentException(
                    call + " needs 16 floats, got " + matrix.remaining());
        }
        float[] copy = new float[16];
        matrix.duplicate().get(copy);
        return copy;
    }
    /** the coordinate belongs to a unit; {@link GL13#glMultiTexCoord2f} names a different one. */
    static void texCoord(int unit, float s, float t) {
        if (recording() && !state().isBuilding()
                && state().record(() -> state().texCoord(unit, s, t))) {
            return;
        }
        state().texCoord(unit, s, t);
    }
    static void lightScalar(int light, int parameter, float value) {
        Lighting lighting = state().lighting();
        switch (parameter) {
            case GL_CONSTANT_ATTENUATION -> lighting.setAttenuation(light, 0, value);
            case GL_LINEAR_ATTENUATION -> lighting.setAttenuation(light, 1, value);
            case GL_QUADRATIC_ATTENUATION -> lighting.setAttenuation(light, 2, value);
            default -> throw new UnsupportedOperationException(
                    "glLightf does not emulate parameter 0x" + Integer.toHexString(parameter));
        }
    }
    static void lightVector(int light, int parameter, float[] values) {
        Lighting lighting = state().lighting();
        switch (parameter) {
            // gl fixes a light's position in eye space the moment it is set
            case GL_POSITION -> lighting.setPosition(light, Matrix4.transform(state().modelView(),
                    values[0], values[1], values[2], values[3]));
            case GL_AMBIENT -> lighting.setAmbient(light, values);
            case GL_DIFFUSE -> lighting.setDiffuse(light, values);
            case GL_SPECULAR -> lighting.setSpecular(light, values);
            case GL_CONSTANT_ATTENUATION, GL_LINEAR_ATTENUATION, GL_QUADRATIC_ATTENUATION ->
                    lightScalar(light, parameter, values[0]);
            default -> throw new UnsupportedOperationException(
                    "glLight does not emulate parameter 0x" + Integer.toHexString(parameter));
        }
    }
    static int lightIndex(int light, String call) {
        if (light < GL_LIGHT0 || light > GL_LIGHT7) {
            throw new IllegalArgumentException(
                    call + " wants GL_LIGHT0 through GL_LIGHT7, got 0x"
                            + Integer.toHexString(light));
        }
        return Lighting.requireLight(light - GL_LIGHT0);
    }
    static void material(int parameter, float[] values) {
        Lighting lighting = state().lighting();
        switch (parameter) {
            case GL_AMBIENT -> System.arraycopy(values, 0, lighting.materialAmbient(), 0, 4);
            case GL_DIFFUSE -> System.arraycopy(values, 0, lighting.materialDiffuse(), 0, 4);
            case GL_SPECULAR -> System.arraycopy(values, 0, lighting.materialSpecular(), 0, 4);
            case GL_EMISSION -> System.arraycopy(values, 0, lighting.materialEmission(), 0, 4);
            case GL_AMBIENT_AND_DIFFUSE -> {
                System.arraycopy(values, 0, lighting.materialAmbient(), 0, 4);
                System.arraycopy(values, 0, lighting.materialDiffuse(), 0, 4);
            }
            default -> throw new UnsupportedOperationException(
                    "glMaterial does not emulate parameter 0x" + Integer.toHexString(parameter));
        }
    }
    /** the shader keeps one material, so a face-by-face material cannot be honored. */
    static void requireBothFaces(int face, String call) {
        if (face != GL_FRONT_AND_BACK) {
            throw new UnsupportedOperationException(
                    call + " emulates GL_FRONT_AND_BACK only: the fixed-function shader here "
                            + "carries a single material, so front and back cannot differ");
        }
    }
    static void fog(int parameter, float value) {
        Lighting lighting = state().lighting();
        switch (parameter) {
            case GL_FOG_MODE -> lighting.setFogMode(fogMode((int) value));
            case GL_FOG_DENSITY -> lighting.setFogDensity(value);
            case GL_FOG_START -> lighting.setFogStart(value);
            case GL_FOG_END -> lighting.setFogEnd(value);
            default -> throw new UnsupportedOperationException(
                    "glFog does not emulate parameter 0x" + Integer.toHexString(parameter));
        }
    }
    static int fogMode(int mode) {
        return switch (mode) {
            case GL_LINEAR -> Lighting.LINEAR;
            case GL_EXP -> Lighting.EXP;
            case GL_EXP2 -> Lighting.EXP2;
            default -> throw new IllegalArgumentException(
                    "glFog wants GL_LINEAR, GL_EXP or GL_EXP2, got 0x"
                            + Integer.toHexString(mode));
        };
    }
    static float[] require4(float[] values, String call) {
        if (values.length < 4) {
            throw new IllegalArgumentException(call + " wants four components, got "
                    + values.length);
        }
        return java.util.Arrays.copyOf(values, 4);
    }
    static float[] read4(FloatBuffer values, String call) {
        if (values.remaining() < 4) {
            throw new IllegalArgumentException(call + " wants four components, got "
                    + values.remaining());
        }
        float[] copied = new float[4];
        values.duplicate().get(copied);
        return copied;
    }
    public static void glDrawArrays(int mode, int first, int count) {
        state().refuseWhileRecording("glDrawArrays");
        state().drawArrays(primitive(mode), first, count);
    }
    public static void glDrawElements(int mode, int count, int type, long offset) {
        state().refuseWhileRecording("glDrawElements");
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("index offset out of range: " + offset);
        }
        state().drawElements(primitive(mode), count, indexType(type), (int) offset);
    }
    static void pointIntoBuffer(ClientArray array, int size, AttributeType type,
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
    // textures

    public static int glGenTextures() {
        return device().createTexture();
    }
    public static void glDeleteTextures(int texture) {
        state().deleteTexture(texture);
    }
    public static void glBindTexture(int target, int texture) {
        requireTexture2D(target, "glBindTexture");
        if (recording() && state().record(() -> state().bindTexture(texture))) {
            return;
        }
        state().bindTexture(texture);
    }
    public static void glTexParameteri(int target, int name, int value) {
        requireTexture2D(target, "glTexParameteri");
        state().refuseWhileRecording("glTexParameteri");
        switch (name) {
            case GL_TEXTURE_MIN_FILTER ->
                    device().setTextureMinFilter(filter(value, "GL_TEXTURE_MIN_FILTER"));
            case GL_TEXTURE_MAG_FILTER ->
                    device().setTextureMagFilter(magFilter(value));
            case GL_TEXTURE_WRAP_S -> device().setTextureWrapS(wrap(value));
            case GL_TEXTURE_WRAP_T -> device().setTextureWrapT(wrap(value));
            case GL_TEXTURE_BASE_LEVEL -> device().setTextureBaseLevel(requireLevel(value));
            case GL_TEXTURE_MAX_LEVEL -> device().setTextureMaxLevel(requireLevel(value));
            case GL_TEXTURE_MIN_LOD -> device().setTextureMinLod(value);
            case GL_TEXTURE_MAX_LOD -> device().setTextureMaxLod(value);
            // webgl has no level bias, and a bias only changes which mipmap level a texture uses
            case GL_TEXTURE_LOD_BIAS -> { }
            case GL_GENERATE_MIPMAP -> generateMipmap(value != GL_FALSE);
            default -> throw new UnsupportedOperationException(
                    "glTexParameteri does not yet emulate parameter 0x"
                            + Integer.toHexString(name));
        }
    }
    public static void glTexParameterf(int target, int name, float value) {
        if (name != GL_TEXTURE_MIN_LOD && name != GL_TEXTURE_MAX_LOD) {
            glTexParameteri(target, name, (int) value);
            return;
        }
        requireTexture2D(target, "glTexParameterf");
        state().refuseWhileRecording("glTexParameterf");
        if (name == GL_TEXTURE_MIN_LOD) {
            device().setTextureMinLod(value);
        } else {
            device().setTextureMaxLod(value);
        }
    }
    /**
     * the rows an upload reads from. gl walks the caller's memory with these; here they pick the
     * rectangle out of the buffer before it reaches the device, which comes to the same picture.
     */
    public static void glPixelStorei(int name, int value) {
        state().refuseWhileRecording("glPixelStorei");
        switch (name) {
            case GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_ROWS, GL_UNPACK_SKIP_PIXELS,
                 GL_PACK_ROW_LENGTH, GL_PACK_SKIP_ROWS, GL_PACK_SKIP_PIXELS -> {
                if (value < 0) {
                    throw new IllegalArgumentException(
                            "glPixelStorei takes no negative count, got " + value);
                }
                state().setPixelStore(name, value);
            }
            case GL_UNPACK_ALIGNMENT, GL_PACK_ALIGNMENT -> {
                if (value != 1 && value != 2 && value != 4 && value != 8) {
                    throw new IllegalArgumentException(
                            "a pixel row aligns to 1, 2, 4 or 8 bytes, got " + value);
                }
                state().setPixelStore(name, value);
            }
            case GL_UNPACK_SWAP_BYTES, GL_UNPACK_LSB_FIRST, GL_PACK_SWAP_BYTES,
                 GL_PACK_LSB_FIRST -> {
                if (value != GL_FALSE) {
                    throw new UnsupportedOperationException(
                            "glPixelStorei does not emulate 0x" + Integer.toHexString(name)
                                    + ": a pixel is read here in the order it lies");
                }
            }
            default -> throw new UnsupportedOperationException(
                    "glPixelStorei does not emulate parameter 0x" + Integer.toHexString(name));
        }
    }
    /** @return what the last upload to the level said, which is where gl reads it from too */
    public static int glGetTexLevelParameteri(int target, int level, int name) {
        boolean proxy = target == GL_PROXY_TEXTURE_2D;
        if (!proxy) {
            requireTexture2D(target, "glGetTexLevelParameteri");
        }
        state().refuseWhileRecording("glGetTexLevelParameteri");
        requireLevel(level);
        int[] size = proxy ? state().proxySize() : state().levelSize(level);
        return switch (name) {
            case GL_TEXTURE_WIDTH -> size[0];
            case GL_TEXTURE_HEIGHT -> size[1];
            case GL_TEXTURE_INTERNAL_FORMAT -> GL_RGBA;
            case GL_TEXTURE_BORDER -> 0;
            default -> throw new UnsupportedOperationException(
                    "glGetTexLevelParameteri does not emulate parameter 0x"
                            + Integer.toHexString(name));
        };
    }
    /**
     * webgl reads back from a framebuffer only, so the level is attached to one and read from
     * there. the texture and framebuffer that were bound are put back before this returns.
     */
    public static void glGetTexImage(int target, int level, int format, int type,
                                     IntBuffer pixels) {
        requireTexture2D(target, "glGetTexImage");
        state().refuseWhileRecording("glGetTexImage");
        requireLevel(level);
        requireRgbaBytes(format, type);
        int[] size = state().levelSize(level);
        if (size[0] == 0 || size[1] == 0) {
            throw new IllegalStateException(
                    "glGetTexImage: nothing was uploaded to level " + level + " of the bound "
                            + "texture, so there is no size to read back");
        }
        int count = size[0] * size[1];
        if (pixels.remaining() < count) {
            throw new IllegalArgumentException("glGetTexImage needs room for " + count
                    + " pixels, got " + pixels.remaining());
        }
        int texture = state().boundTexture();
        int was = device().boundFramebuffer();
        int reader = device().createFramebuffer();
        byte[] read;
        try {
            device().bindFramebuffer(reader);
            device().framebufferTexture(FramebufferAttachment.COLOR, texture, level);
            read = device().readPixels(0, 0, size[0], size[1]);
        } finally {
            device().bindFramebuffer(was);
            device().deleteFramebuffer(reader);
        }
        int at = pixels.position();
        for (int pixel = 0; pixel < count; pixel++) {
            int from = pixel * 4;
            pixels.put(at + pixel, (read[from + 3] & 0xFF) << 24 | (read[from] & 0xFF) << 16
                    | (read[from + 1] & 0xFF) << 8 | (read[from + 2] & 0xFF));
        }
    }
    static TextureFilter filter(int value, String parameter) {
        return switch (value) {
            case GL_NEAREST -> TextureFilter.NEAREST;
            case GL_LINEAR -> TextureFilter.LINEAR;
            case GL_NEAREST_MIPMAP_NEAREST -> TextureFilter.NEAREST_MIPMAP_NEAREST;
            case GL_LINEAR_MIPMAP_NEAREST -> TextureFilter.LINEAR_MIPMAP_NEAREST;
            case GL_NEAREST_MIPMAP_LINEAR -> TextureFilter.NEAREST_MIPMAP_LINEAR;
            case GL_LINEAR_MIPMAP_LINEAR -> TextureFilter.LINEAR_MIPMAP_LINEAR;
            default -> throw new UnsupportedOperationException(
                    parameter + " does not emulate filter 0x" + Integer.toHexString(value));
        };
    }
    /**
     * gl's magnification filter samples one level, so a mipmapped mode is not a legal value for it.
     */
    static TextureFilter magFilter(int value) {
        TextureFilter filter = filter(value, "GL_TEXTURE_MAG_FILTER");
        if (filter != TextureFilter.NEAREST && filter != TextureFilter.LINEAR) {
            throw new IllegalArgumentException(
                    "GL_TEXTURE_MAG_FILTER takes GL_NEAREST or GL_LINEAR; there is nothing to "
                            + "magnify from a smaller level");
        }
        return filter;
    }
    static int requireLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("a mipmap level cannot be negative: " + level);
        }
        return level;
    }
    /**
     * GL_GENERATE_MIPMAP asks gl to rebuild the levels whenever the base changes. webgl has only
     * the explicit glGenerateMipmap, so the levels are built now and after each later upload.
     */
    static void generateMipmap(boolean enabled) {
        state().setAutomaticMipmaps(enabled);
        if (enabled) {
            device().generateMipmap();
        }
    }
    static TextureWrap wrap(int value) {
        return switch (value) {
            case GL_REPEAT -> TextureWrap.REPEAT;
            case GL_CLAMP, GL_CLAMP_TO_EDGE -> TextureWrap.CLAMP_TO_EDGE;
            case GL_MIRRORED_REPEAT -> TextureWrap.MIRRORED_REPEAT;
            default -> throw new UnsupportedOperationException(
                    "glTexParameteri does not emulate wrap mode 0x" + Integer.toHexString(value));
        };
    }
    /** uploads rgba pixels, one int per pixel in 0xAARRGGBB order. */
    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type, int[] pixels) {
        if (proxied(target, level, width, height)) {
            return;
        }
        requireTexture2D(target, "glTexImage2D");
        state().refuseWhileRecording("glTexImage2D");
        requireLevel(level);
        requireRgbaBytes(format, type);
        state().recordLevelSize(level, width, height);
        device().textureImage(level, width, height, spread(pixels, width * height));
        rebuildMipmaps();
    }
    /**
     * the same upload from a buffer of bytes in r, g, b, a order, which is what real lwjgl takes
     */
    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type,
                                    ByteBuffer pixels) {
        if (proxied(target, level, width, height)) {
            return;
        }
        requireTexture2D(target, "glTexImage2D");
        state().refuseWhileRecording("glTexImage2D");
        requireLevel(level);
        requirePixelType(type, format, "glTexImage2D");
        // a null buffer reserves the level, which is how a colour attachment is made
        state().recordLevelSize(level, width, height);
        if (pixels == null) {
            device().textureStorage(level, width, height);
            return;
        }
        int pixelCount = width * height;
        byte[] read = bytes(pixels, pixelCount * components(format, "glTexImage2D"));
        device().textureImage(level, width, height, toRgba(read, pixelCount, format));
        rebuildMipmaps();
    }
    public static void glTexImage2D(int target, int level, int internalFormat, int width,
                                    int height, int border, int format, int type,
                                    IntBuffer pixels) {
        if (proxied(target, level, width, height)) {
            return;
        }
        requireTexture2D(target, "glTexImage2D");
        state().refuseWhileRecording("glTexImage2D");
        requireLevel(level);
        requireRgbaBytes(format, type);
        state().recordLevelSize(level, width, height);
        if (pixels == null) {
            device().textureStorage(level, width, height);
            return;
        }
        device().textureImage(level, width, height,
                spread(unpack(pixels, width, height), width * height));
        rebuildMipmaps();
    }
    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                       int format, int type, ByteBuffer pixels) {
        requireTexture2D(target, "glTexSubImage2D");
        state().refuseWhileRecording("glTexSubImage2D");
        requireLevel(level);
        requirePixelType(type, format, "glTexSubImage2D");
        int pixelCount = width * height;
        byte[] read = bytes(pixels, pixelCount * components(format, "glTexSubImage2D"));
        device().textureSubImage(level, x, y, width, height, toRgba(read, pixelCount, format));
        rebuildMipmaps();
    }
    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                       int format, int type, IntBuffer pixels) {
        requireTexture2D(target, "glTexSubImage2D");
        state().refuseWhileRecording("glTexSubImage2D");
        requireLevel(level);
        requireRgbaBytes(format, type);
        device().textureSubImage(level, x, y, width, height,
                spread(unpack(pixels, width, height), width * height));
        rebuildMipmaps();
    }
    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                       int format, int type, int[] pixels) {
        requireTexture2D(target, "glTexSubImage2D");
        state().refuseWhileRecording("glTexSubImage2D");
        requireLevel(level);
        requireRgbaBytes(format, type);
        device().textureSubImage(level, x, y, width, height, spread(pixels, width * height));
        rebuildMipmaps();
    }
    static void rebuildMipmaps() {
        if (state().automaticMipmaps()) {
            device().generateMipmap();
        }
    }
    /** @return the bytes as they lie, read in bulk and without consuming the caller's buffer */
    static byte[] bytes(ByteBuffer pixels, int count) {
        byte[] rgba = new byte[count];
        pixels.duplicate().get(rgba);
        return rgba;
    }
    /**
     * @return the rectangle glPixelStorei points at, copied out of the buffer row by row
     */
    static int[] unpack(IntBuffer pixels, int width, int height) {
        int rowLength = state().pixelStore(GL_UNPACK_ROW_LENGTH, 0);
        int stride = rowLength == 0 ? width : rowLength;
        int skipRows = state().pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        int skipPixels = state().pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        IntBuffer source = pixels.duplicate();
        int at = source.position() + skipRows * stride + skipPixels;
        int[] copied = new int[width * height];
        for (int row = 0; row < height; row++) {
            source.position(at + row * stride);
            source.get(copied, row * width, width);
        }
        return copied;
    }
    /** @return 0xAARRGGBB ints laid back out as the r, g, b, a bytes a texture takes */
    static byte[] spread(int[] pixels, int count) {
        if (pixels.length < count) {
            throw new IllegalArgumentException(
                    "need " + count + " pixels, got " + pixels.length);
        }
        byte[] rgba = new byte[count * 4];
        for (int pixel = 0, out = 0; pixel < count; pixel++) {
            int argb = pixels[pixel];
            rgba[out++] = (byte) (argb >> 16);
            rgba[out++] = (byte) (argb >> 8);
            rgba[out++] = (byte) argb;
            rgba[out++] = (byte) (argb >>> 24);
        }
        return rgba;
    }
    /** one int per pixel is 0xAARRGGBB, which is what GL_BGRA with the packed type names */
    static void requireRgbaBytes(int format, int type) {
        boolean bytes = format == GL_RGBA && type == GL_UNSIGNED_BYTE;
        boolean packed = format == GL12.GL_BGRA && type == GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
        if (!bytes && !packed) {
            throw new UnsupportedOperationException(
                    "a texture uploaded from ints emulates GL_RGBA/GL_UNSIGNED_BYTE and "
                            + "GL_BGRA/GL_UNSIGNED_INT_8_8_8_8_REV only");
        }
    }
    /**
     * a packed 8_8_8_8_REV pixel puts its first component in the lowest bits, so on the
     * little-endian machines browsers run on it lies in memory as the bytes of its own format.
     * the buffer is read as those bytes, which is what the device takes anyway.
     */
    static void requirePixelType(int type, int format, String call) {
        if (type == GL_UNSIGNED_BYTE) {
            return;
        }
        if (type == GL12.GL_UNSIGNED_INT_8_8_8_8_REV && components(format, call) == 4) {
            return;
        }
        throw new UnsupportedOperationException(
                call + " emulates GL_UNSIGNED_BYTE pixels, and GL_UNSIGNED_INT_8_8_8_8_REV for a "
                        + "four-component format, got 0x" + Integer.toHexString(type));
    }
    /** @return how many bytes of the buffer one pixel of the format takes */
    static int components(int format, String call) {
        return switch (format) {
            case GL_RGBA, GL12.GL_BGRA -> 4;
            case GL_RGB, GL12.GL_BGR -> 3;
            case GL_LUMINANCE_ALPHA -> 2;
            case GL_LUMINANCE, GL_ALPHA -> 1;
            default -> throw new UnsupportedOperationException(
                    call + " does not emulate format 0x" + Integer.toHexString(format));
        };
    }
    /**
     * webgl2 takes rgba bytes, so the narrower formats are widened here rather than at the device.
     */
    static byte[] toRgba(byte[] source, int pixels, int format) {
        if (format == GL_RGBA) {
            return source;
        }
        byte[] rgba = new byte[pixels * 4];
        int step = components(format, "glTexImage2D");
        for (int pixel = 0; pixel < pixels; pixel++) {
            int from = pixel * step;
            int to = pixel * 4;
            switch (format) {
                case GL_RGB -> {
                    rgba[to] = source[from];
                    rgba[to + 1] = source[from + 1];
                    rgba[to + 2] = source[from + 2];
                    rgba[to + 3] = (byte) 0xFF;
                }
                case GL12.GL_BGR -> {
                    rgba[to] = source[from + 2];
                    rgba[to + 1] = source[from + 1];
                    rgba[to + 2] = source[from];
                    rgba[to + 3] = (byte) 0xFF;
                }
                case GL12.GL_BGRA -> {
                    rgba[to] = source[from + 2];
                    rgba[to + 1] = source[from + 1];
                    rgba[to + 2] = source[from];
                    rgba[to + 3] = source[from + 3];
                }
                case GL_LUMINANCE -> {
                    rgba[to] = source[from];
                    rgba[to + 1] = source[from];
                    rgba[to + 2] = source[from];
                    rgba[to + 3] = (byte) 0xFF;
                }
                case GL_LUMINANCE_ALPHA -> {
                    rgba[to] = source[from];
                    rgba[to + 1] = source[from];
                    rgba[to + 2] = source[from];
                    rgba[to + 3] = source[from + 1];
                }
                // an alpha texture is black where it is not transparent, the way gl samples it
                case GL_ALPHA -> rgba[to + 3] = source[from];
                default -> throw new UnsupportedOperationException(
                        "glTexImage2D does not emulate format 0x" + Integer.toHexString(format));
            }
        }
        return rgba;
    }
    /** webgl has no proxy target, so the largest texture the device takes gives the answer */
    private static boolean proxied(int target, int level, int width, int height) {
        if (target != GL_PROXY_TEXTURE_2D) {
            return false;
        }
        requireLevel(level);
        int largest = device().limit(GraphicsLimit.MAX_TEXTURE_SIZE);
        boolean fits = width <= largest && height <= largest;
        state().recordProxySize(fits ? width : 0, fits ? height : 0);
        return true;
    }

    static void requireTexture2D(int target, String call) {
        if (target != GL_TEXTURE_2D) {
            throw new UnsupportedOperationException(
                    call + " emulates GL_TEXTURE_2D only, got 0x" + Integer.toHexString(target));
        }
    }
    // translation

    static DrawMode primitive(int mode) {
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
    static AttributeType componentType(int type, String call) {
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
    static IndexType indexType(int type) {
        return switch (type) {
            case GL_UNSIGNED_BYTE -> IndexType.UNSIGNED_BYTE;
            case GL_UNSIGNED_SHORT -> IndexType.UNSIGNED_SHORT;
            case GL_UNSIGNED_INT -> IndexType.UNSIGNED_INT;
            default -> throw new IllegalArgumentException(
                    "glDrawElements: indices must be unsigned, got 0x" + Integer.toHexString(type));
        };
    }
    static CompareFunc compareFunc(int func, String call) {
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
    static BlendFactor blendFactor(int factor, String call) {
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
            case GL_SRC_ALPHA_SATURATE -> BlendFactor.SRC_ALPHA_SATURATE;
            case GL14.GL_CONSTANT_COLOR -> BlendFactor.CONSTANT_COLOR;
            case GL14.GL_ONE_MINUS_CONSTANT_COLOR -> BlendFactor.ONE_MINUS_CONSTANT_COLOR;
            case GL14.GL_CONSTANT_ALPHA -> BlendFactor.CONSTANT_ALPHA;
            case GL14.GL_ONE_MINUS_CONSTANT_ALPHA -> BlendFactor.ONE_MINUS_CONSTANT_ALPHA;
            default -> throw new IllegalArgumentException(
                    call + ": unknown blend factor 0x" + Integer.toHexString(factor));
        };
    }
}
