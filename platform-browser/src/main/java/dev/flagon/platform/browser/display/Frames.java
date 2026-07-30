package dev.flagon.platform.browser.display;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * makes a blocking frame loop work in an environment that forbids blocking.
 *
 * <p>desktop code is written as {@code while (running) { draw(); Display.update(); }} and a
 * browser cannot run that — the event loop would never regain control. teavm compiles methods
 * reachable from an {@code @Async} method into continuation-passing style, so
 * {@link #awaitNextFrame()} suspends the calling green thread, yields to the browser, and resumes
 * on the next animation frame. the loop above then runs unmodified.
 */
public final class Frames {

    @JSFunctor
    interface FrameHandler extends JSObject {
        void onFrame(double timestamp);
    }

    private Frames() {
    }

    @Async
    public static native void awaitNextFrame();

    private static void awaitNextFrame(AsyncCallback<Void> callback) {
        requestAnimationFrame(timestamp -> callback.complete(null));
    }

    @JSBody(params = "handler", script = "requestAnimationFrame(handler);")
    private static native void requestAnimationFrame(FrameHandler handler);
}
