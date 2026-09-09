package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.platform.Platforms;

import java.nio.IntBuffer;

/**
 * the openal context layer, which a game opens before any al* call.
 *
 * <p>a page has one mixer, so the device a name asks for is the one it gets. attributes that
 * choose a frequency, a refresh rate or a sync mode are accepted and ignored, the browser
 * owning those.
 */
public final class ALC10 {

    public static final int ALC_FALSE = 0;
    public static final int ALC_TRUE = 1;

    public static final int ALC_FREQUENCY = 0x1007;
    public static final int ALC_REFRESH = 0x1008;
    public static final int ALC_SYNC = 0x1009;

    public static final int ALC_NO_ERROR = 0;
    public static final int ALC_INVALID_DEVICE = 0xA001;
    public static final int ALC_INVALID_CONTEXT = 0xA002;
    public static final int ALC_INVALID_ENUM = 0xA003;
    public static final int ALC_INVALID_VALUE = 0xA004;
    public static final int ALC_OUT_OF_MEMORY = 0xA005;

    public static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    public static final int ALC_DEVICE_SPECIFIER = 0x1005;
    public static final int ALC_EXTENSIONS = 0x1006;
    public static final int ALC_MAJOR_VERSION = 0x1000;
    public static final int ALC_MINOR_VERSION = 0x1001;
    public static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    public static final int ALC_ALL_ATTRIBUTES = 0x1003;

    private static ALCcontext current;
    private static int error = ALC_NO_ERROR;

    private ALC10() {
    }

    public static ALCdevice alcOpenDevice(String name) {
        return new ALCdevice(name == null || name.isEmpty() ? defaultSpecifier() : name);
    }

    public static boolean alcCloseDevice(ALCdevice device) {
        if (device == null || !device.isValid()) {
            fail(ALC_INVALID_DEVICE);
            return false;
        }
        device.close();
        return true;
    }

    public static ALCcontext alcCreateContext(ALCdevice device, IntBuffer attributes) {
        if (device == null || !device.isValid()) {
            fail(ALC_INVALID_DEVICE);
            return null;
        }
        return new ALCcontext(device);
    }

    /** a null context detaches the mixer, which is what {@code AL.destroy()} does too. */
    public static boolean alcMakeContextCurrent(ALCcontext context) {
        if (context == null) {
            current = null;
            ALState.detach();
            return true;
        }
        if (!context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
            return false;
        }
        current = context;
        ALState.attach(Platforms.get().audio(), Platforms.get().console());
        return true;
    }

    public static ALCcontext alcGetCurrentContext() {
        return current;
    }

    public static ALCdevice alcGetContextsDevice(ALCcontext context) {
        if (context == null || !context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
            return null;
        }
        return context.device();
    }

    public static void alcDestroyContext(ALCcontext context) {
        if (context == null || !context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
            return;
        }
        if (context == current) {
            alcMakeContextCurrent(null);
        }
        context.destroy();
    }

    /** the mixer runs itself; there is nothing to start or stop. */
    public static void alcProcessContext(ALCcontext context) {
        require(context);
    }

    public static void alcSuspendContext(ALCcontext context) {
        require(context);
    }

    public static int alcGetError(ALCdevice device) {
        int taken = error;
        error = ALC_NO_ERROR;
        return taken;
    }

    public static boolean alcIsExtensionPresent(ALCdevice device, String extension) {
        return false;
    }

    public static String alcGetString(ALCdevice device, int token) {
        switch (token) {
            case ALC_DEFAULT_DEVICE_SPECIFIER:
            case ALC_DEVICE_SPECIFIER:
                return device != null && device.isValid() ? device.specifier() : defaultSpecifier();
            case ALC_EXTENSIONS:
                return "";
            default:
                fail(ALC_INVALID_ENUM);
                return null;
        }
    }

    public static int alcGetInteger(ALCdevice device, int token) {
        switch (token) {
            case ALC_MAJOR_VERSION:
                return 1;
            case ALC_MINOR_VERSION:
                return 1;
            case ALC_ATTRIBUTES_SIZE:
                return 0;
            default:
                fail(ALC_INVALID_ENUM);
                return 0;
        }
    }

    private static void require(ALCcontext context) {
        if (context == null || !context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
        }
    }

    private static String defaultSpecifier() {
        return Platforms.get().audio().name();
    }

    private static void fail(int code) {
        if (error == ALC_NO_ERROR) {
            error = code;
        }
    }
}
