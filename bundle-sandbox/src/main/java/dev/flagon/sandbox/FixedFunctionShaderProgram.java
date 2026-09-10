package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 a shader written against gl's fixed-function state, drawn the way gl 1.x code draws
 */
public final class FixedFunctionShaderProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    /** the shape desktop glsl 1.20 takes: no attributes of its own, all of it gl's state */
    private static final String VERTEX_SHADER = """
            #version 120
            varying float shade;

            void main() {
                gl_FrontColor = gl_Color;
                gl_TexCoord[0] = gl_MultiTexCoord0;
                shade = clamp(gl_Vertex.y + 0.5, 0.0, 1.0);
                gl_Position = ftransform();
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 120
            varying float shade;

            void main() {
                gl_FragColor = vec4(gl_Color.rgb * shade, gl_Color.a * gl_TexCoord[0].s);
            }
            """;

    private int program;

    @Override
    public String id() {
        return "fixedfunction";
    }

    @Override
    public String description() {
        return "draws with a shader that reads gl_Vertex, gl_Color and ftransform()";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — fixed-function glsl");
        Display.create();

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        buildProgram();
        GL20.glUseProgram(program);
        // the matrices the shader reads are the ones these calls set
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame();
            Display.update();
            frame++;
        }

        GL20.glUseProgram(0);
        GL20.glDeleteProgram(program);
        Platforms.get().console().info("fixed-function shader drew " + frame + " frames");
        Display.destroy();
    }

    /** glBegin feeds a program that reads gl_Vertex, which a program of its own could not take */
    private void drawFrame() {
        GL11.glClearColor(0.05f, 0.05f, 0.08f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glColor3f(1.0f, 0.25f, 0.125f);
        GL11.glVertex3f(0.0f, 0.75f, 0.0f);
        GL11.glColor3f(0.125f, 1.0f, 0.375f);
        GL11.glVertex3f(-0.75f, -0.6f, 0.0f);
        GL11.glColor3f(0.25f, 0.375f, 1.0f);
        GL11.glVertex3f(0.75f, -0.6f, 0.0f);
        GL11.glEnd();
    }

    private void buildProgram() {
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex");
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment");

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException("link failed: "
                    + GL20.glGetProgramInfoLog(program, 512));
        }
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        Platforms.get().console().info("linked a fixed-function shader as program " + program);
    }

    private static int compile(int type, String source, String stage) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException(
                    stage + " shader failed: " + GL20.glGetShaderInfoLog(shader, 512));
        }
        return shader;
    }
}
