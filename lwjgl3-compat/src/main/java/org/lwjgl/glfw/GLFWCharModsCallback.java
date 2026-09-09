package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWCharModsCallback extends Callback implements GLFWCharModsCallbackI {

    public static GLFWCharModsCallback create(GLFWCharModsCallbackI delegate) {
        return delegate instanceof GLFWCharModsCallback already ? already : new GLFWCharModsCallback() {
            @Override
            public void invoke(long window, int codepoint, int mods) {
                delegate.invoke(window, codepoint, mods);
            }
        };
    }
}
