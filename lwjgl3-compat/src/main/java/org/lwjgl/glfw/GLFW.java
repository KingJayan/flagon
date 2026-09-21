package org.lwjgl.glfw;

import dev.flagon.lwjgl.internal.Addresses;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.GlfwKeys;
import dev.flagon.lwjgl.internal.InputState;
import dev.flagon.platform.Platform;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.input.InputSource;
import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.MouseEvent;
import dev.flagon.platform.input.Scancode;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

/**
 * glfw over one display surface. the page owns the window, so hints that ask for another are
 * remembered and reported rather than obeyed.
 */
public final class GLFW {

    // real lwjgl leaves NULL to MemoryUtil, and a program static-importing both would not
    // compile if this class named it too
    static final long NULL = Addresses.NULL;

    public static final int GLFW_FALSE = 0;
    public static final int GLFW_TRUE = 1;
    public static final int GLFW_DONT_CARE = -1;

    public static final int GLFW_RELEASE = 0;
    public static final int GLFW_PRESS = 1;
    public static final int GLFW_REPEAT = 2;

    public static final int GLFW_KEY_UNKNOWN = -1;
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_APOSTROPHE = 39;
    public static final int GLFW_KEY_COMMA = 44;
    public static final int GLFW_KEY_MINUS = 45;
    public static final int GLFW_KEY_PERIOD = 46;
    public static final int GLFW_KEY_SLASH = 47;
    public static final int GLFW_KEY_0 = 48;
    public static final int GLFW_KEY_1 = 49;
    public static final int GLFW_KEY_2 = 50;
    public static final int GLFW_KEY_3 = 51;
    public static final int GLFW_KEY_4 = 52;
    public static final int GLFW_KEY_5 = 53;
    public static final int GLFW_KEY_6 = 54;
    public static final int GLFW_KEY_7 = 55;
    public static final int GLFW_KEY_8 = 56;
    public static final int GLFW_KEY_9 = 57;
    public static final int GLFW_KEY_SEMICOLON = 59;
    public static final int GLFW_KEY_EQUAL = 61;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_B = 66;
    public static final int GLFW_KEY_C = 67;
    public static final int GLFW_KEY_D = 68;
    public static final int GLFW_KEY_E = 69;
    public static final int GLFW_KEY_F = 70;
    public static final int GLFW_KEY_G = 71;
    public static final int GLFW_KEY_H = 72;
    public static final int GLFW_KEY_I = 73;
    public static final int GLFW_KEY_J = 74;
    public static final int GLFW_KEY_K = 75;
    public static final int GLFW_KEY_L = 76;
    public static final int GLFW_KEY_M = 77;
    public static final int GLFW_KEY_N = 78;
    public static final int GLFW_KEY_O = 79;
    public static final int GLFW_KEY_P = 80;
    public static final int GLFW_KEY_Q = 81;
    public static final int GLFW_KEY_R = 82;
    public static final int GLFW_KEY_S = 83;
    public static final int GLFW_KEY_T = 84;
    public static final int GLFW_KEY_U = 85;
    public static final int GLFW_KEY_V = 86;
    public static final int GLFW_KEY_W = 87;
    public static final int GLFW_KEY_X = 88;
    public static final int GLFW_KEY_Y = 89;
    public static final int GLFW_KEY_Z = 90;
    public static final int GLFW_KEY_LEFT_BRACKET = 91;
    public static final int GLFW_KEY_BACKSLASH = 92;
    public static final int GLFW_KEY_RIGHT_BRACKET = 93;
    public static final int GLFW_KEY_GRAVE_ACCENT = 96;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static final int GLFW_KEY_ENTER = 257;
    public static final int GLFW_KEY_TAB = 258;
    public static final int GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_INSERT = 260;
    public static final int GLFW_KEY_DELETE = 261;
    public static final int GLFW_KEY_RIGHT = 262;
    public static final int GLFW_KEY_LEFT = 263;
    public static final int GLFW_KEY_DOWN = 264;
    public static final int GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_PAGE_UP = 266;
    public static final int GLFW_KEY_PAGE_DOWN = 267;
    public static final int GLFW_KEY_HOME = 268;
    public static final int GLFW_KEY_END = 269;
    public static final int GLFW_KEY_CAPS_LOCK = 280;
    public static final int GLFW_KEY_SCROLL_LOCK = 281;
    public static final int GLFW_KEY_NUM_LOCK = 282;
    public static final int GLFW_KEY_PRINT_SCREEN = 283;
    public static final int GLFW_KEY_PAUSE = 284;
    public static final int GLFW_KEY_F1 = 290;
    public static final int GLFW_KEY_F2 = 291;
    public static final int GLFW_KEY_F3 = 292;
    public static final int GLFW_KEY_F4 = 293;
    public static final int GLFW_KEY_F5 = 294;
    public static final int GLFW_KEY_F6 = 295;
    public static final int GLFW_KEY_F7 = 296;
    public static final int GLFW_KEY_F8 = 297;
    public static final int GLFW_KEY_F9 = 298;
    public static final int GLFW_KEY_F10 = 299;
    public static final int GLFW_KEY_F11 = 300;
    public static final int GLFW_KEY_F12 = 301;
    public static final int GLFW_KEY_KP_0 = 320;
    public static final int GLFW_KEY_KP_1 = 321;
    public static final int GLFW_KEY_KP_2 = 322;
    public static final int GLFW_KEY_KP_3 = 323;
    public static final int GLFW_KEY_KP_4 = 324;
    public static final int GLFW_KEY_KP_5 = 325;
    public static final int GLFW_KEY_KP_6 = 326;
    public static final int GLFW_KEY_KP_7 = 327;
    public static final int GLFW_KEY_KP_8 = 328;
    public static final int GLFW_KEY_KP_9 = 329;
    public static final int GLFW_KEY_KP_DECIMAL = 330;
    public static final int GLFW_KEY_KP_DIVIDE = 331;
    public static final int GLFW_KEY_KP_MULTIPLY = 332;
    public static final int GLFW_KEY_KP_SUBTRACT = 333;
    public static final int GLFW_KEY_KP_ADD = 334;
    public static final int GLFW_KEY_KP_ENTER = 335;
    public static final int GLFW_KEY_KP_EQUAL = 336;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_LEFT_CONTROL = 341;
    public static final int GLFW_KEY_LEFT_ALT = 342;
    public static final int GLFW_KEY_LEFT_SUPER = 343;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_KEY_RIGHT_CONTROL = 345;
    public static final int GLFW_KEY_RIGHT_ALT = 346;
    public static final int GLFW_KEY_RIGHT_SUPER = 347;
    public static final int GLFW_KEY_MENU = 348;
    public static final int GLFW_KEY_LAST = GLFW_KEY_MENU;

