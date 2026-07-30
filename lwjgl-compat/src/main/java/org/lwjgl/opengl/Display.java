package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.Platform;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.display.DisplaySurface;
import org.lwjgl.LWJGLException;

public final class Display {

    private static final DisplayMode DEFAULT_MODE = new DisplayMode(854, 480);

    private static DisplaySurface surface;
    private static DisplayMode mode = DEFAULT_MODE;
    private static String title = "Flagon";
    private static long lastSync;

    private Display() {
    }

    public static void create() throws LWJGLException {
        if (surface != null) {
            return;
        }
        Platform platform = Platforms.get();
        if (!platform.hasDisplay()) {
            throw new LWJGLException("this host cannot open a display");
        }
        try {
            surface = platform.display(mode.getWidth(), mode.getHeight(), title);
        } catch (UnsupportedCapabilityException failure) {
            throw new LWJGLException(failure.getMessage(), failure);
        }
        surface.graphics().viewport(0, 0, surface.width(), surface.height());
        GLState.attach(surface.graphics());
    }

    public static void create(PixelFormat format) throws LWJGLException {
        create();
    }

    public static boolean isCreated() {
        return surface != null;
    }

    public static void destroy() {
        if (surface != null) {
            GLState.detach();
            surface.destroy();
            surface = null;
        }
    }

    /** presents the frame and returns when the next one is due. */
    public static void update() {
        required().present();
    }

    public static void update(boolean processMessages) {
        update();
    }

    /**
     * desktop lwjgl sleeps here to hit a frame rate. the browser already paces frames through
     * {@link #update()}, so this only records the request rather than sleeping and losing a frame.
     */
    public static void sync(int fps) {
        lastSync = fps;
    }

    public static int getSyncRate() {
        return (int) lastSync;
    }

    public static boolean isCloseRequested() {
        return surface != null && surface.isCloseRequested();
    }

    public static boolean isActive() {
        return surface != null;
    }

    public static boolean isVisible() {
        return surface != null;
    }

    public static void setTitle(String newTitle) {
        title = newTitle;
        if (surface != null) {
            surface.setTitle(newTitle);
        }
    }

    public static String getTitle() {
        return title;
    }

    public static void setDisplayMode(DisplayMode newMode) throws LWJGLException {
        if (newMode == null) {
            throw new LWJGLException("display mode must not be null");
        }
        mode = newMode;
        if (surface != null) {
            surface.setSize(newMode.getWidth(), newMode.getHeight());
            surface.graphics().viewport(0, 0, surface.width(), surface.height());
        }
    }

    public static DisplayMode getDisplayMode() {
        return mode;
    }

    public static DisplayMode getDesktopDisplayMode() {
        return DEFAULT_MODE;
    }

    public static DisplayMode[] getAvailableDisplayModes() {
        return new DisplayMode[] {mode};
    }

    public static void setFullscreen(boolean fullscreen) throws LWJGLException {
        if (surface != null && !surface.setFullscreen(fullscreen)) {
            throw new LWJGLException("the browser refused the fullscreen request");
        }
    }

    public static boolean isFullscreen() {
        return surface != null && surface.isFullscreen();
    }

    public static int getWidth() {
        return surface == null ? mode.getWidth() : surface.width();
    }

    public static int getHeight() {
        return surface == null ? mode.getHeight() : surface.height();
    }

    public static boolean wasResized() {
        return false;
    }

    public static void setVSyncEnabled(boolean enabled) {
        // requestAnimationFrame is always vsynced; there is nothing to switch off
    }

    public static void setResizable(boolean resizable) {
        // the page owns the canvas size
    }

    public static void processMessages() {
        // events are delivered by the browser between frames
    }

    private static DisplaySurface required() {
        if (surface == null) {
            throw new IllegalStateException("Display.create() has not been called");
        }
        return surface;
    }
}
