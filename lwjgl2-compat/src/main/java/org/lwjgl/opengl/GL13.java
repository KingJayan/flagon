package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.Ops;

/**
 * gl 1.3 over webgl2: the core profile of {@link GL13C}, and the fixed-function half here
 */
public final class GL13 extends GL13C {

    private GL13() {
    }

    public static final int GL_CLIENT_ACTIVE_TEXTURE = 0x84E1;
    public static final int GL_MAX_TEXTURE_UNITS = 0x84E2;
    /** selects the unit that {@code glTexCoordPointer} and the coordinate array act on. */
    public static void glClientActiveTexture(int unit) {
        int selected = unitOf(unit, "glClientActiveTexture");
        if (state().isRecording() && state().record(Ops.CLIENT_ACTIVE_TEXTURE, selected)) {
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
}