    public static final int GLFW_MOD_SHIFT = 0x0001;
    public static final int GLFW_MOD_CONTROL = 0x0002;
    public static final int GLFW_MOD_ALT = 0x0004;
    public static final int GLFW_MOD_SUPER = 0x0008;

    public static final int GLFW_MOUSE_BUTTON_1 = 0;
    public static final int GLFW_MOUSE_BUTTON_2 = 1;
    public static final int GLFW_MOUSE_BUTTON_3 = 2;
    public static final int GLFW_MOUSE_BUTTON_4 = 3;
    public static final int GLFW_MOUSE_BUTTON_5 = 4;
    public static final int GLFW_MOUSE_BUTTON_LEFT = GLFW_MOUSE_BUTTON_1;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = GLFW_MOUSE_BUTTON_2;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_3;
    public static final int GLFW_MOUSE_BUTTON_LAST = GLFW_MOUSE_BUTTON_5;

    public static final int GLFW_CURSOR = 0x00033001;
    public static final int GLFW_STICKY_KEYS = 0x00033002;
    public static final int GLFW_STICKY_MOUSE_BUTTONS = 0x00033003;
    public static final int GLFW_RAW_MOUSE_MOTION = 0x00033005;
    public static final int GLFW_CURSOR_NORMAL = 0x00034001;
    public static final int GLFW_CURSOR_HIDDEN = 0x00034002;
    public static final int GLFW_CURSOR_DISABLED = 0x00034003;

