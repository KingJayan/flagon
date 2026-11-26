package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

public abstract class Line2D implements Shape, Cloneable {

    public static class Float extends Line2D {

        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D one, Point2D two) {
            setLine(one, two);
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public Point2D getP1() {
            return new Point2D.Float(x1, y1);
        }

        @Override
        public Point2D getP2() {
            return new Point2D.Float(x2, y2);
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            setLine((float) x1, (float) y1, (float) x2, (float) y2);
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2),
                    Math.abs(x2 - x1), Math.abs(y2 - y1));
        }
    }

    public static class Double extends Line2D {

        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        public Double(Point2D one, Point2D two) {
            setLine(one, two);
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public Point2D getP1() {
            return new Point2D.Double(x1, y1);
        }

        @Override
        public Point2D getP2() {
            return new Point2D.Double(x2, y2);
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2),
                    Math.abs(x2 - x1), Math.abs(y2 - y1));
        }
    }

    protected Line2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP1();

    public abstract Point2D getP2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(Point2D one, Point2D two) {
        setLine(one.getX(), one.getY(), two.getX(), two.getY());
    }

    public void setLine(Line2D line) {
        setLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public static int relativeCCW(double x1, double y1, double x2, double y2, double px,
                                  double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double side = px * y2 - py * x2;
        if (side == 0.0) {
            side = px * x2 + py * y2;
            if (side > 0.0) {
                px -= x2;
                py -= y2;
                side = px * x2 + py * y2;
                if (side < 0.0) {
                    side = 0.0;
                }
            }
        }
        return side < 0.0 ? -1 : side > 0.0 ? 1 : 0;
    }

    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public int relativeCCW(Point2D point) {
        return relativeCCW(point.getX(), point.getY());
    }

    public static boolean linesIntersect(double x1, double y1, double x2, double y2,
                                         double x3, double y3, double x4, double y4) {
        return relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0
                && relativeCCW(x3, y3, x4, y4, x1, y1)
                        * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0;
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2, getX1(), getY1(), getX2(), getY2());
    }

    public boolean intersectsLine(Line2D line) {
        return intersectsLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public static double ptSegDistSq(double x1, double y1, double x2, double y2, double px,
                                     double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double along = px * x2 + py * y2;
        double projected;
        if (along <= 0.0) {
            projected = 0.0;
        } else {
            px = x2 - px;
            py = y2 - py;
            along = px * x2 + py * y2;
            projected = along <= 0.0 ? 0.0 : along * along / (x2 * x2 + y2 * y2);
        }
        return Math.max(0, px * px + py * py - projected);
    }

    public static double ptSegDist(double x1, double y1, double x2, double y2, double px,
                                   double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDistSq(Point2D point) {
        return ptSegDistSq(point.getX(), point.getY());
    }

    public double ptSegDist(double px, double py) {
        return Math.sqrt(ptSegDistSq(px, py));
    }

    public double ptSegDist(Point2D point) {
        return ptSegDist(point.getX(), point.getY());
    }

    public static double ptLineDistSq(double x1, double y1, double x2, double y2, double px,
                                      double py) {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double along = px * x2 + py * y2;
        double projected = along * along / (x2 * x2 + y2 * y2);
        return Math.max(0, px * px + py * py - projected);
    }

    public static double ptLineDist(double x1, double y1, double x2, double y2, double px,
                                    double py) {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDistSq(Point2D point) {
        return ptLineDistSq(point.getX(), point.getY());
    }

    public double ptLineDist(double px, double py) {
        return Math.sqrt(ptLineDistSq(px, py));
    }

    public double ptLineDist(Point2D point) {
        return ptLineDist(point.getX(), point.getY());
    }

    @Override
    public boolean contains(double x, double y) {
        return false;
    }

    @Override
    public boolean contains(Point2D point) {
        return false;
    }

    @Override
    public boolean intersects(double x, double y, double width, double height) {
        return intersects(new Rectangle2D.Double(x, y, width, height));
    }

    @Override
    public boolean intersects(Rectangle2D rectangle) {
        return rectangle.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    @Override
    public boolean contains(double x, double y, double width, double height) {
        return false;
    }

    @Override
    public boolean contains(Rectangle2D rectangle) {
        return false;
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform) {
        return new Segments(new int[] {PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO},
                new double[] {getX1(), getY1(), getX2(), getY2()}, 2,
                PathIterator.WIND_NON_ZERO, transform);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform, double flatness) {
        return getPathIterator(transform);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }
}
