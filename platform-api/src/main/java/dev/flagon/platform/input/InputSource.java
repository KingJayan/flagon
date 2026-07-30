package dev.flagon.platform.input;

/**
 * keyboard and mouse for one surface.
 *
 * <p>events are queued and drained by the caller rather than delivered to a listener, because
 * that is the shape desktop code already has: a game loop polls between frames. hosts that
 * receive input asynchronously buffer it until the loop asks.
 *
 * <p>the queue is bounded. an application that stops draining is misbehaving, and dropping the
 * oldest events keeps that from growing without limit.
 */
public interface InputSource {

    /** @return the next queued key event, or {@code null} when the queue is empty */
    KeyEvent nextKeyEvent();

    boolean isKeyDown(int scancode);

    /**
     * @param enabled whether a held key should keep producing events
     */
    void setRepeatEvents(boolean enabled);

    boolean isRepeatEventsEnabled();

    /** @return the next queued mouse event, or {@code null} when the queue is empty */
    MouseEvent nextMouseEvent();

    boolean isButtonDown(int button);

    int mouseX();

    /** @return the pointer's position measured up from the bottom of the surface */
    int mouseY();

    /** @return movement since this was last called, which it resets */
    int takeDeltaX();

    int takeDeltaY();

    /** @return wheel movement since this was last called, which it resets */
    int takeWheel();

    boolean isPointerInside();

    boolean isGrabbed();

    /**
     * hides and locks the pointer so only movement is reported, which is what mouselook needs.
     *
     * @return whether the request was granted. hosts may require a user gesture and refuse,
     *         so callers must read {@link #isGrabbed()} rather than assume it took effect.
     */
    boolean setGrabbed(boolean grabbed);

    /** drops queued events and clears held state, so a re-created device starts clean. */
    void reset();
}