    public static final int GLFW_FOCUSED = 0x00020001;
    public static final int GLFW_ICONIFIED = 0x00020002;
    public static final int GLFW_RESIZABLE = 0x00020003;
    public static final int GLFW_VISIBLE = 0x00020004;
    public static final int GLFW_DECORATED = 0x00020005;
    public static final int GLFW_AUTO_ICONIFY = 0x00020006;
    public static final int GLFW_FLOATING = 0x00020007;
    public static final int GLFW_MAXIMIZED = 0x00020008;

    public static final int GLFW_RED_BITS = 0x00021001;
    public static final int GLFW_GREEN_BITS = 0x00021002;
    public static final int GLFW_BLUE_BITS = 0x00021003;
    public static final int GLFW_ALPHA_BITS = 0x00021004;
    public static final int GLFW_DEPTH_BITS = 0x00021005;
    public static final int GLFW_STENCIL_BITS = 0x00021006;
    public static final int GLFW_SAMPLES = 0x0002100D;
    public static final int GLFW_REFRESH_RATE = 0x0002100F;
    public static final int GLFW_DOUBLEBUFFER = 0x00021010;

    public static final int GLFW_CLIENT_API = 0x00022001;
    public static final int GLFW_CONTEXT_VERSION_MAJOR = 0x00022002;
    public static final int GLFW_CONTEXT_VERSION_MINOR = 0x00022003;
    public static final int GLFW_OPENGL_FORWARD_COMPAT = 0x00022006;
    public static final int GLFW_OPENGL_DEBUG_CONTEXT = 0x00022007;
    public static final int GLFW_OPENGL_PROFILE = 0x00022008;
    public static final int GLFW_OPENGL_API = 0x00030001;
    public static final int GLFW_OPENGL_ANY_PROFILE = 0;
    public static final int GLFW_OPENGL_CORE_PROFILE = 0x00032001;
    public static final int GLFW_OPENGL_COMPAT_PROFILE = 0x00032002;

    public static final int GLFW_NO_ERROR = 0;
    public static final int GLFW_NOT_INITIALIZED = 0x00010001;
    public static final int GLFW_INVALID_ENUM = 0x00010003;
    public static final int GLFW_INVALID_VALUE = 0x00010004;
    public static final int GLFW_PLATFORM_ERROR = 0x00010008;

    private static final int DEFAULT_REFRESH_RATE = 60;
    private static final long MONITOR = 1L;

    private static boolean initialised;
    /** bytes the window handle names */
    private static final java.nio.ByteBuffer WINDOW = java.nio.ByteBuffer.allocateDirect(1);

    private static long window;
    private static DisplaySurface surface;
    private static String title = "Flagon";
    private static boolean shouldClose;
    private static int cursorMode = GLFW_CURSOR_NORMAL;
    private static int lastError = GLFW_NO_ERROR;
    private static long started;

