package dev.flagon.platform.display;

import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.input.InputSource;

/**
 * a window with a rendering context
 */
public interface DisplaySurface {

    int width();

    int height();

    void setTitle(String title);

    /** resizes the drawable area; hosts that cannot honour the request keep their own size. */
    void setSize(int width, int height);

    boolean isFullscreen();


    boolean setFullscreen(boolean fullscreen);

    boolean isFocused();

    boolean isVisible();

    boolean isCloseRequested();

    void requestClose();

    GraphicsDevice graphics();

    InputSource input();

    /** displays the current frame and returns when the next one should be drawn. */
    void present();

    void destroy();
}
