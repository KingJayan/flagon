package dev.flagon.platform.input;

/**
 * one mouse button, movement, or wheel notch.
 *
 * <p>{@link #y()} counts up from the bottom of the surface, which is gl's convention and the
 * one desktop code already expects; hosts that report from the top convert on the way in.
 */
public final class MouseEvent {

    /** the button of an event that only moved the pointer or turned the wheel. */
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

    /** @return 0 left, 1 right, 2 middle, or {@link #NO_BUTTON} */
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

    /** @return movement since the previous event, positive upwards */
    public int deltaY() {
        return deltaY;
    }

    /** @return wheel movement in notches of 120, positive away from the user */
    public int wheel() {
        return wheel;
    }

    public long nanos() {
        return nanos;
    }
}
