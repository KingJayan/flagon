package org.lwjgl.openal;

/**
 * the output a context is created on. a page has one mixer and no choice of output, so every
 * open hands back the same one.
 */
public final class ALCdevice {

    /** the number lwjgl 3 passes for this device */
    final long handle;
    private final String specifier;
    private boolean open = true;

    ALCdevice(long handle, String specifier) {
        this.handle = handle;
        this.specifier = specifier;
    }

    public String specifier() {
        return specifier;
    }

    public boolean isValid() {
        return open;
    }

    void close() {
        open = false;
    }
}
