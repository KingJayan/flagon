package org.lwjgl.glfw;

/** what a monitor can display */
public final class GLFWVidMode {

    private final int width;
    private final int height;
    private final int refreshRate;

    public GLFWVidMode(int width, int height, int refreshRate) {
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int refreshRate() {
        return refreshRate;
    }

    public int redBits() {
        return 8;
    }

    public int greenBits() {
        return 8;
    }

    public int blueBits() {
        return 8;
    }
}
