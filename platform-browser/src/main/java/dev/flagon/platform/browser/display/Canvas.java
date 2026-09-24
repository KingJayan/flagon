package dev.flagon.platform.browser.display;

import dev.flagon.platform.browser.graphics.WebGL2;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

final class Canvas {

    private Canvas() {
    }

    static JSObject findOrCreate(String id, int width, int height) {
        HTMLDocument page = Window.current().getDocument();
        HTMLCanvasElement canvas = (HTMLCanvasElement) page.getElementById(id);
        if (canvas == null) {
            canvas = (HTMLCanvasElement) page.createElement("canvas");
            canvas.setAttribute("id", id);
            markCreated(canvas);
            page.getBody().appendChild(canvas);
        }
        canvas.setWidth(width);
        canvas.setHeight(height);
        markOpen(canvas);
        return canvas;
    }

    @JSBody(params = "canvas", script = "canvas.dataset.flagonCreated = 'true';")
    private static native void markCreated(JSObject canvas);

    @JSBody(params = "canvas", script = "canvas.dataset.flagonOpen = 'true';")
    private static native void markOpen(JSObject canvas);

    @JSBody(params = "canvas", script = "return canvas.dataset.flagonCreated === 'true';")
    private static native boolean wasCreated(JSObject canvas);

    interface ContextOptions extends JSObject {
        @JSProperty
        void setAlpha(boolean wanted);

        @JSProperty
        void setDepth(boolean wanted);

        @JSProperty
        void setStencil(boolean wanted);

        @JSProperty
        void setAntialias(boolean wanted);

        @JSProperty
        void setPreserveDrawingBuffer(boolean wanted);

        @JSProperty
        void setPowerPreference(String wanted);
    }

    static WebGL2 context(JSObject canvas) {
        ContextOptions wanted = JSObjects.create();

        wanted.setAlpha(false);
        wanted.setDepth(true);
        wanted.setStencil(true);
        wanted.setAntialias(false);
        wanted.setPreserveDrawingBuffer(wantsPreserved());
        wanted.setPowerPreference("high-performance");

        return askForContext(canvas, wanted);
    }

    @JSBody(script = "return new URLSearchParams(location.search).has('preserve');")
    private static native boolean wantsPreserved();

    @JSBody(params = {"canvas", "wanted"}, script = "return canvas.getContext('webgl2', wanted);")
    private static native WebGL2 askForContext(JSObject canvas, ContextOptions wanted);

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

    static boolean isFocused(JSObject canvas) {
        return pageHasFocus() && isActive(canvas);
    }

    @JSBody(script = "return document.hasFocus();")
    private static native boolean pageHasFocus();

    @JSBody(params = "canvas", script = "return document.activeElement === canvas;")
    private static native boolean isActive(JSObject canvas);

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

    static void remove(JSObject canvas) {
        if (wasCreated(canvas)) {
            detach(canvas);
        }
    }

    @JSBody(params = "canvas", script = "canvas.remove();")
    private static native void detach(JSObject canvas);

    @JSBody(script = "return window.__flagonCloseRequested === true;")
    static native boolean isCloseRequested();

    @JSBody(script = "window.__flagonCloseRequested = true;")
    static native void requestClose();
}
