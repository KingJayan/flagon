package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWFramebufferSizeCallback extends Callback implements GLFWFramebufferSizeCallbackI {

    public static GLFWFramebufferSizeCallback create(GLFWFramebufferSizeCallbackI delegate) {
        return delegate instanceof GLFWFramebufferSizeCallback already ? already : new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                delegate.invoke(window, width, height);
            }
        };
    }
}
