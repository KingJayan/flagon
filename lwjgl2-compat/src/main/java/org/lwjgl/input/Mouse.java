package org.lwjgl.input;

import dev.flagon.lwjgl.internal.InputState;
import dev.flagon.platform.input.MouseEvent;
import org.lwjgl.LWJGLException;

/**
 * lwjgl's mouse over the host's input source
 */
public final class Mouse {

    private static MouseEvent event;

    private Mouse() {
    }

    public static void create() throws LWJGLException {
        if (!InputState.isAttached()) {
            throw new LWJGLException("Display.create() must be called before Mouse.create()");
        }
    }

    public static boolean isCreated() {
        return InputState.isAttached();
    }

    public static void destroy() {
        event = null;
    }

    public static void poll() {
    }

    public static boolean next() {
        event = InputState.get().nextMouseEvent();
        return event != null;
    }

    /** @return 0 left, 1 right, 2 middle, or -1 for an event that moved or scrolled only */
    public static int getEventButton() {
        return event == null ? MouseEvent.NO_BUTTON : event.button();
    }

    public static boolean getEventButtonState() {
        return event != null && event.isPressed();
    }

    public static int getEventX() {
        return event == null ? 0 : event.x();
    }

    public static int getEventY() {
        return event == null ? 0 : event.y();
    }

    public static int getEventDX() {
        return event == null ? 0 : event.deltaX();
    }

    public static int getEventDY() {
        return event == null ? 0 : event.deltaY();
    }

    public static int getEventDWheel() {
        return event == null ? 0 : event.wheel();
    }

    public static long getEventNanoseconds() {
        return event == null ? 0L : event.nanos();
    }

    public static int getX() {
        return InputState.get().mouseX();
    }

    public static int getY() {
        return InputState.get().mouseY();
    }

    public static int getDX() {
        return InputState.get().takeDeltaX();
    }

    public static int getDY() {
        return InputState.get().takeDeltaY();
    }

    public static int getDWheel() {
        return InputState.get().takeWheel();
    }

    public static boolean isButtonDown(int button) {
        return InputState.get().isButtonDown(button);
    }

    public static String getButtonName(int button) {
        return switch (button) {
            case 0 -> "BUTTON0";
            case 1 -> "BUTTON1";
            case 2 -> "BUTTON2";
            default -> null;
        };
    }

    public static int getButtonIndex(String name) {
        return switch (name) {
            case "BUTTON0" -> 0;
            case "BUTTON1" -> 1;
            case "BUTTON2" -> 2;
            default -> -1;
        };
    }

    public static int getButtonCount() {
        return 3;
    }

    public static boolean hasWheel() {
        return true;
    }

    public static boolean isInsideWindow() {
        return InputState.get().isPointerInside();
    }

    public static void setGrabbed(boolean grabbed) {
        InputState.get().setGrabbed(grabbed);
    }

    public static boolean isGrabbed() {
        return InputState.get().isGrabbed();
    }

    public static void setCursorPosition(int x, int y) {
    }
}
