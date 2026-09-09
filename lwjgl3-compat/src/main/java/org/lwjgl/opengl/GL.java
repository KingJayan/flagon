package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.GraphicsInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/** the entry point an lwjgl 3 caller uses to bind gl to the current context. */
public final class GL {

    private static GLCapabilities capabilities;

    private GL() {
    }

    public static GLCapabilities createCapabilities() {
        capabilities = new GLCapabilities(reportedExtensions());
        return capabilities;
    }

    public static GLCapabilities createCapabilities(boolean forwardCompatible) {
        return createCapabilities();
    }

    public static GLCapabilities getCapabilities() {
        if (capabilities == null) {
            throw new IllegalStateException(
                    "no gl capabilities; GL.createCapabilities() has not been called on this thread");
        }
        return capabilities;
    }

    public static void setCapabilities(GLCapabilities newCapabilities) {
        capabilities = newCapabilities;
    }

    private static Set<String> reportedExtensions() {
        Set<String> names = new LinkedHashSet<>();
        String reported = GLState.get().device().information(GraphicsInfo.EXTENSIONS);
        if (reported != null && !reported.isBlank()) {
            for (String name : reported.trim().split("\\s+")) {
                names.add(name);
            }
        }
        return names;
    }
}
