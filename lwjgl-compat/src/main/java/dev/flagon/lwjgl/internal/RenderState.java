package dev.flagon.lwjgl.internal;

/**
 everything a draw call needs to know about the gl context that is not the geometry itself
 */
public interface RenderState {

    float[] modelView();

    float[] projection();

    boolean isTextureEnabled();

    boolean isAlphaTestEnabled();

    float alphaTestThreshold();

    float red();

    float green();

    float blue();

    float alpha();

    float s();

    float t();

    float pointSize();
}
