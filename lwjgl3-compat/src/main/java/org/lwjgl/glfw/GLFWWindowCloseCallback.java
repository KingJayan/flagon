package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWWindowCloseCallback extends Callback implements GLFWWindowCloseCallbackI {

    public static GLFWWindowCloseCallback create(GLFWWindowCloseCallbackI delegate) {
        return delegate instanceof GLFWWindowCloseCallback already ? already : new GLFWWindowCloseCallback() {
            @Override
            public void invoke(long window) {
                delegate.invoke(window);
            }
        };
    }
}
