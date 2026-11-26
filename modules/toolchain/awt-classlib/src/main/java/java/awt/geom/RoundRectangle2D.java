package java.awt.geom;

public abstract class RoundRectangle2D extends RectangularShape {

    /** control point offset, as a share of the corner, for a cubic quarter ellipse */
    private static final double CONTROL;

    static {
        double angle = Math.PI / 4.0;
        double a = 1.0 - Math.cos(angle);
        double b = Math.tan(angle);
        double c = Math.sqrt(1.0 + b * b) - 1 + a;
        CONTROL = (1.0 - 4.0 / 3.0 * a * b / c) / 2.0;
    }

    public static class Float extends RoundRectangle2D {

        public float x;
        public float y;
        public float width;
        public float height;
        public float arcwidth;
        public float archeight;

        public Float() {
        }

        public Float(float x, float y, float width, float height, float arcWidth,
                     float arcHeight) {
            setRoundRect(x, y, width, height, arcWidth, arcHeight);
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0f || height <= 0.0f;
        }

        public void setRoundRect(float x, float y, float width, float height, float arcWidth,
                                 float arcHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.arcwidth = arcWidth;
            this.archeight = arcHeight;
        }

        @Override
        public void setRoundRect(double x, double y, double width, double height,
                                 double arcWidth, double arcHeight) {
            setRoundRect((float) x, (float) y, (float) width, (float) height,
                    (float) arcWidth, (float) arcHeight);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(x, y, width, height);
        }
    }

    public static class Double extends RoundRectangle2D {

        public double x;
        public double y;
        public double width;
        public double height;
        public double arcwidth;
        public double archeight;

        public Double() {
        }

        public Double(double x, double y, double width, double height, double arcWidth,
                      double arcHeight) {
            setRoundRect(x, y, width, height, arcWidth, arcHeight);
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
        public double getArcWidth() {
            return arcwidth;
        }

        @Override
        public double getArcHeight() {
            return archeight;
        }

        @Override
        public boolean isEmpty() {
            return width <= 0.0 || height <= 0.0;
        }

        @Override
        public void setRoundRect(double x, double y, double width, double height,
                                 double arcWidth, double arcHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.arcwidth = arcWidth;
            this.archeight = arcHeight;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(x, y, width, height);
        }
    }

    protected RoundRectangle2D() {
    }

    public abstract double getArcWidth();

    public abstract double getArcHeight();

    public abstract void setRoundRect(double x, double y, double width, double height,
                                      double arcWidth, double arcHeight);

    public void setRoundRect(RoundRectangle2D other) {
        setRoundRect(other.getX(), other.getY(), other.getWidth(), other.getHeight(),
                other.getArcWidth(), other.getArcHeight());
    }

    @Override
    public void setFrame(double x, double y, double width, double height) {
        setRoundRect(x, y, width, height, getArcWidth(), getArcHeight());
    }

    @Override
    public boolean contains(double px, double py) {
        if (isEmpty()) {
            return false;
        }
        double left = getX();
        double top = getY();
        double right = left + getWidth();
        double bottom = top + getHeight();
        if (px < left || py < top || px >= right || py >= bottom) {
            return false;
        }
        double aw = Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
        double ah = Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
        double cx;
        if (px >= left + aw) {
            if (px < right - aw) {
                return true;
            }
            cx = right - aw;
        } else {
            cx = left + aw;
        }
        double cy;
        if (py >= top + ah) {
            if (py < bottom - ah) {
                return true;
            }
            cy = bottom - ah;
        } else {
            cy = top + ah;
        }
        double nx = (px - cx) / aw;
        double ny = (py - cy) / ah;
        return nx * nx + ny * ny <= 1.0;
    }

