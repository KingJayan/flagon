package java.awt;

import java.awt.geom.Dimension2D;

public class Dimension extends Dimension2D {

    public int width;
    public int height;

    public Dimension() {
    }

    public Dimension(Dimension size) {
        this(size.width, size.height);
    }

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
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
    public void setSize(double width, double height) {
        this.width = (int) Math.ceil(width);
        this.height = (int) Math.ceil(height);
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public void setSize(Dimension size) {
        setSize(size.width, size.height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Dimension size && width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        int sum = width + height;
        return sum * (sum + 1) / 2 + width;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[width=" + width + ",height=" + height + "]";
    }
}
