package dev.flagon.platform.browser.display;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * makes a blocking frame loop work in an environment that forbids blocking
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

    @JSBody(params = "handler", script = """
            requestAnimationFrame(function (timestamp) {
                window.__flagonFrames = (window.__flagonFrames || 0) + 1;
                handler(timestamp);
            });
            """)
    private static native void requestAnimationFrame(FrameHandler handler);
}
