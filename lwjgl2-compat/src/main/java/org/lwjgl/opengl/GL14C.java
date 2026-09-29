package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.Ops;
import dev.flagon.platform.graphics.BlendEquation;
import dev.flagon.platform.graphics.BlendFactor;

import java.nio.IntBuffer;

/**
 * the core-profile half of gl 1.4: separate blending and point parameters
 */
public class GL14C {

    protected GL14C() {
    }

    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;

    public static final int GL_CONSTANT_COLOR = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_BLEND_COLOR = 0x8005;
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_MIN = 0x8007;
    public static final int GL_MAX = 0x8008;
    public static final int GL_FUNC_SUBTRACT = 0x800A;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
    public static final int GL_BLEND_EQUATION = 0x8009;
    public static final int GL_BLEND_EQUATION_RGB = 0x8009;
    public static final int GL_BLEND_EQUATION_ALPHA = 0x883D;
    public static final int GL_BLEND_SRC_RGB = 0x80C9;
    public static final int GL_BLEND_DST_RGB = 0x80C8;
    public static final int GL_BLEND_SRC_ALPHA = 0x80CB;
    public static final int GL_BLEND_DST_ALPHA = 0x80CA;
    public static final int GL_POINT_SIZE_MIN = 0x8126;
    public static final int GL_POINT_SIZE_MAX = 0x8127;
    public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0x8128;
    public static final int GL_POINT_DISTANCE_ATTENUATION = 0x8129;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    static GLState state() {
        return GLState.get();
    }
    static boolean recording() {
        return state().isRecording();
    }
    public static void glBlendFuncSeparate(int sourceColor, int destinationColor,
                                           int sourceAlpha, int destinationAlpha) {
        BlendFactor sc = GL11.blendFactor(sourceColor, "glBlendFuncSeparate");
        BlendFactor dc = GL11.blendFactor(destinationColor, "glBlendFuncSeparate");
        BlendFactor sa = GL11.blendFactor(sourceAlpha, "glBlendFuncSeparate");
        BlendFactor da = GL11.blendFactor(destinationAlpha, "glBlendFuncSeparate");
        if (recording() && state().record(Ops.BLEND_FUNC_SEPARATE, sc.ordinal(), dc.ordinal(),
                sa.ordinal(), da.ordinal())) {
            return;
        }
        state().setBlendFuncSeparate(sc, dc, sa, da);
    }
    public static void glBlendEquation(int mode) {
        BlendEquation equation = equation(mode, "glBlendEquation");
        if (recording() && state().record(Ops.BLEND_EQUATION, equation.ordinal())) {
            return;
        }
        state().setBlendEquation(equation);
    }
    public static void glBlendEquationSeparate(int color, int alpha) {
        BlendEquation rgb = equation(color, "glBlendEquationSeparate");
        BlendEquation a = equation(alpha, "glBlendEquationSeparate");
        if (recording()
                && state().record(Ops.BLEND_EQUATION_SEPARATE, rgb.ordinal(), a.ordinal())) {
            return;
        }
        state().setBlendEquationSeparate(rgb, a);
    }
    public static void glBlendColor(float red, float green, float blue, float alpha) {
        if (recording() && state().record(Ops.BLEND_COLOR, red, green, blue, alpha)) {
            return;
        }
        state().setBlendColor(red, green, blue, alpha);
    }
    static BlendEquation equation(int mode, String call) {
        return switch (mode) {
            case GL_FUNC_ADD -> BlendEquation.ADD;
            case GL_FUNC_SUBTRACT -> BlendEquation.SUBTRACT;
            case GL_FUNC_REVERSE_SUBTRACT -> BlendEquation.REVERSE_SUBTRACT;
            case GL_MIN -> BlendEquation.MIN;
            case GL_MAX -> BlendEquation.MAX;
            default -> throw new IllegalArgumentException(
                    call + ": unknown blend equation 0x" + Integer.toHexString(mode));
        };
    }
    /** one draw per range, which is what a desktop driver does with the arrays anyway. */
    public static void glMultiDrawArrays(int mode, IntBuffer first, IntBuffer count) {
        if (first.remaining() != count.remaining()) {
            throw new IllegalArgumentException(
                    "glMultiDrawArrays: " + first.remaining() + " first values against "
                            + count.remaining() + " counts");
        }
        for (int draw = 0; draw < first.remaining(); draw++) {
            GL11.glDrawArrays(mode, first.get(first.position() + draw),
                    count.get(count.position() + draw));
        }
    }
    public static void glPointParameterf(int parameter, float value) {
        switch (parameter) {
            case GL_POINT_SIZE_MIN -> state().setPointSizeMin(value);
            case GL_POINT_SIZE_MAX -> state().setPointSizeMax(value);
            // fading a point by its alpha needs the distance the shader does not carry
            case GL_POINT_FADE_THRESHOLD_SIZE, GL_POINT_DISTANCE_ATTENUATION ->
                    throw new UnsupportedOperationException(
                            "glPointParameter does not emulate 0x" + Integer.toHexString(parameter)
                                    + ": gl es sizes a point in the vertex shader, with no "
                                    + "distance term and no fade");
            default -> throw new IllegalArgumentException(
                    "glPointParameter: unknown parameter 0x" + Integer.toHexString(parameter));
        }
    }
    public static void glPointParameteri(int parameter, int value) {
        glPointParameterf(parameter, value);
    }
    public static void glPointParameter(int parameter, java.nio.FloatBuffer values) {
        if (values.remaining() < 1) {
            throw new IllegalArgumentException("glPointParameter wants at least one value");
        }
        glPointParameterf(parameter, values.get(values.position()));
    }
}
