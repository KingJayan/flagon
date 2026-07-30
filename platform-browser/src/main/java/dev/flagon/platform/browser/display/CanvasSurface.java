package dev.flagon.platform.browser.display;

import dev.flagon.platform.Clock;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.browser.graphics.WebGL2;
import dev.flagon.platform.browser.graphics.WebGLGraphicsDevice;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.input.InputSource;
import org.teavm.jso.JSObject;

public final class CanvasSurface implements DisplaySurface {

    public static final String CANVAS_ID = "flagon-canvas";

    private final JSObject canvas;
    private final WebGLGraphicsDevice graphics;
    private final BrowserInput input;
    private boolean destroyed;

    private CanvasSurface(JSObject canvas, WebGLGraphicsDevice graphics, BrowserInput input) {
        this.canvas = canvas;
        this.graphics = graphics;
        this.input = input;
    }

    public static CanvasSurface open(int width, int height, String title, Clock clock) {
        JSObject canvas = Canvas.findOrCreate(CANVAS_ID, width, height);
        WebGL2 gl = Canvas.context(canvas);
        if (gl == null) {
            throw new UnsupportedCapabilityException(
                    "this browser did not provide a WebGL2 context; the gpu may be blocklisted "
                            + "or hardware acceleration disabled");
        }
        Canvas.setTitle(title);
        BrowserInput input = new BrowserInput(canvas, clock);
        input.attach();
        return new CanvasSurface(canvas, new WebGLGraphicsDevice(gl), input);
    }

    @Override
    public int width() {
        return Canvas.width(canvas);
    }

    @Override
    public int height() {
        return Canvas.height(canvas);
    }

    @Override
    public void setTitle(String title) {
        Canvas.setTitle(title);
    }

    @Override
    public void setSize(int width, int height) {
        Canvas.resize(canvas, width, height);
    }

    @Override
    public boolean isFullscreen() {
        return Canvas.isFullscreen();
    }

    @Override
    public boolean setFullscreen(boolean fullscreen) {
        return fullscreen ? Canvas.requestFullscreen(canvas) : Canvas.exitFullscreen();
    }

    @Override
    public boolean isCloseRequested() {
        return destroyed || Canvas.isCloseRequested();
    }

    @Override
    public void requestClose() {
        Canvas.requestClose();
    }

    @Override
    public GraphicsDevice graphics() {
        return graphics;
    }

    @Override
    public InputSource input() {
        return input;
    }

    @Override
    public void present() {
        // webgl presents implicitly when the frame callback returns, so there is nothing to
        // swap; what desktop code is really waiting for here is the next frame
        Frames.awaitNextFrame();
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            destroyed = true;
            input.detach();
            Canvas.remove(canvas);
        }
    }
}
