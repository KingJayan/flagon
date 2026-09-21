package dev.flagon.lwjgl.internal;

/**
 everything a draw call needs to know
 */
public interface RenderState {

    float[] modelView();

    float[] projection();

    boolean isTextureEnabled(int unit);

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

    Lighting lighting();

    /** how many times the state a draw reads has changed */
    int revision();

    float normalX();

    float normalY();

    float normalZ();
}
