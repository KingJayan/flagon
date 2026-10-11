package dev.flagon.lwjgl.internal;

import dev.flagon.platform.input.Scancode;

public final class GlfwKeys {

    public static final int UNKNOWN = -1;

    private static final int HIGHEST = 348;

    private static final int[] TO_SCANCODE = new int[HIGHEST + 1];
    private static final int[] TO_KEY = new int[Scancode.COUNT];

    static {
        map(32, Scancode.SPACE);
        map(39, Scancode.APOSTROPHE);
        map(44, Scancode.COMMA);
        map(45, Scancode.MINUS);
        map(46, Scancode.PERIOD);
        map(47, Scancode.SLASH);
        map(48, Scancode.NUMBER_0);
        map(49, Scancode.NUMBER_1);
        map(50, Scancode.NUMBER_2);
        map(51, Scancode.NUMBER_3);
        map(52, Scancode.NUMBER_4);
        map(53, Scancode.NUMBER_5);
        map(54, Scancode.NUMBER_6);
        map(55, Scancode.NUMBER_7);
        map(56, Scancode.NUMBER_8);
        map(57, Scancode.NUMBER_9);
        map(59, Scancode.SEMICOLON);
        map(61, Scancode.EQUALS);
        map(65, Scancode.A);
        map(66, Scancode.B);
        map(67, Scancode.C);
        map(68, Scancode.D);
        map(69, Scancode.E);
        map(70, Scancode.F);
        map(71, Scancode.G);
        map(72, Scancode.H);
        map(73, Scancode.I);
        map(74, Scancode.J);
        map(75, Scancode.K);
        map(76, Scancode.L);
        map(77, Scancode.M);
        map(78, Scancode.N);
        map(79, Scancode.O);
        map(80, Scancode.P);
        map(81, Scancode.Q);
        map(82, Scancode.R);
        map(83, Scancode.S);
        map(84, Scancode.T);
        map(85, Scancode.U);
        map(86, Scancode.V);
        map(87, Scancode.W);
        map(88, Scancode.X);
        map(89, Scancode.Y);
        map(90, Scancode.Z);
        map(91, Scancode.LEFT_BRACKET);
        map(92, Scancode.BACKSLASH);
        map(93, Scancode.RIGHT_BRACKET);
        map(96, Scancode.GRAVE);
        map(256, Scancode.ESCAPE);
        map(257, Scancode.RETURN);
        map(258, Scancode.TAB);
        map(259, Scancode.BACKSPACE);
        map(260, Scancode.INSERT);
        map(261, Scancode.DELETE);
        map(262, Scancode.RIGHT);
        map(263, Scancode.LEFT);
        map(264, Scancode.DOWN);
        map(265, Scancode.UP);
        map(266, Scancode.PAGE_UP);
        map(267, Scancode.PAGE_DOWN);
        map(268, Scancode.HOME);
        map(269, Scancode.END);
        map(280, Scancode.CAPS_LOCK);
        map(281, Scancode.SCROLL_LOCK);
        map(282, Scancode.NUM_LOCK);
        map(283, Scancode.PRINT_SCREEN);
        map(284, Scancode.PAUSE);
        map(290, Scancode.F1);
        map(291, Scancode.F2);
        map(292, Scancode.F3);
        map(293, Scancode.F4);
        map(294, Scancode.F5);
        map(295, Scancode.F6);
        map(296, Scancode.F7);
        map(297, Scancode.F8);
        map(298, Scancode.F9);
        map(299, Scancode.F10);
        map(300, Scancode.F11);
        map(301, Scancode.F12);
        map(302, Scancode.F13);
        map(303, Scancode.F14);
        map(304, Scancode.F15);
        map(320, Scancode.NUMPAD_0);
        map(321, Scancode.NUMPAD_1);
        map(322, Scancode.NUMPAD_2);
        map(323, Scancode.NUMPAD_3);
        map(324, Scancode.NUMPAD_4);
        map(325, Scancode.NUMPAD_5);
        map(326, Scancode.NUMPAD_6);
        map(327, Scancode.NUMPAD_7);
        map(328, Scancode.NUMPAD_8);
        map(329, Scancode.NUMPAD_9);
        map(330, Scancode.NUMPAD_DECIMAL);
        map(331, Scancode.NUMPAD_DIVIDE);
        map(332, Scancode.NUMPAD_MULTIPLY);
        map(333, Scancode.NUMPAD_SUBTRACT);
        map(334, Scancode.NUMPAD_ADD);
        map(335, Scancode.NUMPAD_ENTER);
        map(336, Scancode.NUMPAD_EQUALS);
        map(340, Scancode.LEFT_SHIFT);
        map(341, Scancode.LEFT_CONTROL);
        map(342, Scancode.LEFT_ALT);
        map(343, Scancode.LEFT_META);
        map(344, Scancode.RIGHT_SHIFT);
        map(345, Scancode.RIGHT_CONTROL);
        map(346, Scancode.RIGHT_ALT);
        map(347, Scancode.RIGHT_META);
        map(348, Scancode.APPS);
    }

    private GlfwKeys() {
    }

    private static void map(int key, int scancode) {
        TO_SCANCODE[key] = scancode;
        TO_KEY[scancode] = key;
    }

    public static int keyOf(int scancode) {
        if (scancode <= 0 || scancode >= TO_KEY.length || TO_KEY[scancode] == 0) {
            return UNKNOWN;
        }
        return TO_KEY[scancode];
    }

    public static int scancodeOf(int key) {
        if (key < 0 || key > HIGHEST) {
            return Scancode.NONE;
        }
        return TO_SCANCODE[key];
    }
}
