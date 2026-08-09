package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;

/**
 * gl 1.3 multitexture over webgl2.
 *
 * <p>the compatibility shader carries four texture units, which is what fixed-function
 * multitexturing is used for; a higher unit is refused by name rather than ignored.
 */
public final class GL13 {

    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;
    public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_TEXTURE5 = 0x84C5;
    public static final int GL_TEXTURE6 = 0x84C6;
    public static final int GL_TEXTURE7 = 0x84C7;

    public static final int GL_ACTIVE_TEXTURE = 0x84E0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0x84E1;
    public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;

    private GL13() {
    }

    private static GLState state() {
        return GLState.get();
    }

    /** selects the unit that {@code glBindTexture} and the texture parameters act on. */
    public static void glActiveTexture(int unit) {
        int selected = unitOf(unit, "glActiveTexture");
        if (state().isRecording() && state().record(() -> state().activeTexture(selected))) {
            return;
        }
        state().activeTexture(selected);
    }

    /** selects the unit that {@code glTexCoordPointer} and the coordinate array act on. */
    public static void glClientActiveTexture(int unit) {
        int selected = unitOf(unit, "glClientActiveTexture");
        if (state().isRecording() && state().record(() -> state().clientActiveTexture(selected))) {
            return;
        }
        state().clientActiveTexture(selected);
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

    private static int unitOf(int unit, String call) {
        if (unit < GL_TEXTURE0) {
            throw new IllegalArgumentException(
                    call + " takes a GL_TEXTUREi name, got 0x" + Integer.toHexString(unit));
        }
        return unit - GL_TEXTURE0;
    }
}
