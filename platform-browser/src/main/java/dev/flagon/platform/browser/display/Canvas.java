package dev.flagon.platform.browser.display;

import dev.flagon.platform.browser.graphics.WebGL2;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/** dom plumbing for the drawing canvas, kept apart from the surface's own logic. */
final class Canvas {

    private Canvas() {
    }

    /** returns the page's canvas, creating and appending one if the page supplied none */
    @JSBody(params = {"id", "width", "height"}, script = """
            var canvas = document.getElementById(id);
            if (!canvas) {
                canvas = document.createElement('canvas');
                canvas.id = id;
                canvas.dataset.flagonCreated = 'true';
                document.body.appendChild(canvas);
            }
            canvas.width = width;
            canvas.height = height;
            canvas.dataset.flagonOpen = 'true';
            return canvas;
            """)
    static native JSObject findOrCreate(String id, int width, int height);

    /**
     * webgl2 context, or null when the browser or gpu refuses one
     */
    @JSBody(params = "canvas", script = """
            return canvas.getContext('webgl2', {
                alpha: false,
                depth: true,
                stencil: true,
                antialias: false,
                preserveDrawingBuffer: true,
                powerPreference: 'high-performance'
            });
            """)
    static native WebGL2 context(JSObject canvas);

    @JSBody(params = {"canvas", "width", "height"}, script = """
            canvas.width = width;
            canvas.height = height;
            """)
    static native void resize(JSObject canvas, int width, int height);

    @JSBody(params = "canvas", script = "return canvas.width;")
    static native int width(JSObject canvas);

    @JSBody(params = "canvas", script = "return canvas.height;")
    static native int height(JSObject canvas);

    @JSBody(params = "title", script = "document.title = title;")
    static native void setTitle(String title);

    @JSBody(params = "canvas", script = """
            return document.hasFocus() && document.activeElement === canvas;
            """)
    static native boolean isFocused(JSObject canvas);

    @JSBody(script = "return document.visibilityState !== 'hidden';")
    static native boolean isVisible();

    @JSBody(script = "return document.fullscreenElement != null;")
    static native boolean isFullscreen();

    @JSBody(params = "canvas", script = """
            try {
                var result = canvas.requestFullscreen();
                if (result && typeof result.catch === 'function') {
                    result.catch(function (error) {
                        console.warn('[flagon] fullscreen refused: ' + error);
                    });
                }
                return true;
            } catch (error) {
                console.warn('[flagon] fullscreen refused: ' + error);
                return false;
            }
            """)
    static native boolean requestFullscreen(JSObject canvas);

    @JSBody(script = """
            if (document.fullscreenElement != null) {
                document.exitFullscreen();
            }
            return true;
            """)
    static native boolean exitFullscreen();

    @JSBody(params = "canvas", script = """
            if (canvas.dataset.flagonCreated === 'true') {
                canvas.remove();
            }
            """)
    static native void remove(JSObject canvas);

    @JSBody(script = "return window.__flagonCloseRequested === true;")
    static native boolean isCloseRequested();

    @JSBody(script = "window.__flagonCloseRequested = true;")
    static native void requestClose();
}
