package dev.flagon.platform.browser.display;

import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.Scancode;

import java.util.HashMap;
import java.util.Map;

/**
 * translates {@code KeyboardEvent.code} into a {@link Scancode}
 */
final class KeyCodes {

    private static final Map<String, Integer> CODES = new HashMap<>();

    static {
        letter("KeyA", Scancode.A);
        letter("KeyB", Scancode.B);
        letter("KeyC", Scancode.C);
        letter("KeyD", Scancode.D);
        letter("KeyE", Scancode.E);
        letter("KeyF", Scancode.F);
        letter("KeyG", Scancode.G);
        letter("KeyH", Scancode.H);
        letter("KeyI", Scancode.I);
        letter("KeyJ", Scancode.J);
        letter("KeyK", Scancode.K);
        letter("KeyL", Scancode.L);
        letter("KeyM", Scancode.M);
        letter("KeyN", Scancode.N);
        letter("KeyO", Scancode.O);
        letter("KeyP", Scancode.P);
        letter("KeyQ", Scancode.Q);
        letter("KeyR", Scancode.R);
        letter("KeyS", Scancode.S);
        letter("KeyT", Scancode.T);
        letter("KeyU", Scancode.U);
        letter("KeyV", Scancode.V);
        letter("KeyW", Scancode.W);
        letter("KeyX", Scancode.X);
        letter("KeyY", Scancode.Y);
        letter("KeyZ", Scancode.Z);

        letter("Digit1", Scancode.NUMBER_1);
        letter("Digit2", Scancode.NUMBER_2);
        letter("Digit3", Scancode.NUMBER_3);
        letter("Digit4", Scancode.NUMBER_4);
        letter("Digit5", Scancode.NUMBER_5);
        letter("Digit6", Scancode.NUMBER_6);
        letter("Digit7", Scancode.NUMBER_7);
        letter("Digit8", Scancode.NUMBER_8);
        letter("Digit9", Scancode.NUMBER_9);
        letter("Digit0", Scancode.NUMBER_0);

        letter("Minus", Scancode.MINUS);
        letter("Equal", Scancode.EQUALS);
        letter("Backspace", Scancode.BACKSPACE);
        letter("Tab", Scancode.TAB);
        letter("BracketLeft", Scancode.LEFT_BRACKET);
        letter("BracketRight", Scancode.RIGHT_BRACKET);
        letter("Enter", Scancode.RETURN);
        letter("Semicolon", Scancode.SEMICOLON);
        letter("Quote", Scancode.APOSTROPHE);
        letter("Backquote", Scancode.GRAVE);
        letter("Backslash", Scancode.BACKSLASH);
        letter("Comma", Scancode.COMMA);
        letter("Period", Scancode.PERIOD);
        letter("Slash", Scancode.SLASH);
        letter("Space", Scancode.SPACE);
        letter("Escape", Scancode.ESCAPE);
        letter("CapsLock", Scancode.CAPS_LOCK);

        letter("ShiftLeft", Scancode.LEFT_SHIFT);
        letter("ShiftRight", Scancode.RIGHT_SHIFT);
        letter("ControlLeft", Scancode.LEFT_CONTROL);
        letter("ControlRight", Scancode.RIGHT_CONTROL);
        letter("AltLeft", Scancode.LEFT_ALT);
        letter("AltRight", Scancode.RIGHT_ALT);
        letter("MetaLeft", Scancode.LEFT_META);
        letter("MetaRight", Scancode.RIGHT_META);
        letter("ContextMenu", Scancode.APPS);

        letter("F1", Scancode.F1);
        letter("F2", Scancode.F2);
        letter("F3", Scancode.F3);
        letter("F4", Scancode.F4);
        letter("F5", Scancode.F5);
        letter("F6", Scancode.F6);
        letter("F7", Scancode.F7);
        letter("F8", Scancode.F8);
        letter("F9", Scancode.F9);
        letter("F10", Scancode.F10);
        letter("F11", Scancode.F11);
        letter("F12", Scancode.F12);
        letter("F13", Scancode.F13);
        letter("F14", Scancode.F14);
        letter("F15", Scancode.F15);

        letter("ArrowUp", Scancode.UP);
        letter("ArrowDown", Scancode.DOWN);
        letter("ArrowLeft", Scancode.LEFT);
        letter("ArrowRight", Scancode.RIGHT);
        letter("Home", Scancode.HOME);
        letter("End", Scancode.END);
        letter("PageUp", Scancode.PAGE_UP);
        letter("PageDown", Scancode.PAGE_DOWN);
        letter("Insert", Scancode.INSERT);
        letter("Delete", Scancode.DELETE);
        letter("Pause", Scancode.PAUSE);
        letter("PrintScreen", Scancode.PRINT_SCREEN);
        letter("ScrollLock", Scancode.SCROLL_LOCK);
        letter("NumLock", Scancode.NUM_LOCK);

        letter("Numpad0", Scancode.NUMPAD_0);
        letter("Numpad1", Scancode.NUMPAD_1);
        letter("Numpad2", Scancode.NUMPAD_2);
        letter("Numpad3", Scancode.NUMPAD_3);
        letter("Numpad4", Scancode.NUMPAD_4);
        letter("Numpad5", Scancode.NUMPAD_5);
        letter("Numpad6", Scancode.NUMPAD_6);
        letter("Numpad7", Scancode.NUMPAD_7);
        letter("Numpad8", Scancode.NUMPAD_8);
        letter("Numpad9", Scancode.NUMPAD_9);
        letter("NumpadAdd", Scancode.NUMPAD_ADD);
        letter("NumpadSubtract", Scancode.NUMPAD_SUBTRACT);
        letter("NumpadMultiply", Scancode.NUMPAD_MULTIPLY);
        letter("NumpadDivide", Scancode.NUMPAD_DIVIDE);
        letter("NumpadDecimal", Scancode.NUMPAD_DECIMAL);
        letter("NumpadEnter", Scancode.NUMPAD_ENTER);
        letter("NumpadEqual", Scancode.NUMPAD_EQUALS);
        letter("NumpadComma", Scancode.NUMPAD_COMMA);
    }

    private KeyCodes() {
    }

    private static void letter(String code, int scancode) {
        CODES.put(code, scancode);
    }

    /** @return the scancode, or {@link Scancode#NONE} for a key this host does not map */
    static int scancode(String code) {
        if (code == null) {
            return Scancode.NONE;
        }
        Integer scancode = CODES.get(code);
        return scancode == null ? Scancode.NONE : scancode;
    }

    static char character(String key) {
        if (key == null || key.isEmpty()) {
            return KeyEvent.NO_CHARACTER;
        }
        if (key.length() == 1) {
            return key.charAt(0);
        }
        return switch (key) {
            case "Enter" -> '\r';
            case "Tab" -> '\t';
            case "Backspace" -> '\b';
            case "Escape" -> (char) 27;
            default -> KeyEvent.NO_CHARACTER;
        };
    }
}
