package dev.flagon.lwjgl.internal;

/**
 everything a draw call needs to know
 */
public interface RenderState {

    float[] modelView();

    float[] projection();

    boolean isTextureEnabled(int unit);

    /** GL_TEXTURE for one unit, which its coordinates pass through */
    float[] textureMatrix(int unit);

    TextureEnvironment textureEnvironment();

    TextureGeneration textureGeneration();

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

    /** how many times one group of the state a draw reads has changed */
    int revision(int group);

    float normalX();

    float normalY();

    float normalZ();
}