    private static final int[] HINTS = new int[64];
    private static final boolean[] KEYS = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] BUTTONS = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private static GLFWErrorCallbackI onError;
    private static GLFWKeyCallbackI onKey;
    private static GLFWCharCallbackI onChar;
    private static GLFWCharModsCallbackI onCharMods;
    private static GLFWMouseButtonCallbackI onMouseButton;
    private static GLFWCursorPosCallbackI onCursorPos;
    private static GLFWCursorEnterCallbackI onCursorEnter;
    private static GLFWScrollCallbackI onScroll;
    private static GLFWWindowSizeCallbackI onWindowSize;
    private static GLFWFramebufferSizeCallbackI onFramebufferSize;
    private static GLFWWindowFocusCallbackI onWindowFocus;
    private static GLFWWindowCloseCallbackI onWindowClose;
    private static GLFWWindowPosCallbackI onWindowPos;

    private GLFW() {
    }

    public static boolean glfwInit() {
        initialised = true;
        started = Platforms.get().clock().nanoTime();
        return true;
    }

    public static void glfwTerminate() {
        if (window != NULL) {
            glfwDestroyWindow(window);
        }
        initialised = false;
        // glfw keeps the error callback across a terminate, so the usual
        // glfwSetErrorCallback(null).free() afterwards still has one to free
    }

    public static GLFWErrorCallback glfwSetErrorCallback(GLFWErrorCallbackI callback) {
        GLFWErrorCallback previous = onError == null ? null : GLFWErrorCallback.create(onError);
        onError = callback;
        return previous;
    }

    /** @return the last error, whose description this host has no address to hand back */
    public static int glfwGetError(java.nio.ByteBuffer description) {
        int error = lastError;
        lastError = GLFW_NO_ERROR;
        return error;
    }

    public static void glfwDefaultWindowHints() {
        java.util.Arrays.fill(HINTS, 0);
    }

    public static void glfwWindowHint(int hint, int value) {
        HINTS[slotOf(hint)] = value;
    }

    public static void glfwWindowHintString(int hint, CharSequence value) {
    }

    /** the page is the window, so a hint is remembered rather than obeyed */
    private static int slotOf(int hint) {
        return Math.floorMod(hint, HINTS.length);
    }

    public static long glfwCreateWindow(int width, int height, CharSequence name,
                                        long monitor, long share) {
        if (!initialised) {
            fail(GLFW_NOT_INITIALIZED, "glfwCreateWindow before glfwInit");
            return NULL;
        }
        if (window != NULL) {
            fail(GLFW_PLATFORM_ERROR, "the page holds one window and it is already open");
            return NULL;
        }
        Platform platform = Platforms.get();
        if (!platform.hasDisplay()) {
            fail(GLFW_PLATFORM_ERROR, "this host cannot open a display");
            return NULL;
        }
        title = name.toString();
        try {
            surface = platform.display(width, height, title);
        } catch (UnsupportedCapabilityException refused) {
            fail(GLFW_PLATFORM_ERROR, refused.getMessage());
            return NULL;
        }
        surface.graphics().viewport(0, 0, surface.width(), surface.height());
        GLState.attach(surface.graphics());
        InputState.attach(surface.input());
        shouldClose = false;
        window = Addresses.of(WINDOW);
        return window;
    }

    public static void glfwDestroyWindow(long handle) {
        if (handle != window || surface == null) {
            return;
        }
        InputState.detach();
        GLState.detach();
        surface.destroy();
        surface = null;
        window = NULL;
        java.util.Arrays.fill(KEYS, false);
        java.util.Arrays.fill(BUTTONS, false);
    }

    public static void glfwMakeContextCurrent(long handle) {
    }

    public static long glfwGetCurrentContext() {
        return window;
    }

    public static void glfwSwapInterval(int interval) {
    }

    public static void glfwSwapBuffers(long handle) {
        required(handle).present();
    }

    public static boolean glfwWindowShouldClose(long handle) {
        return shouldClose || (surface != null && surface.isCloseRequested());
    }

    public static void glfwSetWindowShouldClose(long handle, boolean value) {
        shouldClose = value;
    }

    public static void glfwSetWindowTitle(long handle, CharSequence name) {
        title = name.toString();
        required(handle).setTitle(title);
    }

    public static void glfwSetWindowSize(long handle, int width, int height) {
        required(handle).setSize(width, height);
    }

    public static void glfwGetWindowSize(long handle, int[] width, int[] height) {
        DisplaySurface open = required(handle);
        store(width, open.width());
        store(height, open.height());
    }

    public static void glfwGetWindowSize(long handle, IntBuffer width, IntBuffer height) {
        DisplaySurface open = required(handle);
        store(width, open.width());
        store(height, open.height());
    }

    public static void glfwGetFramebufferSize(long handle, int[] width, int[] height) {
        glfwGetWindowSize(handle, width, height);
    }

    public static void glfwGetFramebufferSize(long handle, IntBuffer width, IntBuffer height) {
        glfwGetWindowSize(handle, width, height);
    }

    public static void glfwGetWindowPos(long handle, int[] x, int[] y) {
        store(x, 0);
        store(y, 0);
    }

    public static void glfwSetWindowPos(long handle, int x, int y) {
    }

    public static void glfwShowWindow(long handle) {
    }

    public static void glfwHideWindow(long handle) {
    }

    public static void glfwFocusWindow(long handle) {
    }

    public static void glfwMaximizeWindow(long handle) {
    }

    public static void glfwIconifyWindow(long handle) {
    }

    public static void glfwRestoreWindow(long handle) {
    }

    public static void glfwSetWindowIcon(long handle, Object images) {
    }

    public static int glfwGetWindowAttrib(long handle, int attribute) {
        DisplaySurface open = required(handle);
        return switch (attribute) {
            case GLFW_FOCUSED -> open.isFocused() ? GLFW_TRUE : GLFW_FALSE;
            case GLFW_VISIBLE -> open.isVisible() ? GLFW_TRUE : GLFW_FALSE;
            case GLFW_ICONIFIED -> GLFW_FALSE;
            case GLFW_RESIZABLE, GLFW_DECORATED, GLFW_DOUBLEBUFFER -> GLFW_TRUE;
            default -> GLFW_FALSE;
        };
    }

    public static long glfwGetPrimaryMonitor() {
        return MONITOR;
    }

    public static GLFWVidMode glfwGetVideoMode(long monitor) {
        DisplaySurface open = surface;
        return open == null
                ? new GLFWVidMode(854, 480, DEFAULT_REFRESH_RATE)
                : new GLFWVidMode(open.width(), open.height(), DEFAULT_REFRESH_RATE);
    }

    /** the page decides how fullscreen is entered, so this asks the surface and reports back */
    public static void glfwSetWindowMonitor(long handle, long monitor, int x, int y,
                                            int width, int height, int refreshRate) {
        required(handle).setFullscreen(monitor != NULL);
    }

    public static long glfwGetWindowMonitor(long handle) {
        return required(handle).isFullscreen() ? MONITOR : NULL;
    }

    public static void glfwPollEvents() {
        if (surface == null) {
            return;
        }
        InputSource input = surface.input();
        drainKeys(input);
        drainMouse(input);
        if (glfwWindowShouldClose(window) && onWindowClose != null) {
            onWindowClose.invoke(window);
        }
    }

    public static void glfwWaitEvents() {
        glfwPollEvents();
    }

    public static void glfwWaitEventsTimeout(double timeout) {
        glfwPollEvents();
    }

    public static void glfwPostEmptyEvent() {
    }

    private static void drainKeys(InputSource input) {
        KeyEvent event;
        while ((event = input.nextKeyEvent()) != null) {
            int key = GlfwKeys.keyOf(event.key());
            int action = event.isPressed()
                    ? (event.isRepeat() ? GLFW_REPEAT : GLFW_PRESS)
                    : GLFW_RELEASE;
            if (key != GLFW_KEY_UNKNOWN) {
                KEYS[key] = event.isPressed();
            }
            if (onKey != null) {
                onKey.invoke(window, key, event.key(), action, modifiers());
            }
            if (event.isPressed() && event.character() != KeyEvent.NO_CHARACTER) {
                if (onChar != null) {
                    onChar.invoke(window, event.character());
                }
                if (onCharMods != null) {
                    onCharMods.invoke(window, event.character(), modifiers());
                }
            }
        }
    }

    private static void drainMouse(InputSource input) {
        MouseEvent event;
        while ((event = input.nextMouseEvent()) != null) {
            if (event.button() != MouseEvent.NO_BUTTON
                    && event.button() < BUTTONS.length) {
                BUTTONS[event.button()] = event.isPressed();
                if (onMouseButton != null) {
                    onMouseButton.invoke(window, event.button(),
                            event.isPressed() ? GLFW_PRESS : GLFW_RELEASE, modifiers());
                }
            }
            if ((event.deltaX() != 0 || event.deltaY() != 0) && onCursorPos != null) {
                onCursorPos.invoke(window, event.x(), topDown(event.y()));
            }
            if (event.wheel() != 0 && onScroll != null) {
                onScroll.invoke(window, 0.0, event.wheel() / 120.0);
            }
        }
    }

    /** glfw counts the pointer down from the top; the platform counts up from the bottom */
    private static double topDown(int y) {
        return surface == null ? y : surface.height() - 1 - y;
    }

    private static int modifiers() {
        int mods = 0;
        if (KEYS[GLFW_KEY_LEFT_SHIFT] || KEYS[GLFW_KEY_RIGHT_SHIFT]) {
            mods |= GLFW_MOD_SHIFT;
        }
        if (KEYS[GLFW_KEY_LEFT_CONTROL] || KEYS[GLFW_KEY_RIGHT_CONTROL]) {
            mods |= GLFW_MOD_CONTROL;
        }
        if (KEYS[GLFW_KEY_LEFT_ALT] || KEYS[GLFW_KEY_RIGHT_ALT]) {
            mods |= GLFW_MOD_ALT;
        }
        if (KEYS[GLFW_KEY_LEFT_SUPER] || KEYS[GLFW_KEY_RIGHT_SUPER]) {
            mods |= GLFW_MOD_SUPER;
        }
        return mods;
    }

    public static int glfwGetKey(long handle, int key) {
        if (key < 0 || key >= KEYS.length) {
            return GLFW_RELEASE;
        }
        if (surface != null) {
            int scancode = GlfwKeys.scancodeOf(key);
            if (scancode != Scancode.NONE) {
                return surface.input().isKeyDown(scancode) ? GLFW_PRESS : GLFW_RELEASE;
            }
        }
        return KEYS[key] ? GLFW_PRESS : GLFW_RELEASE;
    }

    public static int glfwGetMouseButton(long handle, int button) {
        if (button < 0 || button >= BUTTONS.length) {
            return GLFW_RELEASE;
        }
        return surface != null && surface.input().isButtonDown(button)
                ? GLFW_PRESS : GLFW_RELEASE;
    }

    public static void glfwGetCursorPos(long handle, double[] x, double[] y) {
        InputSource input = required(handle).input();
        store(x, input.mouseX());
        store(y, topDown(input.mouseY()));
    }

    public static void glfwGetCursorPos(long handle, DoubleBuffer x, DoubleBuffer y) {
        InputSource input = required(handle).input();
        store(x, input.mouseX());
        store(y, topDown(input.mouseY()));
    }

    public static void glfwSetCursorPos(long handle, double x, double y) {
    }

    public static void glfwSetInputMode(long handle, int mode, int value) {
        if (mode != GLFW_CURSOR) {
            return;
        }
        boolean grabbed = required(handle).input().setGrabbed(value == GLFW_CURSOR_DISABLED);
        // a host may require a gesture and refuse, and the caller has to see that it did
        cursorMode = grabbed ? value : GLFW_CURSOR_NORMAL;
    }

    public static int glfwGetInputMode(long handle, int mode) {
        if (mode != GLFW_CURSOR) {
            return GLFW_FALSE;
        }
        return required(handle).input().isGrabbed() ? GLFW_CURSOR_DISABLED : cursorMode;
    }

    public static boolean glfwRawMouseMotionSupported() {
        return false;
    }

    public static String glfwGetKeyName(int key, int scancode) {
        return null;
    }

    public static int glfwGetKeyScancode(int key) {
        return GlfwKeys.scancodeOf(key);
    }

    public static double glfwGetTime() {
        return (Platforms.get().clock().nanoTime() - started) / 1_000_000_000.0;
    }

    public static void glfwSetTime(double time) {
        started = Platforms.get().clock().nanoTime() - (long) (time * 1_000_000_000.0);
    }

    public static boolean glfwJoystickPresent(int id) {
        return false;
    }

    public static String glfwGetJoystickName(int id) {
        return null;
    }

    public static GLFWKeyCallback glfwSetKeyCallback(long handle, GLFWKeyCallbackI callback) {
        GLFWKeyCallback previous = onKey == null ? null : GLFWKeyCallback.create(onKey);
        onKey = callback;
        return previous;
    }

    public static GLFWCharCallback glfwSetCharCallback(long handle, GLFWCharCallbackI callback) {
        GLFWCharCallback previous = onChar == null ? null : GLFWCharCallback.create(onChar);
        onChar = callback;
        return previous;
    }

    public static GLFWCharModsCallback glfwSetCharModsCallback(long handle,
                                                               GLFWCharModsCallbackI callback) {
        GLFWCharModsCallback previous =
                onCharMods == null ? null : GLFWCharModsCallback.create(onCharMods);
        onCharMods = callback;
        return previous;
    }

    public static GLFWMouseButtonCallback glfwSetMouseButtonCallback(
            long handle, GLFWMouseButtonCallbackI callback) {
        GLFWMouseButtonCallback previous =
                onMouseButton == null ? null : GLFWMouseButtonCallback.create(onMouseButton);
        onMouseButton = callback;
        return previous;
    }

    public static GLFWCursorPosCallback glfwSetCursorPosCallback(
            long handle, GLFWCursorPosCallbackI callback) {
        GLFWCursorPosCallback previous =
                onCursorPos == null ? null : GLFWCursorPosCallback.create(onCursorPos);
        onCursorPos = callback;
        return previous;
    }

    public static GLFWCursorEnterCallback glfwSetCursorEnterCallback(
            long handle, GLFWCursorEnterCallbackI callback) {
        GLFWCursorEnterCallback previous =
                onCursorEnter == null ? null : GLFWCursorEnterCallback.create(onCursorEnter);
        onCursorEnter = callback;
        return previous;
    }

    public static GLFWScrollCallback glfwSetScrollCallback(long handle,
                                                           GLFWScrollCallbackI callback) {
        GLFWScrollCallback previous =
                onScroll == null ? null : GLFWScrollCallback.create(onScroll);
        onScroll = callback;
        return previous;
    }

    public static GLFWWindowSizeCallback glfwSetWindowSizeCallback(
            long handle, GLFWWindowSizeCallbackI callback) {
        GLFWWindowSizeCallback previous =
                onWindowSize == null ? null : GLFWWindowSizeCallback.create(onWindowSize);
        onWindowSize = callback;
        return previous;
    }

    public static GLFWFramebufferSizeCallback glfwSetFramebufferSizeCallback(
            long handle, GLFWFramebufferSizeCallbackI callback) {
        GLFWFramebufferSizeCallback previous = onFramebufferSize == null
                ? null : GLFWFramebufferSizeCallback.create(onFramebufferSize);
        onFramebufferSize = callback;
        return previous;
    }

    public static GLFWWindowFocusCallback glfwSetWindowFocusCallback(
            long handle, GLFWWindowFocusCallbackI callback) {
        GLFWWindowFocusCallback previous =
                onWindowFocus == null ? null : GLFWWindowFocusCallback.create(onWindowFocus);
        onWindowFocus = callback;
        return previous;
    }

    public static GLFWWindowCloseCallback glfwSetWindowCloseCallback(
            long handle, GLFWWindowCloseCallbackI callback) {
        GLFWWindowCloseCallback previous =
                onWindowClose == null ? null : GLFWWindowCloseCallback.create(onWindowClose);
        onWindowClose = callback;
        return previous;
    }

    public static GLFWWindowPosCallback glfwSetWindowPosCallback(
            long handle, GLFWWindowPosCallbackI callback) {
        GLFWWindowPosCallback previous =
                onWindowPos == null ? null : GLFWWindowPosCallback.create(onWindowPos);
        onWindowPos = callback;
        return previous;
    }

    /** the surface changed size on its own, so tell whoever asked to hear about it */
    public static void resized(int width, int height) {
        if (onWindowSize != null) {
            onWindowSize.invoke(window, width, height);
        }
        if (onFramebufferSize != null) {
            onFramebufferSize.invoke(window, width, height);
        }
    }

    private static void fail(int error, String description) {
        lastError = error;
        if (onError != null) {
            onError.invoke(error, Addresses.of(
                    org.lwjgl.system.MemoryUtil.memUTF8(description)));
        }
    }

    private static void store(int[] target, int value) {
        if (target != null && target.length > 0) {
            target[0] = value;
        }
    }

    private static void store(IntBuffer target, int value) {
        if (target != null && target.remaining() > 0) {
            target.put(target.position(), value);
        }
    }

    private static void store(double[] target, double value) {
        if (target != null && target.length > 0) {
            target[0] = value;
        }
    }

    private static void store(DoubleBuffer target, double value) {
        if (target != null && target.remaining() > 0) {
            target.put(target.position(), value);
        }
    }

    private static DisplaySurface required(long handle) {
        if (surface == null) {
            throw new IllegalStateException("no glfw window is open");
        }
        return surface;
    }
}
