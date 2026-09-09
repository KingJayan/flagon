package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWWindowFocusCallback extends Callback implements GLFWWindowFocusCallbackI {

    public static GLFWWindowFocusCallback create(GLFWWindowFocusCallbackI delegate) {
        return delegate instanceof GLFWWindowFocusCallback already ? already : new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focused) {
                delegate.invoke(window, focused);
            }
        };
    }
}
