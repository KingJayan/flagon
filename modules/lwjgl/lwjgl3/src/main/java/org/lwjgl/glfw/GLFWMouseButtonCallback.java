package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWMouseButtonCallback extends Callback implements GLFWMouseButtonCallbackI {

    public static GLFWMouseButtonCallback create(GLFWMouseButtonCallbackI delegate) {
        return delegate instanceof GLFWMouseButtonCallback already ? already
                : new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                delegate.invoke(window, button, action, mods);
            }
        };
    }
}
