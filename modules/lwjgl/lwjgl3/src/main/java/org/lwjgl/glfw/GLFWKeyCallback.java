package org.lwjgl.glfw;

import org.lwjgl.system.Callback;

public abstract class GLFWKeyCallback extends Callback implements GLFWKeyCallbackI {

    public static GLFWKeyCallback create(GLFWKeyCallbackI delegate) {
        return delegate instanceof GLFWKeyCallback already ? already : new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                delegate.invoke(window, key, scancode, action, mods);
            }
        };
    }
}
