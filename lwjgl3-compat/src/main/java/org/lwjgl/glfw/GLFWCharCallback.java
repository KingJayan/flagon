package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWCharCallback extends Callback implements GLFWCharCallbackI {

    public static GLFWCharCallback create(GLFWCharCallbackI delegate) {
        return delegate instanceof GLFWCharCallback already ? already : new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                delegate.invoke(window, codepoint);
            }
        };
    }
}
