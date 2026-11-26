package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.audio.AudioDevice;

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
