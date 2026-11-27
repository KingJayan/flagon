package org.lwjgl.openal;

/** where lwjgl 3 loads openal. the mixer is always there, so there is nothing to load */
public final class ALC {

    private static ALCCapabilities capabilities;

    private ALC() {
    }

    public static void create() {
    }

    public static void create(String libraryName) {
    }

    public static void destroy() {
        capabilities = null;
    }

    public static ALCCapabilities createCapabilities(long device) {
        capabilities = new ALCCapabilities();
        return capabilities;
    }

    public static ALCCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException("no ALCCapabilities; call ALC.createCapabilities first");
        }
        return capabilities;
    }

    public static void setCapabilities(ALCCapabilities newCapabilities) {
        capabilities = newCapabilities;
    }
}
