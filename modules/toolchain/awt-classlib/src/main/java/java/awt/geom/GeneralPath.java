package java.awt.geom;

import java.awt.Shape;

public final class GeneralPath extends Path2D.Float {

    public GeneralPath() {
        super(WIND_NON_ZERO, 20);
    }

    public GeneralPath(int rule) {
        super(rule, 20);
    }

    public GeneralPath(int rule, int capacity) {
        super(rule, capacity);
    }

    public GeneralPath(Shape shape) {
        super(shape, null);
    }

    @Override
    public Object clone() {
        return copyInto(new GeneralPath(getWindingRule(), types.length));
    }
}
