package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsInfo;

public final class GLContext {

    private static GraphicsDevice described;
    private static ContextCapabilities capabilities;

    private GLContext() {
    }

    public static ContextCapabilities getCapabilities() {
        GraphicsDevice device = GLState.get().device();
        if (capabilities == null || described != device) {
            described = device;
            capabilities = new ContextCapabilities(device.information(GraphicsInfo.EXTENSIONS));
        }
        return capabilities;
    }

    public static void useContext(Object context) {
        described = null;
        capabilities = null;
        getCapabilities();
    }
}
