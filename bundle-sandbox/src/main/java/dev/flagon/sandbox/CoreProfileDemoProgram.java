package dev.flagon.sandbox;

import static org.lwjgl.glfw.GLFW.*;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

/** a 3.2 core profile frame: a vertex array, an integer attribute and an instanced draw. */
public final class CoreProfileDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int FRAMES = 10;
    private static final int INSTANCES = 3;

    private static final int POSITION = 0;
    private static final int INSTANCE = 1;

    private static final String VERTEX_SHADER = """
            #version 300 es
            in vec2 in_position;
            in int in_instance;

            out vec3 v_tint;

            void main() {
                float id = float(in_instance);
                v_tint = vec3(id * 0.25, 0.4, 1.0 - id * 0.25);
                gl_Position = vec4(in_position + vec2(id * 0.5 - 0.5, 0.0), 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec3 v_tint;

            out vec4 fragColor;

            void main() {
                fragColor = vec4(v_tint, 1.0);
            }
            """;

    @Override
    public String id() {
        return "core-profile";
    }

    @Override
    public String description() {
        return "draws instanced triangles through a GL 3.2 core profile context";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        glfwInit();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long window = glfwCreateWindow(WIDTH, HEIGHT, "Flagon — core profile", NULL, NULL);
        if (window == NULL) {
            console.info("core: no window");
            return;
        }
        glfwMakeContextCurrent(window);

        GLCapabilities capabilities = GL.createCapabilities();
        console.info("core: profile " + GL30.glGetInteger(GL30.GL_MAJOR_VERSION) + "."
                + GL30.glGetInteger(GL30.GL_MINOR_VERSION)
                + ", 3.3 entry points " + capabilities.OpenGL33);
        console.info("core: " + GL30.glGetInteger(GL30.GL_NUM_EXTENSIONS) + " extensions");

        int array = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(array);
        console.info("core: vertex array bound "
                + (GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING) == array));

        int program = link();
        GL20.glUseProgram(program);

        int positions = triangle();
        int instances = instanceIds();

        GL11.glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
        int frame = 0;
        while (frame < FRAMES && !glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 3, INSTANCES);
            glfwSwapBuffers(window);
            glfwPollEvents();
            frame++;
        }
        console.info("core: drew " + frame + " frames of " + INSTANCES + " instances");
        console.info("core: base vertex is " + refusedBaseVertex());

        GL20.glUseProgram(0);
        GL20.glDeleteProgram(program);
        GL15.glDeleteBuffers(positions);
        GL15.glDeleteBuffers(instances);
        GL30.glDeleteVertexArrays(array);
        glfwDestroyWindow(window);
        glfwTerminate();
        console.info("core: done");
    }

    private static String refusedBaseVertex() {
        try {
            org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, 3,
                    GL11.GL_UNSIGNED_SHORT, 0L, 1);
            return "honoured";
        } catch (UnsupportedOperationException refused) {
            return "refused";
        }
    }

    private static int link() {
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glBindAttribLocation(program, POSITION, "in_position");
        GL20.glBindAttribLocation(program, INSTANCE, "in_instance");
        GL30.glBindFragDataLocation(program, 0, "fragColor");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException("link failed: " + GL20.glGetProgramInfoLog(program, 512));
        }
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        return program;
    }

    private static int compile(int stage, String source) {
        int shader = GL20.glCreateShader(stage);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException("compile failed: " + GL20.glGetShaderInfoLog(shader, 512));
        }
        return shader;
    }

    private static int triangle() {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
                    stack.floats(-0.2f, -0.3f, 0.2f, -0.3f, 0.0f, 0.3f), GL15.GL_STATIC_DRAW);
        }
        GL20.glEnableVertexAttribArray(POSITION);
        GL20.glVertexAttribPointer(POSITION, 2, GL11.GL_FLOAT, false, 0, 0L);
        return buffer;
    }

    /** one integer per instance, read as an integer and stepped once per instance. */
    private static int instanceIds() {
        int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        ByteBuffer ids = ByteBuffer.allocate(INSTANCES * Integer.BYTES);
        ids.order(ByteOrder.nativeOrder());
        for (int instance = 0; instance < INSTANCES; instance++) {
            ids.putInt(instance * Integer.BYTES, instance);
        }
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, ids, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(INSTANCE);
        GL30.glVertexAttribIPointer(INSTANCE, 1, GL11.GL_INT, Integer.BYTES, 0L);
        GL33.glVertexAttribDivisor(INSTANCE, 1);
        return buffer;
    }
}
