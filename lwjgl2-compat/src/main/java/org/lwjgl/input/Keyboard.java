package org.lwjgl.input;

import dev.flagon.lwjgl.internal.InputState;
import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.Scancode;
import org.lwjgl.LWJGLException;

/**
 * lwjgl's keyboard over the host's input source.
 *
 * <p>the constants are directinput scancodes, so they name physical keys rather than
 * characters — {@link #KEY_Z} is the key where z sits on a us layout whatever it prints
 * elsewhere. text comes from {@link #getEventCharacter()} instead, which is why lwjgl reports
 * both for the same event.
 */
public final class Keyboard {

    public static final int KEYBOARD_SIZE = Scancode.COUNT;
    public static final char CHAR_NONE = KeyEvent.NO_CHARACTER;

    public static final int KEY_NONE = Scancode.NONE;
    public static final int KEY_ESCAPE = Scancode.ESCAPE;
    public static final int KEY_1 = Scancode.NUMBER_1;
    public static final int KEY_2 = Scancode.NUMBER_2;
    public static final int KEY_3 = Scancode.NUMBER_3;
    public static final int KEY_4 = Scancode.NUMBER_4;
    public static final int KEY_5 = Scancode.NUMBER_5;
    public static final int KEY_6 = Scancode.NUMBER_6;
    public static final int KEY_7 = Scancode.NUMBER_7;
    public static final int KEY_8 = Scancode.NUMBER_8;
    public static final int KEY_9 = Scancode.NUMBER_9;
    public static final int KEY_0 = Scancode.NUMBER_0;
    public static final int KEY_MINUS = Scancode.MINUS;
    public static final int KEY_EQUALS = Scancode.EQUALS;
    public static final int KEY_BACK = Scancode.BACKSPACE;
    public static final int KEY_TAB = Scancode.TAB;

    public static final int KEY_Q = Scancode.Q;
    public static final int KEY_W = Scancode.W;
    public static final int KEY_E = Scancode.E;
    public static final int KEY_R = Scancode.R;
    public static final int KEY_T = Scancode.T;
    public static final int KEY_Y = Scancode.Y;
    public static final int KEY_U = Scancode.U;
    public static final int KEY_I = Scancode.I;
    public static final int KEY_O = Scancode.O;
    public static final int KEY_P = Scancode.P;
    public static final int KEY_LBRACKET = Scancode.LEFT_BRACKET;
    public static final int KEY_RBRACKET = Scancode.RIGHT_BRACKET;
    public static final int KEY_RETURN = Scancode.RETURN;
    public static final int KEY_LCONTROL = Scancode.LEFT_CONTROL;

    public static final int KEY_A = Scancode.A;
    public static final int KEY_S = Scancode.S;
    public static final int KEY_D = Scancode.D;
    public static final int KEY_F = Scancode.F;
    public static final int KEY_G = Scancode.G;
    public static final int KEY_H = Scancode.H;
    public static final int KEY_J = Scancode.J;
    public static final int KEY_K = Scancode.K;
    public static final int KEY_L = Scancode.L;
    public static final int KEY_SEMICOLON = Scancode.SEMICOLON;
    public static final int KEY_APOSTROPHE = Scancode.APOSTROPHE;
    public static final int KEY_GRAVE = Scancode.GRAVE;
    public static final int KEY_LSHIFT = Scancode.LEFT_SHIFT;
    public static final int KEY_BACKSLASH = Scancode.BACKSLASH;

    public static final int KEY_Z = Scancode.Z;
    public static final int KEY_X = Scancode.X;
    public static final int KEY_C = Scancode.C;
    public static final int KEY_V = Scancode.V;
    public static final int KEY_B = Scancode.B;
    public static final int KEY_N = Scancode.N;
    public static final int KEY_M = Scancode.M;
    public static final int KEY_COMMA = Scancode.COMMA;
    public static final int KEY_PERIOD = Scancode.PERIOD;
    public static final int KEY_SLASH = Scancode.SLASH;
    public static final int KEY_RSHIFT = Scancode.RIGHT_SHIFT;
    public static final int KEY_MULTIPLY = Scancode.NUMPAD_MULTIPLY;
    public static final int KEY_LMENU = Scancode.LEFT_ALT;
    public static final int KEY_SPACE = Scancode.SPACE;
    public static final int KEY_CAPITAL = Scancode.CAPS_LOCK;

    public static final int KEY_F1 = Scancode.F1;
    public static final int KEY_F2 = Scancode.F2;
    public static final int KEY_F3 = Scancode.F3;
    public static final int KEY_F4 = Scancode.F4;
    public static final int KEY_F5 = Scancode.F5;
    public static final int KEY_F6 = Scancode.F6;
    public static final int KEY_F7 = Scancode.F7;
    public static final int KEY_F8 = Scancode.F8;
    public static final int KEY_F9 = Scancode.F9;
    public static final int KEY_F10 = Scancode.F10;
    public static final int KEY_F11 = Scancode.F11;
    public static final int KEY_F12 = Scancode.F12;
    public static final int KEY_F13 = Scancode.F13;
    public static final int KEY_F14 = Scancode.F14;
    public static final int KEY_F15 = Scancode.F15;

