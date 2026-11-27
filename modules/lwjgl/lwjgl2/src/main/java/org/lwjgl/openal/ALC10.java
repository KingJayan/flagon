package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.platform.Platforms;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * the context calls in both forms: lwjgl 2 passes {@link ALCdevice} and {@link ALCcontext}, and
 * lwjgl 3 passes a handle to the same objects
 */
public class ALC10 {

    public static final int ALC_INVALID = -1;
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

    private static final Map<Long, ALCdevice> DEVICES = new HashMap<>();
    private static final Map<Long, ALCcontext> CONTEXTS = new HashMap<>();

    private static ALCcontext current;
    private static int error = ALC_NO_ERROR;
    private static long lastHandle;

    protected ALC10() {
    }

    public static ALCdevice alcOpenDevice(String name) {
        ALCdevice device = new ALCdevice(++lastHandle,
                name == null || name.isEmpty() ? defaultSpecifier() : name);
        DEVICES.put(device.handle, device);
        return device;
    }

    public static long alcOpenDevice(CharSequence name) {
        return alcOpenDevice(name == null ? null : name.toString()).handle;
    }

    public static long alcOpenDevice(ByteBuffer name) {
        return alcOpenDevice(name == null ? null : Enums.text(name)).handle;
    }

    public static boolean alcCloseDevice(ALCdevice device) {
        if (device == null || !device.isValid()) {
            fail(ALC_INVALID_DEVICE);
            return false;
        }
        device.close();
        DEVICES.remove(device.handle);
        return true;
    }

    public static boolean alcCloseDevice(long device) {
        return alcCloseDevice(DEVICES.get(device));
    }

    public static ALCcontext alcCreateContext(ALCdevice device, IntBuffer attributes) {
        if (device == null || !device.isValid()) {
            fail(ALC_INVALID_DEVICE);
            return null;
        }
        ALCcontext context = new ALCcontext(++lastHandle, device);
        CONTEXTS.put(context.handle, context);
        return context;
    }

    public static long alcCreateContext(long device, IntBuffer attributes) {
        ALCcontext context = alcCreateContext(DEVICES.get(device), attributes);
        return context == null ? 0L : context.handle;
    }

    public static long alcCreateContext(long device, int[] attributes) {
        return alcCreateContext(device, (IntBuffer) null);
    }

    public static int alcMakeContextCurrent(ALCcontext context) {
        return makeCurrent(context) ? ALC_TRUE : ALC_FALSE;
    }

    public static boolean alcMakeContextCurrent(long context) {
        if (context != 0L && !CONTEXTS.containsKey(context)) {
            fail(ALC_INVALID_CONTEXT);
            return false;
        }
        return makeCurrent(CONTEXTS.get(context));
    }

    private static boolean makeCurrent(ALCcontext context) {
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

    public static long alcGetContextsDevice(long context) {
        ALCdevice device = alcGetContextsDevice(CONTEXTS.get(context));
        return device == null ? 0L : device.handle;
    }

    public static void alcDestroyContext(ALCcontext context) {
        if (context == null || !context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
            return;
        }
        if (context == current) {
            makeCurrent(null);
        }
        context.destroy();
        CONTEXTS.remove(context.handle);
    }

    public static void alcDestroyContext(long context) {
        alcDestroyContext(CONTEXTS.get(context));
    }

    /** the mixer runs itself; there is nothing to start or stop. */
    public static void alcProcessContext(ALCcontext context) {
        require(context);
    }

    public static void alcProcessContext(long context) {
        require(CONTEXTS.get(context));
    }

    public static void alcSuspendContext(ALCcontext context) {
        require(context);
    }

    public static void alcSuspendContext(long context) {
        require(CONTEXTS.get(context));
    }

    public static int alcGetError(ALCdevice device) {
        int taken = error;
        error = ALC_NO_ERROR;
        return taken;
    }

    public static int alcGetError(long device) {
        return alcGetError(DEVICES.get(device));
    }

    public static boolean alcIsExtensionPresent(ALCdevice device, String extension) {
        return false;
    }

    public static boolean alcIsExtensionPresent(long device, CharSequence extension) {
        return false;
    }

    public static boolean alcIsExtensionPresent(long device, ByteBuffer extension) {
        return false;
    }

    public static int alcGetEnumValue(ALCdevice device, String name) {
        return Enums.valueOf(name);
    }

    public static int alcGetEnumValue(long device, CharSequence name) {
        return Enums.valueOf(name.toString());
    }

    public static int alcGetEnumValue(long device, ByteBuffer name) {
        return Enums.valueOf(Enums.text(name));
    }

    /** there is no native function to hand out */
    public static long alcGetProcAddress(long device, CharSequence name) {
        return 0L;
    }

    public static long alcGetProcAddress(long device, ByteBuffer name) {
        return 0L;
    }

    public static String alcGetString(ALCdevice device, int token) {
        switch (token) {
            case ALC_DEFAULT_DEVICE_SPECIFIER:
            case ALC_DEVICE_SPECIFIER:
            case ALC11.ALC_DEFAULT_ALL_DEVICES_SPECIFIER:
            case ALC11.ALC_ALL_DEVICES_SPECIFIER:
                return device != null && device.isValid() ? device.specifier() : defaultSpecifier();
            case ALC_EXTENSIONS:
            case ALC11.ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER:
            case ALC11.ALC_CAPTURE_DEVICE_SPECIFIER:
                return "";
            default:
                fail(ALC_INVALID_ENUM);
                return null;
        }
    }

    public static String alcGetString(long device, int token) {
        return alcGetString(DEVICES.get(device), token);
    }

    public static int alcGetInteger(ALCdevice device, int token) {
        switch (token) {
            case ALC_MAJOR_VERSION:
                return 1;
            case ALC_MINOR_VERSION:
                return 1;
            case ALC_ATTRIBUTES_SIZE:
            case ALC_ALL_ATTRIBUTES:
                return 0;
            default:
                fail(ALC_INVALID_ENUM);
                return 0;
        }
    }

    public static int alcGetInteger(long device, int token) {
        return alcGetInteger(DEVICES.get(device), token);
    }

    public static void alcGetInteger(ALCdevice device, int token, IntBuffer value) {
        value.put(value.position(), alcGetInteger(device, token));
    }

    public static void alcGetIntegerv(long device, int token, IntBuffer value) {
        alcGetInteger(DEVICES.get(device), token, value);
    }

    public static void alcGetIntegerv(long device, int token, int[] value) {
        value[0] = alcGetInteger(device, token);
    }

    private static void require(ALCcontext context) {
        if (context == null || !context.isValid()) {
            fail(ALC_INVALID_CONTEXT);
        }
    }

    private static String defaultSpecifier() {
        return Platforms.get().audio().name();
    }

    static void fail(int code) {
        if (error == ALC_NO_ERROR) {
            error = code;
        }
    }
}
