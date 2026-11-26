package dev.flagon.lwjgl;

import dev.flagon.platform.input.InputSource;
import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.MouseEvent;
import dev.flagon.platform.input.Scancode;

import java.util.ArrayDeque;
import java.util.Deque;

/** an input source a test can drive, so the lwjgl api can be checked without a browser. */
final class FakeInputSource implements InputSource {

    private final Deque<KeyEvent> keyEvents = new ArrayDeque<>();
    private final Deque<MouseEvent> mouseEvents = new ArrayDeque<>();
    private final boolean[] keysDown = new boolean[Scancode.COUNT];
    private final boolean[] buttonsDown = new boolean[8];

    boolean repeatEvents;
    boolean grabbed;
    boolean grantGrab = true;
    boolean inside;
    int x;
    int y;
    int deltaX;
    int deltaY;
    int wheel;

    void pressKey(int scancode, char character) {
        keysDown[scancode] = true;
        keyEvents.addLast(new KeyEvent(scancode, character, true, false, 1_000L));
    }

    void releaseKey(int scancode) {
        keysDown[scancode] = false;
        keyEvents.addLast(new KeyEvent(scancode, KeyEvent.NO_CHARACTER, false, false, 2_000L));
    }

    void clickButton(int button) {
        buttonsDown[button] = true;
        mouseEvents.addLast(new MouseEvent(button, true, x, y, 0, 0, 0, 3_000L));
    }

    void moveTo(int newX, int newY) {
        int movedX = newX - x;
        int movedY = newY - y;
        x = newX;
        y = newY;
        deltaX += movedX;
        deltaY += movedY;
        mouseEvents.addLast(
                new MouseEvent(MouseEvent.NO_BUTTON, false, x, y, movedX, movedY, 0, 4_000L));
    }

    void turnWheel(int notches) {
        wheel += notches;
        mouseEvents.addLast(new MouseEvent(MouseEvent.NO_BUTTON, false, x, y, 0, 0, notches,
                5_000L));
    }

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
        return inside || grabbed;
    }

    @Override
    public boolean isGrabbed() {
        return grabbed;
    }

    @Override
    public boolean setGrabbed(boolean grab) {
        if (grab && !grantGrab) {
            return false;
        }
        grabbed = grab;
        return true;
    }

    @Override
    public void reset() {
        keyEvents.clear();
        mouseEvents.clear();
        deltaX = 0;
        deltaY = 0;
        wheel = 0;
    }
}
