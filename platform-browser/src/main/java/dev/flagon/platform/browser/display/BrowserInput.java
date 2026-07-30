package dev.flagon.platform.browser.display;

import dev.flagon.platform.Clock;
import dev.flagon.platform.input.InputSource;
import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.MouseEvent;
import dev.flagon.platform.input.Scancode;
import org.teavm.jso.JSObject;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * {@link InputSource} over dom events
 */
final class BrowserInput implements InputSource {

    private static final int MAX_QUEUED = 256;

    /**
     * wheel notch is 120 in gl apis vs chrome's modern notches of 100px
     */
    private static final int NOTCH = 120;
    private static final double PIXELS_PER_NOTCH = 100.0;
    private static final double PIXELS_PER_LINE = 40.0;
    private static final double PIXELS_PER_PAGE = 400.0;

    private final JSObject canvas;
    private final Clock clock;
    private final Deque<KeyEvent> keyEvents = new ArrayDeque<>();
    private final Deque<MouseEvent> mouseEvents = new ArrayDeque<>();
    private final boolean[] keysDown = new boolean[Scancode.COUNT];
    private final boolean[] buttonsDown = new boolean[8];

    private JSObject listeners;
    private boolean repeatEvents;
    private boolean inside;
    private int x;
    private int y;
    private int deltaX;
    private int deltaY;
    private int wheel;
    private double wheelPixels;

    BrowserInput(JSObject canvas, Clock clock) {
        this.canvas = canvas;
        this.clock = clock;
    }

    void attach() {
        if (listeners != null) {
            return;
        }
        listeners = DomInput.attach(canvas, this::onKey, this::onButton, this::onMove,
                this::onWheel, this::onPresence);
    }

    void detach() {
        if (listeners != null) {
            DomInput.detach(listeners);
            listeners = null;
        }
        reset();
    }

    // dom callbacks

    /** @return whether the key was recognised, which is what decides if the default is suppressed */
    private boolean onKey(String code, String key, boolean pressed, boolean repeat) {
        int scancode = KeyCodes.scancode(code);
        if (scancode == Scancode.NONE) {
            return false;
        }
        if (repeat && !repeatEvents) {
            // still counts as handled: the key is held, the page must not act on it
            return true;
        }
        keysDown[scancode] = pressed;
        push(keyEvents, new KeyEvent(scancode, KeyCodes.character(key), pressed, repeat,
                clock.nanoTime()));
        return true;
    }

    private void onButton(int button, boolean pressed, int canvasX, int canvasY) {
        int mapped = mapButton(button);
        if (mapped < 0 || mapped >= buttonsDown.length) {
            return;
        }
        buttonsDown[mapped] = pressed;
        if (!isGrabbed()) {
            x = canvasX;
            y = flipY(canvasY);
        }
        push(mouseEvents, new MouseEvent(mapped, pressed, x, y, 0, 0, 0, clock.nanoTime()));
    }

    private void onMove(int canvasX, int canvasY, int movementX, int movementY, boolean locked) {
        int movedX;
        int movedY;
        if (locked) {
            // the last one is kept
            movedX = movementX;
            movedY = -movementY;
        } else {
            int newX = canvasX;
            int newY = flipY(canvasY);
            movedX = newX - x;
            movedY = newY - y;
            x = newX;
            y = newY;
        }
        if (movedX == 0 && movedY == 0) {
            return;
        }
        deltaX += movedX;
        deltaY += movedY;
        push(mouseEvents, new MouseEvent(MouseEvent.NO_BUTTON, false, x, y,
                movedX, movedY, 0, clock.nanoTime()));
    }

    private void onWheel(double delta, int deltaMode) {
        double pixels = switch (deltaMode) {
            case 1 -> delta * PIXELS_PER_LINE;
            case 2 -> delta * PIXELS_PER_PAGE;
            default -> delta;
        };

        wheelPixels -= pixels;

        int notches = (int) (wheelPixels / PIXELS_PER_NOTCH);
        if (notches == 0) {
            return;
        }
        wheelPixels -= notches * PIXELS_PER_NOTCH;
        int turned = notches * NOTCH;
        wheel += turned;
        push(mouseEvents, new MouseEvent(MouseEvent.NO_BUTTON, false, x, y, 0, 0, turned,
                clock.nanoTime()));
    }

    private void onPresence(boolean pointerInside) {
        inside = pointerInside;
    }

    /** dom mouse button order vs gl-era apis order */
    private static int mapButton(int domButton) {
        return switch (domButton) {
            case 0 -> 0;
            case 1 -> 2;
            case 2 -> 1;
            default -> domButton;
        };
    }

    private int flipY(int canvasY) {
        return Math.max(0, Canvas.height(canvas) - 1 - canvasY);
    }

    private static <T> void push(Deque<T> queue, T event) {
        if (queue.size() >= MAX_QUEUED) {
            queue.pollFirst();
        }
        queue.addLast(event);
    }

    // InputSource

    @Override
    public KeyEvent nextKeyEvent() {
        return keyEvents.pollFirst();
    }

    @Override
    public boolean isKeyDown(int scancode) {
        return scancode > 0 && scancode < keysDown.length && keysDown[scancode];
    }

    @Override
    public void setRepeatEvents(boolean enabled) {
        repeatEvents = enabled;
    }

    @Override
    public boolean isRepeatEventsEnabled() {
        return repeatEvents;
    }

    @Override
    public MouseEvent nextMouseEvent() {
        return mouseEvents.pollFirst();
    }

    @Override
    public boolean isButtonDown(int button) {
        return button >= 0 && button < buttonsDown.length && buttonsDown[button];
    }

    @Override
    public int mouseX() {
        return x;
    }

    @Override
    public int mouseY() {
        return y;
    }

    @Override
    public int takeDeltaX() {
        int taken = deltaX;
        deltaX = 0;
        return taken;
    }

    @Override
    public int takeDeltaY() {
        int taken = deltaY;
        deltaY = 0;
        return taken;
    }

    @Override
    public int takeWheel() {
        int taken = wheel;
        wheel = 0;
        return taken;
    }

    @Override
    public boolean isPointerInside() {
        return inside || isGrabbed();
    }

    @Override
    public boolean isGrabbed() {
        return DomInput.isPointerLocked(canvas);
    }

    @Override
    public boolean setGrabbed(boolean grabbed) {
        if (grabbed == isGrabbed()) {
            return true;
        }
        if (!grabbed) {
            DomInput.exitPointerLock();
            return true;
        }
        return DomInput.requestPointerLock(canvas);
    }

    @Override
    public void reset() {
        keyEvents.clear();
        mouseEvents.clear();
        java.util.Arrays.fill(keysDown, false);
        java.util.Arrays.fill(buttonsDown, false);
        deltaX = 0;
        deltaY = 0;
        wheel = 0;
        wheelPixels = 0.0;
    }
}
