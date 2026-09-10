package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 two programs, each supplying one stage and leaving the other to gl's fixed-function pipeline
 */
public final class HalfShaderProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    /** no fragment shader: gl shades this with the primary colour the vertex stage wrote */
    private static final String VERTEX_ONLY = """
            #version 120
            void main() {
                gl_FrontColor = gl_Color;
                gl_Position = ftransform();
            }
            """;

    /** no vertex shader: gl transforms and interpolates, this only decides the colour */
    private static final String FRAGMENT_ONLY = """
            #version 120
            void main() {
                gl_FragColor = vec4(gl_Color.bgr, 1.0);
            }
            """;

    private int vertexOnly;
    private int fragmentOnly;

    @Override
    public String id() {
        return "halfshader";
    }

    @Override
    public String description() {
        return "links programs that supply one shader stage and leave the other fixed-function";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — half a shader");
        Display.create();

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        vertexOnly = link(GL20.GL_VERTEX_SHADER, VERTEX_ONLY, "vertex");
        fragmentOnly = link(GL20.GL_FRAGMENT_SHADER, FRAGMENT_ONLY, "fragment");

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame();
            Display.update();
            frame++;
        }

        GL20.glUseProgram(0);
        GL20.glDeleteProgram(vertexOnly);
        GL20.glDeleteProgram(fragmentOnly);
        Platforms.get().console().info("half-shader programs drew " + frame + " frames");
        Display.destroy();
    }

    private void drawFrame() {
        GL11.glClearColor(0.05f, 0.05f, 0.08f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(vertexOnly);
        GL11.glColor3f(0.0f, 1.0f, 0.0f);
        triangle(-0.5f);

        // red goes in, and this program's fragment stage swaps it to blue
        GL20.glUseProgram(fragmentOnly);
        GL11.glColor3f(1.0f, 0.0f, 0.0f);
        triangle(0.5f);
    }

    private static void triangle(float centre) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(centre, 0.7f, 0.0f);
        GL11.glVertex3f(centre - 0.4f, -0.6f, 0.0f);
        GL11.glVertex3f(centre + 0.4f, -0.6f, 0.0f);
        GL11.glEnd();
    }

    private static int link(int type, String source, String stage) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException(
                    stage + " shader failed: " + GL20.glGetShaderInfoLog(shader, 512));
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, shader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException(stage + "-only link failed: "
                    + GL20.glGetProgramInfoLog(program, 512));
        }
        GL20.glDeleteShader(shader);
        Platforms.get().console().info("linked a " + stage + "-only program as " + program);
        return program;
    }
}
