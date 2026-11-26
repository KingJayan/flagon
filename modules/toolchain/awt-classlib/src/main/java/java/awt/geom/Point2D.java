package java.awt.geom;

public abstract class Point2D implements Cloneable {

    public static class Float extends Point2D {

        public float x;
        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        @Override
        public String toString() {
            return "Point2D.Float[" + x + ", " + y + "]";
        }
    }

    public static class Double extends Point2D {

        public double x;
        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point2D.Double[" + x + ", " + y + "]";
        }
    }

    protected Point2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }

    public static double distanceSq(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(distanceSq(x1, y1, x2, y2));
    }

    public double distanceSq(double x, double y) {
        return distanceSq(getX(), getY(), x, y);
    }

    public double distanceSq(Point2D point) {
        return distanceSq(point.getX(), point.getY());
    }

    public double distance(double x, double y) {
        return Math.sqrt(distanceSq(x, y));
    }

    public double distance(Point2D point) {
        return Math.sqrt(distanceSq(point));
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException impossible) {
            throw new InternalError(impossible);
        }
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Point2D point && getX() == point.getX()
                && getY() == point.getY();
    }
}
