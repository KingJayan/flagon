package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** openal 1.1 contexts. a page has no capture device, so every capture call is refused */
public class ALC11 extends ALC10 {

    public static final int ALC_MONO_SOURCES = 0x1010;
    public static final int ALC_STEREO_SOURCES = 0x1011;
    public static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    public static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;
    public static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    public static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    public static final int ALC_CAPTURE_SAMPLES = 0x312;

    public ALC11() {
    }

    public static ALCdevice alcCaptureOpenDevice(String name, int frequency, int format,
                                                 int samples) {
        fail(ALC_INVALID_VALUE);
        return null;
    }

    public static long alcCaptureOpenDevice(CharSequence name, int frequency, int format,
                                            int samples) {
        fail(ALC_INVALID_VALUE);
        return 0L;
    }

    public static long alcCaptureOpenDevice(ByteBuffer name, int frequency, int format,
                                            int samples) {
        fail(ALC_INVALID_VALUE);
        return 0L;
    }

    public static boolean alcCaptureCloseDevice(ALCdevice device) {
        fail(ALC_INVALID_DEVICE);
        return false;
    }

    public static boolean alcCaptureCloseDevice(long device) {
        fail(ALC_INVALID_DEVICE);
        return false;
    }

    public static void alcCaptureStart(ALCdevice device) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureStart(long device) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureStop(ALCdevice device) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureStop(long device) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(ALCdevice device, ByteBuffer buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, ByteBuffer buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, ShortBuffer buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, IntBuffer buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, FloatBuffer buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, short[] buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, int[] buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }

    public static void alcCaptureSamples(long device, float[] buffer, int samples) {
        fail(ALC_INVALID_DEVICE);
    }
}
