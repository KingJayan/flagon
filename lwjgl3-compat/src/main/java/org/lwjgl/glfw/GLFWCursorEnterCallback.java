package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWCursorEnterCallback extends Callback implements GLFWCursorEnterCallbackI {

    public static GLFWCursorEnterCallback create(GLFWCursorEnterCallbackI delegate) {
        return delegate instanceof GLFWCursorEnterCallback already ? already
                : new GLFWCursorEnterCallback() {
            @Override
            public void invoke(long window, boolean entered) {
                delegate.invoke(window, entered);
            }
        };
    }
}
