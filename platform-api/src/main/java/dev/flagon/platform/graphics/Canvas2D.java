package dev.flagon.platform.graphics;

/**
 * a 2d canvas with the html canvas model: one current path, a transform, and state that
 * {@link #save} and {@link #restore} stack. colors are 0xAARRGGBB, not premultiplied.
 */
public interface Canvas2D {

    int width();

    int height();

    /** copies {@code width x height} pixels from {@code argb}, rows {@code stride} apart */
    void put(int[] argb, int offset, int stride, int x, int y, int width, int height);

    /** copies pixels out into {@code argb}, rows {@code stride} apart */
    void get(int[] argb, int offset, int stride, int x, int y, int width, int height);

    void save();

    void restore();

    void setTransform(double m00, double m10, double m01, double m11, double m02, double m12);

    void beginPath();

    void moveTo(double x, double y);

    void lineTo(double x, double y);

    void quadTo(double cx, double cy, double x, double y);

    void curveTo(double cx1, double cy1, double cx2, double cy2, double x, double y);

    /** adds an elliptical arc; angles are radians clockwise from the positive x axis */
    void ellipse(double cx, double cy, double rx, double ry, double start, double end,
                 boolean counterclockwise);

    void closePath();

    void clip(boolean evenOdd);

    void fill(boolean evenOdd);

    void stroke();

    void clearRect(double x, double y, double width, double height);

    void color(int argb);

    /**
     * @param cap butt, round or square
     * @param join miter, round or bevel
     * @param dash empty for a solid line
     */
    void line(double width, String cap, String join, double miterLimit, double[] dash,
              double dashPhase);

    void alpha(double alpha);

    /** @param operation a canvas globalCompositeOperation, such as source-over or copy */
    void composite(String operation);

    void smoothing(boolean smooth);

    /** @param font a css font, such as {@code bold 12px sans-serif} */
    void font(String font);

    void fillText(String text, double x, double y);

    /** @return width, ascent and descent of the text in the current font */
    double[] measure(String text);

    void drawImage(Canvas2D source, double sx, double sy, double sw, double sh,
                   double dx, double dy, double dw, double dh);
}
