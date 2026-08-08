package org.lwjgl.openal;

/** a context on an {@link ALCdevice}; only one can be current, as openal has it. */
public final class ALCcontext {

    private final ALCdevice device;
    private boolean alive = true;

    ALCcontext(ALCdevice device) {
        this.device = device;
    }

    public ALCdevice device() {
        return device;
    }

    public boolean isValid() {
        return alive;
    }

    void destroy() {
        alive = false;
    }
}
