package java.awt.geom;

import java.awt.Shape;

/** insideness for any path, from its edges once its curves are cut into lines */
final class Crossings {

    static final double FLATNESS = 0.01;

    private Crossings() {
    }

    /**
     * @return how many times the path winds around the point, counting edges that cross a ray
     *         from it towards positive x
     */
    static int around(PathIterator flat, double x, double y) {
        double[] point = new double[6];
        int crossings = 0;
        double startX = 0;
        double startY = 0;
        double lastX = 0;
        double lastY = 0;
        while (!flat.isDone()) {
            int type = flat.currentSegment(point);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_CLOSE) {
                crossings += edge(x, y, lastX, lastY, startX, startY);
                if (type == PathIterator.SEG_MOVETO) {
                    startX = point[0];
                    startY = point[1];
                }
                lastX = startX;
                lastY = startY;
            } else {
                crossings += edge(x, y, lastX, lastY, point[0], point[1]);
                lastX = point[0];
                lastY = point[1];
            }
            flat.next();
        }
        return crossings + edge(x, y, lastX, lastY, startX, startY);
    }

    private static int edge(double px, double py, double x0, double y0, double x1, double y1) {
        if (py < y0 && py < y1 || py >= y0 && py >= y1 || px >= x0 && px >= x1) {
            return 0;
        }
        int direction = y0 < y1 ? 1 : -1;
        if (px < x0 && px < x1) {
            return direction;
        }
        return px < x0 + (py - y0) * (x1 - x0) / (y1 - y0) ? direction : 0;
    }

    static boolean intersects(Shape shape, double x, double y, double width,
                                     double height) {
        if (!(width > 0 && height > 0)) {
            return false;
        }
        if (shape.contains(x + width / 2, y + height / 2) || shape.contains(x, y)
                || shape.contains(x + width, y) || shape.contains(x, y + height)
                || shape.contains(x + width, y + height)) {
            return true;
        }
        return crossesInside(shape, x, y, width, height);
    }

    static boolean contains(Shape shape, double x, double y, double width,
                                   double height) {
        if (!(width > 0 && height > 0)) {
            return false;
        }
        return shape.contains(x + width / 2, y + height / 2)
                && !crossesInside(shape, x, y, width, height);
    }

    /** whether an edge reaches the open inside of the rectangle */
    private static boolean crossesInside(Shape shape, double x, double y, double width,
                                         double height) {
        double inset = 1e-9 * Math.max(1, Math.max(width, height));
        Rectangle2D inside = new Rectangle2D.Double(x + inset, y + inset, width - 2 * inset,
                height - 2 * inset);
        PathIterator flat = shape.getPathIterator(null, FLATNESS);
        double[] point = new double[6];
        double startX = 0;
        double startY = 0;
        double lastX = 0;
        double lastY = 0;
        boolean started = false;
        while (!flat.isDone()) {
            int type = flat.currentSegment(point);
            double toX = type == PathIterator.SEG_LINETO ? point[0] : startX;
            double toY = type == PathIterator.SEG_LINETO ? point[1] : startY;
            if (type == PathIterator.SEG_MOVETO) {
                if (started && inside.intersectsLine(lastX, lastY, startX, startY)) {
                    return true;
                }
                started = true;
                startX = point[0];
                startY = point[1];
                toX = startX;
                toY = startY;
            } else if (inside.intersectsLine(lastX, lastY, toX, toY)) {
                return true;
            }
            lastX = toX;
            lastY = toY;
            flat.next();
        }
        return started && inside.intersectsLine(lastX, lastY, startX, startY);
    }
}
