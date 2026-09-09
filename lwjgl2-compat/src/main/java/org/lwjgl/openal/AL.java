package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.audio.AudioDevice;

/**
 * the openal context.
 *
 * <p>desktop openal opens a device and a context on it; a page has one mixer and no choice of
 * output, so the arguments that select one are accepted and ignored.
 */
public final class AL {

    private AL() {
    }

    public static void create() {
        if (isCreated()) {
            return;
        }
        AudioDevice device = Platforms.get().audio();
        ALState.attach(device, Platforms.get().console());
    }

    /** the device name, frequency, refresh and sync a page has no way to choose. */
    public static void create(String deviceArguments, int contextFrequency, int contextRefresh,
                              boolean contextSynchronized) {
        create();
    }

    public static void destroy() {
        ALState.detach();
    }

    public static boolean isCreated() {
        return ALState.isAttached();
    }
}
