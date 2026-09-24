package dev.flagon.platform.input;

public interface InputSource {

    KeyEvent nextKeyEvent();

    boolean isKeyDown(int scancode);

    void setRepeatEvents(boolean enabled);

    boolean isRepeatEventsEnabled();

    MouseEvent nextMouseEvent();

    boolean isButtonDown(int button);

    int mouseX();
    int mouseY();

    int takeDeltaX();
    int takeDeltaY();
    int takeWheel();

    boolean isPointerInside();
    boolean isGrabbed();
    boolean setGrabbed(boolean grabbed);

    void reset();
}
