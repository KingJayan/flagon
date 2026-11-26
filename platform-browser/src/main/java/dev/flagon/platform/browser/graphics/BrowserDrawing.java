package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.browser.js.JsArrays;
import dev.flagon.platform.graphics.Canvas2D;
import dev.flagon.platform.graphics.Drawing;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.IOException;

/** {@link Drawing} over OffscreenCanvas, and the image decoder web/src/images.ts installs */
public final class BrowserDrawing implements Drawing {

    @JSFunctor
    interface Decoded extends JSObject {
        void call(String error, int width, int height, Int8Array rgba);
    }

    @Override
    public boolean isAvailable() {
        return hasOffscreenCanvas();
    }

    @Override
    public Canvas2D canvas(int width, int height) {
        if (!hasOffscreenCanvas()) {
            throw new UnsupportedCapabilityException(
                    "this browser has no OffscreenCanvas for java.awt to draw on");
        }
        return new BrowserCanvas2D(width, height);
    }

    @Override
    public Drawing.Decoded decode(byte[] file) throws IOException {
        if (!hasDecoder()) {
            throw new IOException("no self.__flagonImages on this page; the flagon bundle loader"
                    + " installs it");
        }
        return decode(JsArrays.bytes(file, file.length));
    }

    @Async
    private static native Drawing.Decoded decode(Int8Array file) throws IOException;

    private static void decode(Int8Array file, AsyncCallback<Drawing.Decoded> callback) {
        start(file, (error, width, height, rgba) -> {
            if (error == null) {
                callback.complete(new Drawing.Decoded(width, height,
                        BrowserCanvas2D.argbOf(JsArrays.bytesOf(rgba))));
            } else {
                callback.error(new IOException("the browser cannot decode this image: " + error));
            }
        });
    }

    @JSBody(script = "return typeof OffscreenCanvas !== 'undefined';")
    private static native boolean hasOffscreenCanvas();

    @JSBody(script = "return typeof self.__flagonImages !== 'undefined';")
    private static native boolean hasDecoder();

    @JSBody(params = {"file", "done"}, script = "self.__flagonImages.decode(file, done);")
    private static native void start(Int8Array file, Decoded done);
}
