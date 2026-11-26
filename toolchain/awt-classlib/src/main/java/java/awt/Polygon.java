package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class Polygon implements Shape {

    public int npoints;
    public int[] xpoints;
    public int[] ypoints;
    protected Rectangle bounds;

    public Polygon() {
        xpoints = new int[4];
        ypoints = new int[4];
    }

    public Polygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException("npoints > xpoints.length || "
                    + "npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = Arrays.copyOf(xpoints, npoints);
        this.ypoints = Arrays.copyOf(ypoints, npoints);
    }

    public void reset() {
        npoints = 0;
        bounds = null;
    }

    public void invalidate() {
        bounds = null;
    }

    public void translate(int dx, int dy) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += dx;
            ypoints[i] += dy;
        }
        if (bounds != null) {
            bounds.translate(dx, dy);
        }
    }

    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length || npoints >= ypoints.length) {
            int grown = Math.max(npoints * 2, 4);
            xpoints = Arrays.copyOf(xpoints, grown);
            ypoints = Arrays.copyOf(ypoints, grown);
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        if (bounds != null) {
            bounds.add(x, y);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;
            for (int i = 0; i < npoints; i++) {
                left = Math.min(left, xpoints[i]);
                right = Math.max(right, xpoints[i]);
                top = Math.min(top, ypoints[i]);
                bottom = Math.max(bottom, ypoints[i]);
            }
            bounds = new Rectangle(left, top, right - left, bottom - top);
        }
        return bounds.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    public boolean contains(Point point) {
        return contains(point.x, point.y);
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    @Override
    public boolean contains(double x, double y) {
        return npoints > 2 && getBounds().contains(x, y) && path().contains(x, y);
    }

    @Override
    public boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    @Override
    public boolean intersects(double x, double y, double width, double height) {
        return npoints > 0 && getBounds().intersects(x, y, width, height)
                && path().intersects(x, y, width, height);
    }

    @Override
    public boolean intersects(Rectangle2D rectangle) {
        return intersects(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double width, double height) {
        return npoints > 0 && getBounds().intersects(x, y, width, height)
                && path().contains(x, y, width, height);
    }

    @Override
    public boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                rectangle.getHeight());
    }

    private Path2D path() {
        return new Path2D.Double(this);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform) {
        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD, npoints + 1);
        if (npoints > 0) {
            path.moveTo(xpoints[0], ypoints[0]);
            for (int i = 1; i < npoints; i++) {
                path.lineTo(xpoints[i], ypoints[i]);
            }
            path.closePath();
        }
        return path.getPathIterator(transform);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform, double flatness) {
        return getPathIterator(transform);
    }
}
