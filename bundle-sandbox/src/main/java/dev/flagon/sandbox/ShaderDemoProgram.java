package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 desktop glsl 1.20, its own attributes and uniforms
 */
public final class ShaderDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    private static final int POSITION = 0;
    private static final int TINT = 1;

    /** xy as floats, then rgba as bytes: the same interleaving a desktop program would use. */
    private static final int STRIDE = 2 * Float.BYTES + 4;
    private static final int TINT_OFFSET = 2 * Float.BYTES;

    private static final String VERTEX_SHADER = """
            #version 120
            /* the desktop original was #version 330 core and read
               gl_ModelViewProjectionMatrix * gl_Vertex */
            attribute vec2 in_position;
            attribute vec3 in_tint;

            uniform vec2 u_scale;

            varying vec3 v_tint;

            void main() {
                v_tint = in_tint;
                gl_Position = vec4(in_position * u_scale, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 120
            // precision highp float; was declared here before, and gl_Color before that
            varying vec3 v_tint;

            uniform float u_fade;

            void main() {
                gl_FragColor = vec4(v_tint * u_fade, 1.0);
            }
            """;

    private int program;
    private int buffer;
    private int scaleUniform;
    private int fadeUniform;
    private FloatBuffer scale;

    @Override
    public String id() {
        return "shaders";
    }

    @Override
    public String description() {
        return "renders with an application-supplied shader program";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — shaders");
        Display.create();

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        buildProgram();
        buildTriangle();
        scale = BufferUtils.createFloatBuffer(2);

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame(frame);
            Display.update();
            frame++;
        }

        GL20.glUseProgram(0);
        GL20.glDeleteProgram(program);
        GL15.glDeleteBuffers(buffer);
        Platforms.get().console().info("rendered " + frame + " frames");
        Display.destroy();
    }

    private void buildProgram() {
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex");
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment");

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        // locations chosen by the application, not by the linker
        GL20.glBindAttribLocation(program, POSITION, "in_position");
        GL20.glBindAttribLocation(program, TINT, "in_tint");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new IllegalStateException("link failed: " + GL20.glGetProgramInfoLog(program, 512));
        }
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);

        scaleUniform = GL20.glGetUniformLocation(program, "u_scale");
        fadeUniform = GL20.glGetUniformLocation(program, "u_fade");
        // a uniform the shader does not declare
        int absent = GL20.glGetUniformLocation(program, "u_notThere");
        Platforms.get().console().info("program " + program + ", u_scale at " + scaleUniform
                + ", missing uniform reports " + absent);
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

    private void buildTriangle() {
        ByteBuffer vertices = BufferUtils.createByteBuffer(3 * STRIDE);
        putVertex(vertices, 0.0f, 0.75f, 255, 64, 32);
        putVertex(vertices, -0.75f, -0.6f, 32, 255, 96);
        putVertex(vertices, 0.75f, -0.6f, 64, 96, 255);
        vertices.flip();

        buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
    }

    private static void putVertex(ByteBuffer target, float x, float y, int red, int green, int blue) {
        target.putFloat(x).putFloat(y);
        target.put((byte) red).put((byte) green).put((byte) blue).put((byte) 255);
    }

    private void drawFrame(int frame) {
        GL11.glClearColor(0.06f, 0.06f, 0.10f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(program);

        float breathe = 0.75f + 0.25f * (frame % 120) / 119.0f;
        scale.clear();
        scale.put(breathe).put(breathe).flip();
        GL20.glUniform2(scaleUniform, scale);
        GL20.glUniform1f(fadeUniform, 1.0f);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL20.glEnableVertexAttribArray(POSITION);
        GL20.glEnableVertexAttribArray(TINT);
        GL20.glVertexAttribPointer(POSITION, 2, GL11.GL_FLOAT, false, STRIDE, 0L);
        GL20.glVertexAttribPointer(TINT, 3, GL11.GL_UNSIGNED_BYTE, true, STRIDE, TINT_OFFSET);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        GL20.glDisableVertexAttribArray(POSITION);
        GL20.glDisableVertexAttribArray(TINT);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // back to the fixed-function pipeline, which draws over the top with no shader of its own
        GL20.glUseProgram(0);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(-0.98f, -0.98f);
        GL11.glVertex2f(-0.88f, -0.98f);
        GL11.glVertex2f(-0.88f, -0.88f);
        GL11.glVertex2f(-0.98f, -0.88f);
        GL11.glEnd();
    }
}
