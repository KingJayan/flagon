package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.InputState;
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
    private static int syncRate;
    private static long frameDue;

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
        InputState.attach(surface.input());
    }

    public static void create(PixelFormat format) throws LWJGLException {
        create();
    }

    public static boolean isCreated() {
        return surface != null;
    }

    public static void destroy() {
        if (surface != null) {
            InputState.detach();
            GLState.detach();
            surface.destroy();
            surface = null;
            frameDue = 0;
        }
    }

    /** presents the frame and returns when the next one is due. */
    public static void update() {
        required().present();
    }

    public static void update(boolean processMessages) {
        update();
    }

    public static void sync(int fps) {
        syncRate = fps;
        if (fps <= 0 || surface == null) {
            frameDue = 0;
            return;
        }
        long interval = 1_000_000_000L / fps;
        long now = Platforms.get().clock().nanoTime();
        if (frameDue == 0) {
            frameDue = now + interval;
            return;
        }
        while (Platforms.get().clock().nanoTime() < frameDue) {
            surface.present();
        }
        // a loop that has fallen behind takes the rate it can manage rather than chasing frames
        frameDue = Math.max(frameDue + interval, Platforms.get().clock().nanoTime());
    }

    public static int getSyncRate() {
        return syncRate;
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
