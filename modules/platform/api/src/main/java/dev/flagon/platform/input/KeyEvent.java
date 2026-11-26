package dev.flagon.platform.input;

/** one key going down or coming up. */
public final class KeyEvent {

    /** what a key that produces no text reports. */
    public static final char NO_CHARACTER = '\0';

    private final int key;
    private final char character;
    private final boolean pressed;
    private final boolean repeat;
    private final long nanos;

    public KeyEvent(int key, char character, boolean pressed, boolean repeat, long nanos) {
        this.key = key;
        this.character = character;
        this.pressed = pressed;
        this.repeat = repeat;
        this.nanos = nanos;
    }

    /** @return a {@link Scancode}, identifying the physical key */
    public int key() {
        return key;
    }

    /** @return the text the key produced, or {@link #NO_CHARACTER} for keys that produce none */
    public char character() {
        return character;
    }

    public boolean isPressed() {
        return pressed;
    }

    /** @return whether the host generated this from a key being held rather than struck */
    public boolean isRepeat() {
        return repeat;
    }

    public long nanos() {
        return nanos;
    }
}
