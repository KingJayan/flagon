package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWWindowPosCallback extends Callback implements GLFWWindowPosCallbackI {

    public static GLFWWindowPosCallback create(GLFWWindowPosCallbackI delegate) {
        return delegate instanceof GLFWWindowPosCallback already ? already
                : new GLFWWindowPosCallback() {
            @Override
            public void invoke(long window, int x, int y) {
                delegate.invoke(window, x, y);
            }
        };
    }
}
