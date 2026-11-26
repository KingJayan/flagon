package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWScrollCallback extends Callback implements GLFWScrollCallbackI {

    public static GLFWScrollCallback create(GLFWScrollCallbackI delegate) {
        return delegate instanceof GLFWScrollCallback already ? already : new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                delegate.invoke(window, xoffset, yoffset);
            }
        };
    }
}
