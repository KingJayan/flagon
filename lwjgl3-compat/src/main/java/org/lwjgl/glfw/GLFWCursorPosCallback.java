package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWCursorPosCallback extends Callback implements GLFWCursorPosCallbackI {

    public static GLFWCursorPosCallback create(GLFWCursorPosCallbackI delegate) {
        return delegate instanceof GLFWCursorPosCallback already ? already : new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                delegate.invoke(window, x, y);
            }
        };
    }
}
