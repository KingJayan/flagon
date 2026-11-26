package dev.flagon.sandbox;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL11;

/** the window and loop a 1.13+ client opens: glfw hints, a window, callbacks and a swap */
public final class GlfwDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int FRAMES = 30;

    @Override
    public String id() {
        return "glfw";
    }

    @Override
    public String description() {
        return "opens a window through GLFW and runs its loop";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();

        console.info("glfw: init " + glfwInit());
        glfwSetErrorCallback((error, description) ->
                console.info("glfw: error " + error));

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        long window = glfwCreateWindow(WIDTH, HEIGHT, "Flagon — glfw demo", NULL, NULL);
        if (window == NULL) {
            console.info("glfw: no window");
            return;
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        console.info("glfw: framebuffer " + width[0] + "x" + height[0]);

        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        console.info("glfw: monitor " + mode.width() + "x" + mode.height()
                + " at " + mode.refreshRate() + "Hz");

        int[] keys = new int[1];
        glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> keys[0]++);
        glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> keys[0]++);
        glfwSetFramebufferSizeCallback(window, (handle, w, h) ->
                console.info("glfw: resized to " + w + "x" + h));

        GL11.glClearColor(0.1f, 0.2f, 0.5f, 1.0f);
        int frames = 0;
        while (frames < FRAMES && !glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            glfwSwapBuffers(window);
            glfwPollEvents();
            frames++;
        }
        console.info("glfw: drew " + frames + " frames, saw " + keys[0] + " input events");
        console.info("glfw: escape is " + (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS
                ? "down" : "up"));
        console.info("glfw: the clock reached " + (glfwGetTime() > 0.0 ? "forward" : "nowhere"));

        glfwDestroyWindow(window);
        glfwTerminate();
        console.info("glfw: done");
    }
}
