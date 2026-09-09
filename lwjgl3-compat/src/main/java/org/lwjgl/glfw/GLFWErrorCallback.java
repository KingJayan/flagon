package org.lwjgl.glfw;

import java.io.PrintStream;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

public abstract class GLFWErrorCallback extends Callback implements GLFWErrorCallbackI {

    public static GLFWErrorCallback createPrint(PrintStream out) {
        return create((error, description) -> out.println("GLFW error " + error + ": "
                + MemoryUtil.memUTF8Safe(description)));
    }

    public static GLFWErrorCallback createThrow() {
        return create((error, description) -> {
            throw new IllegalStateException("GLFW error " + error + ": "
                    + MemoryUtil.memUTF8Safe(description));
        });
    }

    public GLFWErrorCallback set() {
        return GLFW.glfwSetErrorCallback(this);
    }

    public static GLFWErrorCallback create(GLFWErrorCallbackI delegate) {
        return delegate instanceof GLFWErrorCallback already ? already : new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                delegate.invoke(error, description);
            }
        };
    }
}
