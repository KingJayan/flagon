package org.lwjgl.openal;

/**
 * the output a context is created on. a page has one mixer and no choice of output, so every
 * open hands back the same one.
 */
public final class ALCdevice {

    private final String specifier;
    private boolean open = true;

    ALCdevice(String specifier) {
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
