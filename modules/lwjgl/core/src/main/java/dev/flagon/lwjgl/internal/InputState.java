package dev.flagon.lwjgl.internal;

import dev.flagon.platform.input.InputSource;

public final class InputState {

    private static InputSource current;

    private InputState() {
    }

    public static void attach(InputSource source) {
        current = source;
    }

    public static void detach() {
        current = null;
    }

    public static boolean isAttached() {
        return current != null;
    }

    public static InputSource get() {
        if (current == null) {
            throw new IllegalStateException(
                    "no input device; Display.create() must be called first");
        }
        return current;
    }
}