    public static final int KEY_NUMLOCK = Scancode.NUM_LOCK;
    public static final int KEY_SCROLL = Scancode.SCROLL_LOCK;
    public static final int KEY_NUMPAD0 = Scancode.NUMPAD_0;
    public static final int KEY_NUMPAD1 = Scancode.NUMPAD_1;
    public static final int KEY_NUMPAD2 = Scancode.NUMPAD_2;
    public static final int KEY_NUMPAD3 = Scancode.NUMPAD_3;
    public static final int KEY_NUMPAD4 = Scancode.NUMPAD_4;
    public static final int KEY_NUMPAD5 = Scancode.NUMPAD_5;
    public static final int KEY_NUMPAD6 = Scancode.NUMPAD_6;
    public static final int KEY_NUMPAD7 = Scancode.NUMPAD_7;
    public static final int KEY_NUMPAD8 = Scancode.NUMPAD_8;
    public static final int KEY_NUMPAD9 = Scancode.NUMPAD_9;
    public static final int KEY_SUBTRACT = Scancode.NUMPAD_SUBTRACT;
    public static final int KEY_ADD = Scancode.NUMPAD_ADD;
    public static final int KEY_DECIMAL = Scancode.NUMPAD_DECIMAL;
    public static final int KEY_DIVIDE = Scancode.NUMPAD_DIVIDE;
    public static final int KEY_NUMPADENTER = Scancode.NUMPAD_ENTER;
    public static final int KEY_NUMPADEQUALS = Scancode.NUMPAD_EQUALS;
    public static final int KEY_NUMPADCOMMA = Scancode.NUMPAD_COMMA;

    public static final int KEY_RCONTROL = Scancode.RIGHT_CONTROL;
    public static final int KEY_RMENU = Scancode.RIGHT_ALT;
    public static final int KEY_SYSRQ = Scancode.PRINT_SCREEN;
    public static final int KEY_PAUSE = Scancode.PAUSE;
    public static final int KEY_HOME = Scancode.HOME;
    public static final int KEY_UP = Scancode.UP;
    public static final int KEY_PRIOR = Scancode.PAGE_UP;
    public static final int KEY_LEFT = Scancode.LEFT;
    public static final int KEY_RIGHT = Scancode.RIGHT;
    public static final int KEY_END = Scancode.END;
    public static final int KEY_DOWN = Scancode.DOWN;
    public static final int KEY_NEXT = Scancode.PAGE_DOWN;
    public static final int KEY_INSERT = Scancode.INSERT;
    public static final int KEY_DELETE = Scancode.DELETE;
    public static final int KEY_LMETA = Scancode.LEFT_META;
    public static final int KEY_RMETA = Scancode.RIGHT_META;
    public static final int KEY_APPS = Scancode.APPS;

    private static final String[] NAMES = new String[KEYBOARD_SIZE];

