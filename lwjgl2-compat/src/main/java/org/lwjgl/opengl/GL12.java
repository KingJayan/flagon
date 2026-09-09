package org.lwjgl.opengl;

/**
 * the gl 1.2 entry points, which are the ones applications reach for after GL11
 */
public final class GL12 {

    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_MAX_ELEMENTS_VERTICES = 0x80E8;
    public static final int GL_MAX_ELEMENTS_INDICES = 0x80E9;
    public static final int GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;
    public static final int GL_BGR = 0x80E0;
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_RESCALE_NORMAL = 0x803A;

    private GL12() {
    }

    /**
     * the range is a hint desktop drivers use to decide what to prefetch; webgl has no equivalent,
     * so it is checked and then dropped.
     */
    public static void glDrawRangeElements(int mode, int start, int end, int count, int type,
                                           long offset) {
        if (end < start) {
            throw new IllegalArgumentException(
                    "glDrawRangeElements: end " + end + " is below start " + start);
        }
        GL11.glDrawElements(mode, count, type, offset);
    }

}
