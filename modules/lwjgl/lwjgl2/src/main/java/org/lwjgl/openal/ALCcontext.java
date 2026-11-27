package org.lwjgl.openal;

/** a context on an {@link ALCdevice}; only one can be current, as openal has it. */
public final class ALCcontext {

    /** the number lwjgl 3 passes for this context */
    final long handle;
    private final ALCdevice device;
    private boolean alive = true;

    ALCcontext(long handle, ALCdevice device) {
        this.handle = handle;
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
