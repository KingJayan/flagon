package org.lwjgl.glfw;

@FunctionalInterface
public interface GLFWCursorPosCallbackI {

    void invoke(long window, double x, double y);
}
