package dev.flagon.platform.graphics;

/** how a texture is sampled between texels, and between its mipmap levels. */
public enum TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}
