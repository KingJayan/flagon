package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.nio.FloatBuffer;

/**
 drawn only through the GLxxC classes, which is what lwjgl 3 code imports
 */
public final class CoreClassesDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    private static final String VERTEX_SHADER = """
            #version 150 core
            in vec2 in_position;
            in vec3 in_tint;
            out vec3 v_tint;

            void main() {
                v_tint = in_tint;
                gl_Position = vec4(in_position, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 150 core
            in vec3 v_tint;
            out vec4 out_color;

            void main() {
                out_color = vec4(v_tint, 1.0);
            }
            """;

    private static final int POSITION = 0;
    private static final int TINT = 1;
    private static final int STRIDE = 5 * Float.BYTES;

    private int array;
    private int buffer;
    private int program;

    @Override
    public String id() {
        return "glcore";
    }

    @Override
    public String description() {
        return "draws through the GLxxC core-profile classes lwjgl 3 code imports";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — core-profile classes");
        Display.create();

        GL11C.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11C.glDisable(GL11C.GL_DEPTH_TEST);
        Platforms.get().console().info("GL11C reports " + GL11C.glGetString(GL11C.GL_VERSION));

        build();

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame();
            Display.update();
            frame++;
        }

        GL20C.glUseProgram(0);
        GL20C.glDeleteProgram(program);
        GL30C.glDeleteVertexArrays(array);
        GL15C.glDeleteBuffers(buffer);
        Platforms.get().console().info("the core-profile classes drew " + frame + " frames");
        Display.destroy();
    }

    private void build() {
        FloatBuffer vertices = BufferUtils.createFloatBuffer(15);
        vertices.put(new float[] {
            0.0f, 0.8f, 1.0f, 0.2f, 0.2f,
            -0.8f, -0.6f, 0.2f, 1.0f, 0.2f,
            0.8f, -0.6f, 0.2f, 0.2f, 1.0f,
        });
        vertices.flip();

        array = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(array);
        buffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);

        program = GL20C.glCreateProgram();
        GL20C.glAttachShader(program, compile(GL20C.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex"));
        GL20C.glAttachShader(program,
                compile(GL20C.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment"));
        GL20C.glBindAttribLocation(program, POSITION, "in_position");
        GL20C.glBindAttribLocation(program, TINT, "in_tint");
        GL30C.glBindFragDataLocation(program, 0, "out_color");
        GL20C.glLinkProgram(program);
        if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) != GL11C.GL_TRUE) {
            throw new IllegalStateException("link failed: "
                    + GL20C.glGetProgramInfoLog(program, 512));
        }

        GL20C.glEnableVertexAttribArray(POSITION);
        GL20C.glVertexAttribPointer(POSITION, 2, GL11C.GL_FLOAT, false, STRIDE, 0L);
        GL20C.glEnableVertexAttribArray(TINT);
        GL20C.glVertexAttribPointer(TINT, 3, GL11C.GL_FLOAT, false, STRIDE, 2L * Float.BYTES);
        GL30C.glBindVertexArray(0);
        Platforms.get().console().info("built a core-profile program through GL20C and GL30C");
    }

    private static int compile(int type, String source, String stage) {
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, source);
        GL20C.glCompileShader(shader);
        if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) != GL11C.GL_TRUE) {
            throw new IllegalStateException(
                    stage + " shader failed: " + GL20C.glGetShaderInfoLog(shader, 512));
        }
        return shader;
    }

    private void drawFrame() {
        GL11C.glClearColor(0.04f, 0.04f, 0.09f, 1.0f);
        GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
        GL20C.glUseProgram(program);
        GL30C.glBindVertexArray(array);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
        GL30C.glBindVertexArray(0);
    }
}
