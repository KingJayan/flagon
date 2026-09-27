package dev.flagon.platform.browser.display;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

final class Promises {

    @JSFunctor
    interface Refused extends JSObject {
        void call(JSObject error);
    }

    private Promises() {
    }

    static void warnIfRefused(JSObject result, String about) {
        if (isPresent(result) && hasCatch(result)) {
            onRefused(result, error -> warn("[flagon] " + about + " refused: " + textOf(error)));
        }
    }

    @JSBody(params = "text", script = "console.warn(text);")
    static native void warn(String text);

    @JSBody(params = "value", script = "return value != null;")
    private static native boolean isPresent(JSObject value);

    @JSBody(params = "value", script = "return typeof value.catch === 'function';")
    private static native boolean hasCatch(JSObject value);

    @JSBody(params = {"value", "refused"}, script = "value.catch(refused);")
    private static native void onRefused(JSObject value, Refused refused);

    @JSBody(params = "value", script = "return String(value);")
    private static native String textOf(JSObject value);
}
