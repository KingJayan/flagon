package org.lwjgl.glfw;

/** the callback housekeeping every lwjgl 3 program does around a window. */
public final class Callbacks {

    private Callbacks() {
    }

    /**
     * drops every callback set on a window. the callbacks here are ordinary objects rather than
     * native closures, so this clears them rather than freeing anything.
     */
    public static void glfwFreeCallbacks(long window) {
        GLFW.glfwSetKeyCallback(window, null);
        GLFW.glfwSetCharCallback(window, null);
        GLFW.glfwSetCharModsCallback(window, null);
        GLFW.glfwSetMouseButtonCallback(window, null);
        GLFW.glfwSetCursorPosCallback(window, null);
        GLFW.glfwSetCursorEnterCallback(window, null);
        GLFW.glfwSetScrollCallback(window, null);
        GLFW.glfwSetWindowSizeCallback(window, null);
        GLFW.glfwSetWindowPosCallback(window, null);
        GLFW.glfwSetWindowFocusCallback(window, null);
        GLFW.glfwSetWindowCloseCallback(window, null);
        GLFW.glfwSetFramebufferSizeCallback(window, null);
    }
}
