package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;

/**
 * openal setup in both forms: lwjgl 2 creates a device and context here, and lwjgl 3 makes the
 * capabilities of the context ALC10 made current
 */
public final class AL {

    private static ALCapabilities capabilities;

    private AL() {
    }

    public static void create() {
        if (isCreated()) {
            return;
        }
        ALCdevice device = ALC10.alcOpenDevice((String) null);
        ALC10.alcMakeContextCurrent(ALC10.alcCreateContext(device, null));
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh,
                              boolean contextSynchronized) {
        create();
    }

    public static void create(String deviceArguments, int contextFrequency, int contextRefresh,
                              boolean contextSynchronized, boolean openDevice) {
        create();
    }

    public static void destroy() {
        ALCcontext context = ALC10.alcGetCurrentContext();
        if (context == null) {
            ALState.detach();
            return;
        }
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(context.device());
    }

    public static boolean isCreated() {
        return ALState.isAttached();
    }

    public static ALCcontext getContext() {
        return ALC10.alcGetCurrentContext();
    }

    public static ALCdevice getDevice() {
        ALCcontext context = ALC10.alcGetCurrentContext();
        return context == null ? null : context.device();
    }

    public static ALCapabilities createCapabilities(ALCCapabilities deviceCapabilities) {
        if (!isCreated()) {
            throw new IllegalStateException("no openal context is current");
        }
        capabilities = new ALCapabilities();
        return capabilities;
    }

    public static ALCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException("no ALCapabilities; call AL.createCapabilities first");
        }
        return capabilities;
    }

    public static void setCurrentProcess(ALCapabilities newCapabilities) {
        capabilities = newCapabilities;
    }

    public static void setCurrentThread(ALCapabilities newCapabilities) {
        capabilities = newCapabilities;
    }
}
