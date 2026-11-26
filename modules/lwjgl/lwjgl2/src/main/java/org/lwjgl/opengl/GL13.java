package org.lwjgl.opengl;

/**
 * gl 1.3 over webgl2: the core profile of {@link GL13C}, and the fixed-function half here
 */
public final class GL13 extends GL13C {

    private GL13() {
    }

    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0x84E1;
    public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;
    public static final int GL_SUBTRACT = 0x84E7;
    public static final int GL_COMBINE = 0x8570;
    public static final int GL_COMBINE_RGB = 0x8571;
    public static final int GL_COMBINE_ALPHA = 0x8572;
    public static final int GL_RGB_SCALE = 0x8573;
    public static final int GL_ADD_SIGNED = 0x8574;
    public static final int GL_INTERPOLATE = 0x8575;
    public static final int GL_CONSTANT = 0x8576;
    public static final int GL_PRIMARY_COLOR = 0x8577;
    public static final int GL_PREVIOUS = 0x8578;
    public static final int GL_SOURCE0_RGB = 0x8580;
    public static final int GL_SOURCE1_RGB = 0x8581;
    public static final int GL_SOURCE2_RGB = 0x8582;
    public static final int GL_SOURCE0_ALPHA = 0x8588;
    public static final int GL_SOURCE1_ALPHA = 0x8589;
    public static final int GL_SOURCE2_ALPHA = 0x858A;
    public static final int GL_OPERAND0_RGB = 0x8590;
    public static final int GL_OPERAND1_RGB = 0x8591;
    public static final int GL_OPERAND2_RGB = 0x8592;
    public static final int GL_OPERAND0_ALPHA = 0x8598;
    public static final int GL_OPERAND1_ALPHA = 0x8599;
    public static final int GL_OPERAND2_ALPHA = 0x859A;
    public static final int GL_DOT3_RGB = 0x86AE;
    public static final int GL_DOT3_RGBA = 0x86AF;
    /**
     * selects the unit that {@code glTexCoordPointer} and the coordinate array act on. it is
     * client state, so like the pointers it acts at once, even while a list is compiled.
     */
    public static void glClientActiveTexture(int unit) {
        state().clientActiveTexture(unitOf(unit, "glClientActiveTexture"));
    }
    public static void glMultiTexCoord1f(int unit, float s) {
        GL11.texCoord(unitOf(unit, "glMultiTexCoord1f"), s, 0.0f);
    }
    public static void glMultiTexCoord2f(int unit, float s, float t) {
        GL11.texCoord(unitOf(unit, "glMultiTexCoord2f"), s, t);
    }
    /** the third coordinate is dropped: the units here sample two-dimensional textures. */
    public static void glMultiTexCoord3f(int unit, float s, float t, float r) {
        GL11.texCoord(unitOf(unit, "glMultiTexCoord3f"), s, t);
    }
}