    static {
        name(KEY_ESCAPE, "ESCAPE");
        name(KEY_1, "1");
        name(KEY_2, "2");
        name(KEY_3, "3");
        name(KEY_4, "4");
        name(KEY_5, "5");
        name(KEY_6, "6");
        name(KEY_7, "7");
        name(KEY_8, "8");
        name(KEY_9, "9");
        name(KEY_0, "0");
        name(KEY_MINUS, "MINUS");
        name(KEY_EQUALS, "EQUALS");
        name(KEY_BACK, "BACK");
        name(KEY_TAB, "TAB");
        name(KEY_Q, "Q");
        name(KEY_W, "W");
        name(KEY_E, "E");
        name(KEY_R, "R");
        name(KEY_T, "T");
        name(KEY_Y, "Y");
        name(KEY_U, "U");
        name(KEY_I, "I");
        name(KEY_O, "O");
        name(KEY_P, "P");
        name(KEY_LBRACKET, "LBRACKET");
        name(KEY_RBRACKET, "RBRACKET");
        name(KEY_RETURN, "RETURN");
        name(KEY_LCONTROL, "LCONTROL");
        name(KEY_A, "A");
        name(KEY_S, "S");
        name(KEY_D, "D");
        name(KEY_F, "F");
        name(KEY_G, "G");
        name(KEY_H, "H");
        name(KEY_J, "J");
        name(KEY_K, "K");
        name(KEY_L, "L");
        name(KEY_SEMICOLON, "SEMICOLON");
        name(KEY_APOSTROPHE, "APOSTROPHE");
        name(KEY_GRAVE, "GRAVE");
        name(KEY_LSHIFT, "LSHIFT");
        name(KEY_BACKSLASH, "BACKSLASH");
        name(KEY_Z, "Z");
        name(KEY_X, "X");
        name(KEY_C, "C");
        name(KEY_V, "V");
        name(KEY_B, "B");
        name(KEY_N, "N");
        name(KEY_M, "M");
        name(KEY_COMMA, "COMMA");
        name(KEY_PERIOD, "PERIOD");
        name(KEY_SLASH, "SLASH");
        name(KEY_RSHIFT, "RSHIFT");
        name(KEY_MULTIPLY, "MULTIPLY");
        name(KEY_LMENU, "LMENU");
        name(KEY_SPACE, "SPACE");
        name(KEY_CAPITAL, "CAPITAL");
        name(KEY_F1, "F1");
        name(KEY_F2, "F2");
        name(KEY_F3, "F3");
        name(KEY_F4, "F4");
        name(KEY_F5, "F5");
        name(KEY_F6, "F6");
        name(KEY_F7, "F7");
        name(KEY_F8, "F8");
        name(KEY_F9, "F9");
        name(KEY_F10, "F10");
        name(KEY_F11, "F11");
        name(KEY_F12, "F12");
        name(KEY_F13, "F13");
        name(KEY_F14, "F14");
        name(KEY_F15, "F15");
        name(KEY_NUMLOCK, "NUMLOCK");
        name(KEY_SCROLL, "SCROLL");
        name(KEY_NUMPAD0, "NUMPAD0");
        name(KEY_NUMPAD1, "NUMPAD1");
        name(KEY_NUMPAD2, "NUMPAD2");
        name(KEY_NUMPAD3, "NUMPAD3");
        name(KEY_NUMPAD4, "NUMPAD4");
        name(KEY_NUMPAD5, "NUMPAD5");
        name(KEY_NUMPAD6, "NUMPAD6");
        name(KEY_NUMPAD7, "NUMPAD7");
        name(KEY_NUMPAD8, "NUMPAD8");
        name(KEY_NUMPAD9, "NUMPAD9");
        name(KEY_SUBTRACT, "SUBTRACT");
        name(KEY_ADD, "ADD");
        name(KEY_DECIMAL, "DECIMAL");
        name(KEY_DIVIDE, "DIVIDE");
        name(KEY_NUMPADENTER, "NUMPADENTER");
        name(KEY_NUMPADEQUALS, "NUMPADEQUALS");
        name(KEY_NUMPADCOMMA, "NUMPADCOMMA");
        name(KEY_RCONTROL, "RCONTROL");
        name(KEY_RMENU, "RMENU");
        name(KEY_SYSRQ, "SYSRQ");
        name(KEY_PAUSE, "PAUSE");
        name(KEY_HOME, "HOME");
        name(KEY_UP, "UP");
        name(KEY_PRIOR, "PRIOR");
        name(KEY_LEFT, "LEFT");
        name(KEY_RIGHT, "RIGHT");
        name(KEY_END, "END");
        name(KEY_DOWN, "DOWN");
        name(KEY_NEXT, "NEXT");
        name(KEY_INSERT, "INSERT");
        name(KEY_DELETE, "DELETE");
        name(KEY_LMETA, "LMETA");
        name(KEY_RMETA, "RMETA");
        name(KEY_APPS, "APPS");
    }

    private static KeyEvent event;

    private Keyboard() {
    }

    private static void name(int key, String name) {
        NAMES[key] = name;
    }

    public static void create() throws LWJGLException {
        if (!InputState.isAttached()) {
            throw new LWJGLException("Display.create() must be called before Keyboard.create()");
        }
    }

    public static boolean isCreated() {
        return InputState.isAttached();
    }

    public static void destroy() {
        event = null;
    }

    /** the host delivers events as they happen, so there is nothing to pull in. */
    public static void poll() {
    }

    /** @return whether an event was dequeued, which the getters below then describe */
    public static boolean next() {
        event = InputState.get().nextKeyEvent();
        return event != null;
    }

    public static int getEventKey() {
        return event == null ? KEY_NONE : event.key();
    }

    public static char getEventCharacter() {
        return event == null ? CHAR_NONE : event.character();
    }

    public static boolean getEventKeyState() {
        return event != null && event.isPressed();
    }

    public static boolean isRepeatEvent() {
        return event != null && event.isRepeat();
    }

    public static long getEventNanoseconds() {
        return event == null ? 0L : event.nanos();
    }

    public static boolean isKeyDown(int key) {
        return InputState.get().isKeyDown(key);
    }

    public static void enableRepeatEvents(boolean enable) {
        InputState.get().setRepeatEvents(enable);
    }

    public static boolean areRepeatEventsEnabled() {
        return InputState.get().isRepeatEventsEnabled();
    }

    public static String getKeyName(int key) {
        if (key < 0 || key >= NAMES.length || NAMES[key] == null) {
            return "NONE";
        }
        return NAMES[key];
    }

    public static int getKeyIndex(String name) {
        for (int key = 0; key < NAMES.length; key++) {
            if (name.equals(NAMES[key])) {
                return key;
            }
        }
        return KEY_NONE;
    }

    public static int getKeyCount() {
        return KEYBOARD_SIZE;
    }
}
