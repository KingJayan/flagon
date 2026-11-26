package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.browser.js.JsArrays;
import dev.flagon.platform.graphics.Canvas2D;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.canvas.CanvasImageSource;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.util.Locale;

/** {@link Canvas2D} over one OffscreenCanvas */
final class BrowserCanvas2D implements Canvas2D {

    interface Offscreen extends CanvasImageSource {

        @JSMethod
        Context getContext(String kind, Options options);
    }

    interface Options extends JSObject {

        @JSProperty
        void setWillReadFrequently(boolean value);
    }

    interface Context extends CanvasRenderingContext2D {

        @JSMethod("fill")
        void fillWith(String rule);

        @JSMethod("clip")
        void clipWith(String rule);

        @JSMethod("setLineDash")
        void dashes(Float64Array dash);

        @JSMethod("measureText")
        Metrics metrics(String text);

        @JSProperty
        void setImageSmoothingEnabled(boolean smooth);
    }

    interface Metrics extends JSObject {

        @JSProperty
        double getWidth();

        @JSProperty
        double getFontBoundingBoxAscent();

        @JSProperty
        double getFontBoundingBoxDescent();
    }

    private final int width;
    private final int height;
    private final Offscreen canvas;
    private final Context context;
    private Uint32Array swizzled;

    BrowserCanvas2D(int width, int height) {
        this.width = width;
        this.height = height;
        this.canvas = create(width, height);
        Options options = emptyOptions();
        options.setWillReadFrequently(true);
        this.context = canvas.getContext("2d", options);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void put(int[] argb, int offset, int stride, int x, int y, int width, int height) {
        int count = width * height;
        if (count == 0) {
            return;
        }
        int[] packed = argb;
        if (offset != 0 || stride != width || argb.length != count) {
            packed = new int[count];
            for (int row = 0; row < height; row++) {
                System.arraycopy(argb, offset + row * stride, packed, row * width, width);
            }
        }
        if (swizzled == null || swizzled.getLength() < count) {
            swizzled = new Uint32Array(count);
        }
        Uint8Array rgba = JsArrays.rgba(packed, count, swizzled);
        context.putImageData(new ImageData(
                new Uint8ClampedArray(rgba.getBuffer(), rgba.getByteOffset(), count * 4),
                width, height), x, y);
    }

    @Override
    public void get(int[] argb, int offset, int stride, int x, int y, int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        Uint8ClampedArray data = context.getImageData(x, y, width, height).getData();
        int[] read = argbOf(JsArrays.bytesOf(
                new Int8Array(data.getBuffer(), data.getByteOffset(), data.getLength())));
        for (int row = 0; row < height; row++) {
            System.arraycopy(read, row * width, argb, offset + row * stride, width);
        }
    }

    @Override
    public void save() {
        context.save();
    }

    @Override
    public void restore() {
        context.restore();
    }

    @Override
    public void setTransform(double m00, double m10, double m01, double m11, double m02,
                             double m12) {
        context.setTransform(m00, m10, m01, m11, m02, m12);
    }

    @Override
    public void beginPath() {
        context.beginPath();
    }

    @Override
    public void moveTo(double x, double y) {
        context.moveTo(x, y);
    }

    @Override
    public void lineTo(double x, double y) {
        context.lineTo(x, y);
    }

    @Override
    public void quadTo(double cx, double cy, double x, double y) {
        context.quadraticCurveTo(cx, cy, x, y);
    }

    @Override
    public void curveTo(double cx1, double cy1, double cx2, double cy2, double x, double y) {
        context.bezierCurveTo(cx1, cy1, cx2, cy2, x, y);
    }

    @Override
    public void ellipse(double cx, double cy, double rx, double ry, double start, double end,
                        boolean counterclockwise) {
        context.ellipse(cx, cy, rx, ry, 0, start, end, counterclockwise ? 1 : 0);
    }

    @Override
    public void closePath() {
        context.closePath();
    }

    @Override
    public void clip(boolean evenOdd) {
        context.clipWith(evenOdd ? "evenodd" : "nonzero");
    }

    @Override
    public void fill(boolean evenOdd) {
        context.fillWith(evenOdd ? "evenodd" : "nonzero");
    }

    @Override
    public void stroke() {
        context.stroke();
    }

    @Override
    public void clearRect(double x, double y, double width, double height) {
        context.clearRect(x, y, width, height);
    }

    @Override
    public void color(int argb) {
        String style = String.format(Locale.ROOT, "rgba(%d,%d,%d,%s)", (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF, argb & 0xFF, Double.toString((argb >>> 24) / 255.0));
        context.setFillStyle(style);
        context.setStrokeStyle(style);
    }

    @Override
    public void line(double width, String cap, String join, double miterLimit, double[] dash,
                     double dashPhase) {
        context.setLineWidth(width);
        context.setLineCap(cap);
        context.setLineJoin(join);
        context.setMiterLimit(miterLimit);
        Float64Array dashes = new Float64Array(dash.length);
        for (int at = 0; at < dash.length; at++) {
            dashes.set(at, dash[at]);
        }
        context.dashes(dashes);
        context.setLineDashOffset(dashPhase);
    }

    @Override
    public void alpha(double alpha) {
        context.setGlobalAlpha(alpha);
    }

    @Override
    public void composite(String operation) {
        context.setGlobalCompositeOperation(operation);
    }

    @Override
    public void smoothing(boolean smooth) {
        context.setImageSmoothingEnabled(smooth);
    }

    @Override
    public void font(String font) {
        context.setFont(font);
    }

    @Override
    public void fillText(String text, double x, double y) {
        context.fillText(text, x, y);
    }

    @Override
    public double[] measure(String text) {
        Metrics metrics = context.metrics(text);
        return new double[] {metrics.getWidth(), metrics.getFontBoundingBoxAscent(),
            metrics.getFontBoundingBoxDescent()};
    }

    @Override
    public void drawImage(Canvas2D source, double sx, double sy, double sw, double sh,
                          double dx, double dy, double dw, double dh) {
        context.drawImage(((BrowserCanvas2D) source).canvas, sx, sy, sw, sh, dx, dy, dw, dh);
    }

    static int[] argbOf(byte[] rgba) {
        int[] argb = new int[rgba.length / 4];
        for (int at = 0; at < argb.length; at++) {
            int base = at * 4;
            argb[at] = (rgba[base + 3] & 0xFF) << 24 | (rgba[base] & 0xFF) << 16
                    | (rgba[base + 1] & 0xFF) << 8 | rgba[base + 2] & 0xFF;
        }
        return argb;
    }

    @JSBody(params = {"width", "height"}, script = "return new OffscreenCanvas(width, height);")
    private static native Offscreen create(int width, int height);

    @JSBody(script = "return {};")
    private static native Options emptyOptions();
}
