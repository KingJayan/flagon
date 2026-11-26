package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWWindowSizeCallback extends Callback implements GLFWWindowSizeCallbackI {

    public static GLFWWindowSizeCallback create(GLFWWindowSizeCallbackI delegate) {
        return delegate instanceof GLFWWindowSizeCallback already ? already
                : new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                delegate.invoke(window, width, height);
            }
        };
    }
}
