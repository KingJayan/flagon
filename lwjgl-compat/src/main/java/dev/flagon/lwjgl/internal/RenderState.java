package dev.flagon.lwjgl.internal;

/**
 everything a draw call needs to know about the gl context that is not the geometry itself
 */
public interface RenderState {

    float[] modelView();

    float[] projection();

    boolean isTextureEnabled(int unit);

    /** the texture environment mode: 0 modulate, 1 replace, 2 add. */
    int textureEnv(int unit);

    boolean isAlphaTestEnabled();

    float alphaTestThreshold();

    float red();

    float green();

    float blue();

    float alpha();

    float s(int unit);

    float t(int unit);

    float pointSize();
}
