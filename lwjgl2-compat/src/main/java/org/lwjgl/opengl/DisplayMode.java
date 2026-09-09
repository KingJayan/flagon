package org.lwjgl.opengl;

public final class DisplayMode {

    private final int width;
    private final int height;
    private final int bitsPerPixel;
    private final int frequency;
    private final boolean fullscreenCapable;

    public DisplayMode(int width, int height) {
        this(width, height, 32, 60, false);
    }

    public DisplayMode(int width, int height, int bitsPerPixel, int frequency) {
        this(width, height, bitsPerPixel, frequency, true);
    }

    DisplayMode(int width, int height, int bitsPerPixel, int frequency, boolean fullscreenCapable) {
        this.width = width;
        this.height = height;
        this.bitsPerPixel = bitsPerPixel;
        this.frequency = frequency;
        this.fullscreenCapable = fullscreenCapable;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getFrequency() {
        return frequency;
    }

    public boolean isFullscreenCapable() {
        return fullscreenCapable;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DisplayMode mode)) {
            return false;
        }
        return width == mode.width && height == mode.height
                && bitsPerPixel == mode.bitsPerPixel && frequency == mode.frequency;
    }

    @Override
    public int hashCode() {
        return width * 31 + height;
    }

    @Override
    public String toString() {
        return width + " x " + height + " x " + bitsPerPixel + " @" + frequency + "Hz";
    }
}
