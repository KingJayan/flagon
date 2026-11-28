package org.lwjgl.opengles;

import dev.flagon.lwjgl.internal.GLState;

import java.util.Set;

/** binds gl es to the context glfw made current. webgl2 is gl es 3.0, so the calls pass through. */
public final class GLES {

    private static GLESCapabilities capabilities;

    private GLES() {
    }

    public static void create() {
    }

    public static void create(String libName) {
    }

    public static void destroy() {
    }

    public static GLESCapabilities createCapabilities() {
        if (!GLState.isAttached()) {
            throw new IllegalStateException(
                    "no current context; call glfwMakeContextCurrent before"
                            + " GLES.createCapabilities()");
        }
        capabilities = new GLESCapabilities(Set.of(Es.context().extensions()));
        return capabilities;
    }

    public static GLESCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException(
                    "no gl es capabilities; GLES.createCapabilities() has not been called on this"
                            + " thread");
        }
        return capabilities;
    }

    public static void setCapabilities(GLESCapabilities newCapabilities) {
        capabilities = newCapabilities;
    }
}
