package dev.flagon.lwjgl.internal;

import java.util.Arrays;

/**
 * how each texture unit folds its sample into the color, with the GL_COMBINE settings the
 * shader reads as codes. the defaults are gl's.
 */
public final class TextureEnvironment {

    public static final int MODULATE = 0;
    public static final int REPLACE = 1;
    public static final int ADD = 2;
    public static final int COMBINE = 3;
    public static final int DECAL = 4;
    public static final int BLEND = 5;

    // a combine function, as the shader numbers them
    public static final int FUNCTION_REPLACE = 0;
    public static final int FUNCTION_MODULATE = 1;
    public static final int FUNCTION_ADD = 2;
    public static final int FUNCTION_ADD_SIGNED = 3;
    public static final int FUNCTION_INTERPOLATE = 4;
    public static final int FUNCTION_SUBTRACT = 5;

    // a combine source; GL_TEXTUREn is SOURCE_UNIT + n
    public static final int SOURCE_TEXTURE = 0;
    public static final int SOURCE_CONSTANT = 1;
    public static final int SOURCE_PRIMARY_COLOR = 2;
    public static final int SOURCE_PREVIOUS = 3;
    public static final int SOURCE_UNIT = 4;

    // a combine operand; an alpha operand is one of the last two
    public static final int OPERAND_COLOR = 0;
    public static final int OPERAND_ONE_MINUS_COLOR = 1;
    public static final int OPERAND_ALPHA = 2;
    public static final int OPERAND_ONE_MINUS_ALPHA = 3;

    // where each combine setting sits in a unit's codes, which go up as four ivec4s
    public static final int RGB_FUNCTION = 0;
    public static final int RGB_SOURCE = 1;
    public static final int ALPHA_FUNCTION = 4;
    public static final int ALPHA_SOURCE = 5;
    public static final int RGB_OPERAND = 8;
    public static final int ALPHA_OPERAND = 12;
    public static final int CODES = 16;

    private static final int[] DEFAULT_CODES = {
        FUNCTION_MODULATE, SOURCE_TEXTURE, SOURCE_PREVIOUS, SOURCE_CONSTANT,
        FUNCTION_MODULATE, SOURCE_TEXTURE, SOURCE_PREVIOUS, SOURCE_CONSTANT,
        OPERAND_COLOR, OPERAND_COLOR, OPERAND_ALPHA, 0,
        OPERAND_ALPHA, OPERAND_ALPHA, OPERAND_ALPHA, 0,
    };

    private final Revision revision;
    private final int[] modes = new int[VertexArrays.UNITS];
    private final int[] codes = new int[VertexArrays.UNITS * CODES];
    private final float[] colors = new float[VertexArrays.UNITS * 4];
    private final float[] scales = new float[VertexArrays.UNITS * 2];

    TextureEnvironment(Revision revision) {
        this.revision = revision;
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            System.arraycopy(DEFAULT_CODES, 0, codes, unit * CODES, CODES);
        }
        Arrays.fill(scales, 1.0f);
    }

    public int mode(int unit) {
        return modes[unit];
    }

    public void setMode(int unit, int mode) {
        if (modes[unit] != mode) {
            revision.bump(Revision.TEXTURE);
            modes[unit] = mode;
        }
    }

    /** every unit's codes back to back, CODES to a unit */
    public int[] codes() {
        return codes;
    }

    public void setCode(int unit, int at, int code) {
        if (codes[unit * CODES + at] != code) {
            revision.bump(Revision.ENVIRONMENT);
            codes[unit * CODES + at] = code;
        }
    }

    /** every unit's GL_TEXTURE_ENV_COLOR, four floats to a unit */
    public float[] colors() {
        return colors;
    }

    public void setColor(int unit, float red, float green, float blue, float alpha) {
        revision.bump(Revision.ENVIRONMENT);
        colors[unit * 4] = red;
        colors[unit * 4 + 1] = green;
        colors[unit * 4 + 2] = blue;
        colors[unit * 4 + 3] = alpha;
    }

    /** every unit's GL_RGB_SCALE then GL_ALPHA_SCALE */
    public float[] scales() {
        return scales;
    }

    public void setScale(int unit, boolean alpha, float scale) {
        revision.bump(Revision.ENVIRONMENT);
        scales[unit * 2 + (alpha ? 1 : 0)] = scale;
    }
}
