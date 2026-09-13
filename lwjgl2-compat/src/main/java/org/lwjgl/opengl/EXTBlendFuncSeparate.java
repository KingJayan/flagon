package org.lwjgl.opengl;

/**
 * the EXT_blend_func_separate name for what gl 1.4 took into core
 */
public final class EXTBlendFuncSeparate {

    public static final int GL_BLEND_DST_RGB_EXT = 0x80C8;
    public static final int GL_BLEND_SRC_RGB_EXT = 0x80C9;
    public static final int GL_BLEND_DST_ALPHA_EXT = 0x80CA;
    public static final int GL_BLEND_SRC_ALPHA_EXT = 0x80CB;

    private EXTBlendFuncSeparate() {
    }

    public static void glBlendFuncSeparateEXT(int sourceColor, int destinationColor,
                                              int sourceAlpha, int destinationAlpha) {
        GL14.glBlendFuncSeparate(sourceColor, destinationColor, sourceAlpha, destinationAlpha);
    }
}
