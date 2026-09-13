package org.lwjgl.opengl;

/**
 * the ARB_multitexture names for what gl 1.3 took into core
 */
public final class ARBMultitexture {

    public static final int GL_TEXTURE0_ARB = 0x84C0;
    public static final int GL_TEXTURE1_ARB = 0x84C1;
    public static final int GL_TEXTURE2_ARB = 0x84C2;
    public static final int GL_ACTIVE_TEXTURE_ARB = 0x84E0;
    public static final int GL_CLIENT_ACTIVE_TEXTURE_ARB = 0x84E1;
    public static final int GL_MAX_TEXTURE_UNITS_ARB = 0x84E2;

    private ARBMultitexture() {
    }

    public static void glActiveTextureARB(int unit) {
        GL13.glActiveTexture(unit);
    }

    public static void glClientActiveTextureARB(int unit) {
        GL13.glClientActiveTexture(unit);
    }

    public static void glMultiTexCoord1fARB(int unit, float s) {
        GL13.glMultiTexCoord1f(unit, s);
    }

    public static void glMultiTexCoord2fARB(int unit, float s, float t) {
        GL13.glMultiTexCoord2f(unit, s, t);
    }

    public static void glMultiTexCoord3fARB(int unit, float s, float t, float r) {
        GL13.glMultiTexCoord3f(unit, s, t, r);
    }
}
