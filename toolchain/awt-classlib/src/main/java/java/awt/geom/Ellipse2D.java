package java.awt.geom;

public abstract class Ellipse2D extends RectangularShape {

    /** how far a cubic's control points sit along the tangent to follow a quarter circle */
    private static final double KAPPA = 0.5522847498307933;
    private static final double NEAR = 0.5 - KAPPA * 0.5;
    private static final double FAR = 0.5 + KAPPA * 0.5;

    public static class Float extends Ellipse2D {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float width, float height) {
            setFrame(x, y, width, height);
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

        public void setFrame(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void setFrame(double x, double y, double width, double height) {
            setFrame((float) x, (float) y, (float) width, (float) height);
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(x, y, width, height);
        }
    }

    public static class Double extends Ellipse2D {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double width, double height) {
            setFrame(x, y, width, height);
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
        public void setFrame(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(x, y, width, height);
        }
    }

    protected Ellipse2D() {
    }

    @Override
    public boolean contains(double px, double py) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0.0 || height <= 0.0) {
            return false;
        }
        double nx = (px - getX()) / width - 0.5;
        double ny = (py - getY()) / height - 0.5;
        return nx * nx + ny * ny < 0.25;
    }

    @Override
    public boolean intersects(double px, double py, double rw, double rh) {
        double width = getWidth();
        double height = getHeight();
        if (rw <= 0.0 || rh <= 0.0 || width <= 0.0 || height <= 0.0) {
            return false;
        }
        double left = (px - getX()) / width - 0.5;
        double right = left + rw / width;
        double top = (py - getY()) / height - 0.5;
        double bottom = top + rh / height;
        double nx = left > 0.0 ? left : right < 0.0 ? right : 0.0;
        double ny = top > 0.0 ? top : bottom < 0.0 ? bottom : 0.0;
        return nx * nx + ny * ny < 0.25;
    }

    @Override
    public boolean contains(double px, double py, double rw, double rh) {
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
        int cubic = PathIterator.SEG_CUBICTO;
        int[] types = {PathIterator.SEG_MOVETO, cubic, cubic, cubic, cubic,
            PathIterator.SEG_CLOSE};
        double[] unit = {
            1.0, 0.5,
            1.0, FAR, FAR, 1.0, 0.5, 1.0,
            NEAR, 1.0, 0.0, FAR, 0.0, 0.5,
            0.0, NEAR, NEAR, 0.0, 0.5, 0.0,
            FAR, 0.0, 1.0, NEAR, 1.0, 0.5,
        };
        double[] coords = new double[unit.length];
        for (int i = 0; i < unit.length; i += 2) {
            coords[i] = x + unit[i] * w;
            coords[i + 1] = y + unit[i + 1] * h;
        }
        return new Segments(types, coords, types.length, PathIterator.WIND_NON_ZERO, transform);
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
        return other == this || other instanceof Ellipse2D ellipse && getX() == ellipse.getX()
                && getY() == ellipse.getY() && getWidth() == ellipse.getWidth()
                && getHeight() == ellipse.getHeight();
    }
}
