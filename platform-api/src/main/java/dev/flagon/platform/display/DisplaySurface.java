package dev.flagon.platform.display;

import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.input.InputSource;

/**
 * a window with a rendering context.
 *
 * <p>{@link #present()} is the load-bearing method. desktop code drives itself with
 * {@code while (!closeRequested) { draw(); Display.update(); }} and expects update to block until
 * the frame is displayed. a host that cannot block must make present suspend the calling thread
 * and resume it on the next frame, so the loop above runs unmodified.
 */
public interface DisplaySurface {

    int width();

    int height();

    void setTitle(String title);

    /** resizes the drawable area; hosts that cannot honour the request keep their own size. */
    void setSize(int width, int height);

    boolean isFullscreen();

    /**
     * @return whether the request was granted. hosts may require a user gesture and refuse,
     *         which callers must tolerate rather than assume success.
     */
    boolean setFullscreen(boolean fullscreen);

    boolean isCloseRequested();

    void requestClose();

    GraphicsDevice graphics();

    InputSource input();

    /** displays the current frame and returns when the next one should be drawn. */
    void present();

    void destroy();
}