    @Override
    public boolean intersects(double px, double py, double rw, double rh) {
        if (isEmpty() || rw <= 0 || rh <= 0) {
            return false;
        }
        double left = getX();
        double top = getY();
        double right = left + getWidth();
        double bottom = top + getHeight();
        if (px + rw <= left || px >= right || py + rh <= top || py >= bottom) {
            return false;
        }
        double aw = Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
        double ah = Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
        int x0 = band(px, left, right, aw);
        int x1 = band(px + rw, left, right, aw);
        int y0 = band(py, top, bottom, ah);
        int y1 = band(py + rh, top, bottom, ah);
        if (x0 == 2 || x1 == 2 || y0 == 2 || y1 == 2) {
            return true;
        }
        if (x0 < 2 && x1 > 2 || y0 < 2 && y1 > 2) {
            return true;
        }
        double nx = (x1 == 1 ? px + rw - (left + aw) : px - (right - aw)) / aw;
        double ny = (y1 == 1 ? py + rh - (top + ah) : py - (bottom - ah)) / ah;
        return nx * nx + ny * ny <= 1.0;
    }

    /** 0 before, 1 left arc, 2 flat, 3 right arc, 4 after */
    private static int band(double at, double low, double high, double arc) {
        if (at < low) {
            return 0;
        } else if (at < low + arc) {
            return 1;
        } else if (at < high - arc) {
            return 2;
        } else if (at < high) {
            return 3;
        }
        return 4;
    }

    @Override
    public boolean contains(double px, double py, double rw, double rh) {
        if (isEmpty() || rw <= 0 || rh <= 0) {
            return false;
        }
        return contains(px, py) && contains(px + rw, py) && contains(px, py + rh)
                && contains(px + rw, py + rh);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform transform) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        if (w < 0 || h < 0) {
            return new Segments(new int[0], new double[0], 0, PathIterator.WIND_NON_ZERO,
                    transform);
        }
        double aw = Math.min(w, Math.abs(getArcWidth()));
        double ah = Math.min(h, Math.abs(getArcHeight()));
        double k = CONTROL;
        // each point is x + a * w + b * aw, y + c * h + d * ah
        double[][] points = {
            {0.0, 0.0, 0.0, 0.5},
            {0.0, 0.0, 1.0, -0.5},
            {0.0, 0.0, 1.0, -k, 0.0, k, 1.0, 0.0, 0.0, 0.5, 1.0, 0.0},
            {1.0, -0.5, 1.0, 0.0},
            {1.0, -k, 1.0, 0.0, 1.0, 0.0, 1.0, -k, 1.0, 0.0, 1.0, -0.5},
            {1.0, 0.0, 0.0, 0.5},
            {1.0, 0.0, 0.0, k, 1.0, -k, 0.0, 0.0, 1.0, -0.5, 0.0, 0.0},
            {0.0, 0.5, 0.0, 0.0},
            {0.0, k, 0.0, 0.0, 0.0, 0.0, 0.0, k, 0.0, 0.0, 0.0, 0.5},
        };
        int line = PathIterator.SEG_LINETO;
        int cubic = PathIterator.SEG_CUBICTO;
        int[] types = {PathIterator.SEG_MOVETO, line, cubic, line, cubic, line, cubic, line,
            cubic, PathIterator.SEG_CLOSE};
        double[] coords = new double[36];
        int at = 0;
        for (double[] point : points) {
            for (int i = 0; i < point.length; i += 4) {
                coords[at++] = x + point[i] * w + point[i + 1] * aw;
                coords[at++] = y + point[i + 2] * h + point[i + 3] * ah;
            }
        }
        return new Segments(types, coords, types.length, PathIterator.WIND_NON_ZERO, transform);
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits += java.lang.Double.doubleToLongBits(getY()) * 37;
        bits += java.lang.Double.doubleToLongBits(getWidth()) * 43;
        bits += java.lang.Double.doubleToLongBits(getHeight()) * 47;
        bits += java.lang.Double.doubleToLongBits(getArcWidth()) * 53;
        bits += java.lang.Double.doubleToLongBits(getArcHeight()) * 59;
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof RoundRectangle2D round
                && getX() == round.getX() && getY() == round.getY()
                && getWidth() == round.getWidth() && getHeight() == round.getHeight()
                && getArcWidth() == round.getArcWidth() && getArcHeight() == round.getArcHeight();
    }
}
