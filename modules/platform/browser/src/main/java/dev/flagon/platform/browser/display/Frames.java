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

    private static int drawn;

    @Async
    public static native void awaitNextFrame();

    private static void awaitNextFrame(AsyncCallback<Void> callback) {
        requestAnimationFrame(timestamp -> {
            countFrame(++drawn);
            callback.complete(null);
        });
    }

    @JSBody(params = "handler", script = "requestAnimationFrame(handler);")
    private static native void requestAnimationFrame(FrameHandler handler);

    // the browser tests read this count
    @JSBody(params = "count", script = "self.__flagonFrames = count;")
    private static native void countFrame(int count);
}
