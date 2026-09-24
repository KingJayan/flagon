package dev.flagon.platform.input;

public final class MouseEvent {

    public static final int NO_BUTTON = -1;

    private final int button;
    private final boolean pressed;
    private final int x;
    private final int y;
    private final int deltaX;
    private final int deltaY;
    private final int wheel;
    private final long nanos;

    public MouseEvent(int button, boolean pressed, int x, int y,
                      int deltaX, int deltaY, int wheel, long nanos) {
        this.button = button;
        this.pressed = pressed;
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.wheel = wheel;
        this.nanos = nanos;
    }

    public int button() {
        return button;
    }

    public boolean isPressed() {
        return pressed;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int deltaX() {
        return deltaX;
    }

    public int deltaY() {
        return deltaY;
    }

    public int wheel() {
        return wheel;
    }

    public long nanos() {
        return nanos;
    }
}
