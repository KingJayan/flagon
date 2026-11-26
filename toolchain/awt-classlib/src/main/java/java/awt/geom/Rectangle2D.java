package java.awt.geom;

public abstract class Rectangle2D extends RectangularShape {

    public static final int OUT_LEFT = 1;
    public static final int OUT_TOP = 2;
    public static final int OUT_RIGHT = 4;
    public static final int OUT_BOTTOM = 8;

    public static class Float extends Rectangle2D {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float width, float height) {
            setRect(x, y, width, height);
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void setRect(double x, double y, double width, double height) {
            setRect((float) x, (float) y, (float) width, (float) height);
        }

        @Override
        public void setRect(Rectangle2D other) {
            setRect(other.getX(), other.getY(), other.getWidth(), other.getHeight());
        }

        @Override
        public int outcode(double px, double py) {
            return Rectangle2D.outcode(px, py, x, y, width, height);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Float(x, y, width, height);
        }

        @Override
        public Rectangle2D createIntersection(Rectangle2D other) {
            Rectangle2D into = other instanceof Float ? new Float() : new Rectangle2D.Double();
            intersect(this, other, into);
            return into;
        }

        @Override
        public Rectangle2D createUnion(Rectangle2D other) {
            Rectangle2D into = other instanceof Float ? new Float() : new Rectangle2D.Double();
            union(this, other, into);
            return into;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[x=" + x + ",y=" + y + ",w=" + width + ",h="
                    + height + "]";
        }
    }

    public static class Double extends Rectangle2D {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double width, double height) {
            setRect(x, y, width, height);
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRect(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void setRect(Rectangle2D other) {
            setRect(other.getX(), other.getY(), other.getWidth(), other.getHeight());
        }

        @Override
        public int outcode(double px, double py) {
            return Rectangle2D.outcode(px, py, x, y, width, height);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Double(x, y, width, height);
        }

        @Override
        public Rectangle2D createIntersection(Rectangle2D other) {
            Rectangle2D into = new Double();
            intersect(this, other, into);
            return into;
        }

        @Override
        public Rectangle2D createUnion(Rectangle2D other) {
            Rectangle2D into = new Double();
            union(this, other, into);
            return into;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[x=" + x + ",y=" + y + ",w=" + width + ",h="
                    + height + "]";
        }
    }

    protected Rectangle2D() {
    }

    public abstract void setRect(double x, double y, double width, double height);

    public void setRect(Rectangle2D other) {
        setRect(other.getX(), other.getY(), other.getWidth(), other.getHeight());
    }

    public abstract int outcode(double x, double y);

    public int outcode(Point2D point) {
        return outcode(point.getX(), point.getY());
    }

    public abstract Rectangle2D createIntersection(Rectangle2D other);

    public abstract Rectangle2D createUnion(Rectangle2D other);

    static int outcode(double px, double py, double x, double y, double width, double height) {
        int out = 0;
        if (width <= 0) {
            out |= OUT_LEFT | OUT_RIGHT;
        } else if (px < x) {
            out |= OUT_LEFT;
        } else if (px > x + width) {
            out |= OUT_RIGHT;
        }
        if (height <= 0) {
            out |= OUT_TOP | OUT_BOTTOM;
        } else if (py < y) {
            out |= OUT_TOP;
        } else if (py > y + height) {
            out |= OUT_BOTTOM;
        }
        return out;
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        int end = outcode(x2, y2);
        if (end == 0) {
            return true;
        }
        int start;
        while ((start = outcode(x1, y1)) != 0) {
            if ((start & end) != 0) {
                return false;
            }
            if ((start & (OUT_LEFT | OUT_RIGHT)) != 0) {
                double edge = (start & OUT_RIGHT) != 0 ? getX() + getWidth() : getX();
                y1 = y1 + (edge - x1) * (y2 - y1) / (x2 - x1);
                x1 = edge;
            } else {
                double edge = (start & OUT_BOTTOM) != 0 ? getY() + getHeight() : getY();
                x1 = x1 + (edge - y1) * (x2 - x1) / (y2 - y1);
                y1 = edge;
            }
        }
        return true;
    }

    public boolean intersectsLine(Line2D line) {
        return intersectsLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    @Override
    public void setFrame(double x, double y, double width, double height) {
        setRect(x, y, width, height);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return (Rectangle2D) clone();
    }

    @Override
    public boolean contains(double px, double py) {
        double x = getX();
        double y = getY();
        return px >= x && py >= y && px < x + getWidth() && py < y + getHeight();
    }

    @Override
    public boolean intersects(double px, double py, double width, double height) {
        if (isEmpty() || width <= 0 || height <= 0) {
            return false;
        }
        double x = getX();
        double y = getY();
        return px + width > x && py + height > y && px < x + getWidth()
                && py < y + getHeight();
    }

    @Override
    public boolean contains(double px, double py, double width, double height) {
        if (isEmpty() || width <= 0 || height <= 0) {
            return false;
        }
        double x = getX();
        double y = getY();
        return px >= x && py >= y && px + width <= x + getWidth()
                && py + height <= y + getHeight();
    }

    public static void intersect(Rectangle2D one, Rectangle2D two, Rectangle2D into) {
        double x1 = Math.max(one.getMinX(), two.getMinX());
        double y1 = Math.max(one.getMinY(), two.getMinY());
        double x2 = Math.min(one.getMaxX(), two.getMaxX());
        double y2 = Math.min(one.getMaxY(), two.getMaxY());
        into.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    public static void union(Rectangle2D one, Rectangle2D two, Rectangle2D into) {
        double x1 = Math.min(one.getMinX(), two.getMinX());
        double y1 = Math.min(one.getMinY(), two.getMinY());
        double x2 = Math.max(one.getMaxX(), two.getMaxX());
        double y2 = Math.max(one.getMaxY(), two.getMaxY());
        into.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    public void add(double px, double py) {
        double x1 = Math.min(getMinX(), px);
        double x2 = Math.max(getMaxX(), px);
        double y1 = Math.min(getMinY(), py);
        double y2 = Math.max(getMaxY(), py);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point2D point) {
        add(point.getX(), point.getY());
    }

    public void add(Rectangle2D other) {
        double x1 = Math.min(getMinX(), other.getMinX());
        double x2 = Math.max(getMaxX(), other.getMaxX());
        double y1 = Math.min(getMinY(), other.getMinY());
        double y2 = Math.max(getMaxY(), other.getMaxY());
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform) {
        double x = getX();
        double y = getY();
        double width = getWidth();
        double height = getHeight();
        if (width < 0 || height < 0) {
            return new Segments(new int[0], new double[0], 0, PathIterator.WIND_NON_ZERO,
                    transform);
        }
        int line = PathIterator.SEG_LINETO;
        int[] types = {PathIterator.SEG_MOVETO, line, line, line, line, PathIterator.SEG_CLOSE};
        double[] coords = {x, y, x + width, y, x + width, y + height, x, y + height, x, y};
        return new Segments(types, coords, types.length, PathIterator.WIND_NON_ZERO, transform);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform, double flatness) {
        return getPathIterator(transform);
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits += java.lang.Double.doubleToLongBits(getY()) * 37;
        bits += java.lang.Double.doubleToLongBits(getWidth()) * 43;
        bits += java.lang.Double.doubleToLongBits(getHeight()) * 47;
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof Rectangle2D rectangle
                && getX() == rectangle.getX() && getY() == rectangle.getY()
                && getWidth() == rectangle.getWidth() && getHeight() == rectangle.getHeight();
    }
}
