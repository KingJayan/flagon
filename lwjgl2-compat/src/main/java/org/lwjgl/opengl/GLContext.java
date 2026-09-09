package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsInfo;

/**
 * the way lwjgl applications reach the capabilities of the current context.
 *
 * <p>there is one context here, the one {@code Display.create()} opened, so the capabilities are
 * built once per device and handed back until that device goes away.
 */
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

    /**
     * rereads the host's extension list.
     *
     * <p>lwjgl uses this to bind a thread to a context it was handed; there is only ever the one
     * context here, so the argument names which one and is otherwise unused.
     */
    public static void useContext(Object context) {
        described = null;
        capabilities = null;
        getCapabilities();
    }
}
