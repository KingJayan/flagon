package org.lwjgl.opengl;

/**
 * accepted for source compatibility. the canvas context is requested once with fixed
 * attributes, so per-format requests cannot be honored after the fact.
 */
public final class PixelFormat {

    private final int depth;
    private final int stencil;
    private final int samples;

    public PixelFormat() {
        this(24, 8, 0);
    }

    public PixelFormat(int depth, int stencil, int samples) {
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;
    }

    public PixelFormat withDepthBits(int bits) {
        return new PixelFormat(bits, stencil, samples);
    }

    public PixelFormat withStencilBits(int bits) {
        return new PixelFormat(depth, bits, samples);
    }

    public PixelFormat withSamples(int count) {
        return new PixelFormat(depth, stencil, count);
    }

    public int getDepthBits() {
        return depth;
    }

    public int getStencilBits() {
        return stencil;
    }

    public int getSamples() {
        return samples;
    }
}
