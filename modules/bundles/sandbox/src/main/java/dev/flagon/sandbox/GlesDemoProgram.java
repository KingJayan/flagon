package dev.flagon.sandbox;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengles.GLES30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLESCapabilities;
import org.lwjgl.system.MemoryStack;

/** an lwjgl 3 gl es 3.0 program: the calls a mobile renderer makes, each read back. */
public final class GlesDemoProgram implements Program {

    private static final int SIZE = 64;
    private static final int FRAMES = 10;

    private static final String QUAD_VERTEX = """
            #version 300 es
            layout(location = 0) in vec2 position;
            layout(location = 1) in uint shade;
            flat out uint v_shade;
            out vec2 v_uv;
            void main() {
                v_shade = shade;
                v_uv = position * 0.5 + 0.5;
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """;

    private static final String TWO_TARGETS = """
            #version 300 es
            precision highp float;
            layout(std140) uniform Tint {
                vec4 tint;
            };
            uniform uint scale;
            flat in uint v_shade;
            layout(location = 0) out vec4 first;
            layout(location = 1) out vec4 second;
            void main() {
                first = tint * float(v_shade * scale);
                second = vec4(0.0, 1.0, 0.0, 1.0);
            }
            """;

    private static final String LAYERED = """
            #version 300 es
            precision highp float;
            precision highp sampler2DArray;
            uniform sampler2DArray layers;
            uniform float layer;
            in vec2 v_uv;
            out vec4 color;
            void main() {
                color = texture(layers, vec3(v_uv, layer));
            }
            """;

    private static final String SOLID_VERTEX = """
            #version 100
            attribute vec2 position;
            void main() {
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """;

    private static final String SOLID = """
            #version 100
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
            """;

    private static final String DOUBLING = """
            #version 300 es
            layout(location = 0) in float value;
            out float doubled;
            void main() {
                doubled = value * 2.0;
                gl_Position = vec4(0.0);
            }
            """;

    private static final String NOTHING = """
            #version 300 es
            precision mediump float;
            out vec4 color;
            void main() {
                color = vec4(0.0);
            }
            """;

    private ConsoleSink console;

    @Override
    public String id() {
        return "gles";
    }

    @Override
    public String description() {
        return "draws through lwjgl 3's gl es 3.0 api and reads the results back";
    }

    @Override
    public void run(String[] arguments) {
        console = Platforms.get().console();
        glfwInit();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        long window = glfwCreateWindow(SIZE, SIZE, "Flagon — gl es", NULL, NULL);
        glfwMakeContextCurrent(window);

        GLESCapabilities capabilities = GLES.createCapabilities();
        console.info("gles: 3.0 " + capabilities.GLES30 + ", 3.1 " + capabilities.GLES31
                + ", version " + glGetString(GL_VERSION).startsWith("OpenGL ES 3.0"));

        int quad = quad();
        int targets = twoTargets(quad);
        int layered = arrayTexture(quad);
        clientArrays();
        transformFeedback();
        query(quad);
        fence();
        blit(targets);

        int layers = glGetUniformLocation(layered, "layers");
        glUseProgram(layered);
        glUniform1i(layers, 0);
        for (int frame = 0; frame < FRAMES && !glfwWindowShouldClose(window); frame++) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, SIZE, SIZE);
            glUniform1f(glGetUniformLocation(layered, "layer"), frame % 2);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0L);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        int error = glGetError();
        console.info(error == GL_NO_ERROR ? "gles: no gl errors"
                : "gles: gl error 0x" + Integer.toHexString(error));
        glfwDestroyWindow(window);
        glfwTerminate();
        console.info("gles: done");
    }

    /** a vertex array with a full-screen quad, an integer shade and an index buffer */
    private static int quad() {
        int array = glGenVertexArrays();
        glBindVertexArray(array);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, stack.floats(-1, -1, 1, -1, 1, 1, -1, 1),
                    GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);

            glBindBuffer(GL_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ARRAY_BUFFER, stack.ints(1, 1, 1, 1), GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribIPointer(1, 1, GL_UNSIGNED_INT, 0, 0L);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, glGenBuffers());
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, stack.shorts((short) 0, (short) 1, (short) 2,
                    (short) 0, (short) 2, (short) 3), GL_STATIC_DRAW);
        }
        return array;
    }

    /** a uniform block, an unsigned uniform and two colour targets drawn at once */
    private int twoTargets(int quad) {
        int program = link(QUAD_VERTEX, TWO_TARGETS);
        glUseProgram(program);
        int block = glGetUniformBlockIndex(program, "Tint");
        glUniformBlockBinding(program, block, 0);
        int tint = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, tint);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glBufferData(GL_UNIFORM_BUFFER, stack.floats(0, 0, 1, 1), GL_STATIC_DRAW);
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, tint);
        glUniform1ui(glGetUniformLocation(program, "scale"), 1);

        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        for (int target = 0; target < 2; target++) {
            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, SIZE, SIZE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + target, GL_TEXTURE_2D,
                    texture, 0);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glDrawBuffers(stack.ints(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1));
        }
        boolean complete = glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
        glViewport(0, 0, SIZE, SIZE);
        glBindVertexArray(quad);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0L);

        glReadBuffer(GL_COLOR_ATTACHMENT0);
        String first = pixel();
        glReadBuffer(GL_COLOR_ATTACHMENT1);
        String second = pixel();
        console.info("gles: framebuffer complete " + complete + ", block " + (block == 0)
                + ", targets read " + first + " and " + second);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glClearBufferfv(GL_COLOR, 1, stack.floats(1, 1, 0, 1));
        }
        console.info("gles: cleared target reads " + pixel());
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        return framebuffer;
    }

    /** a two-layer array texture read through a sampler object */
    private int arrayTexture(int quad) {
        int texture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, texture);
        ByteBuffer texels = ByteBuffer.allocateDirect(8);
        texels.put(new byte[] {(byte) 255, 0, 0, (byte) 255, (byte) 255, 0, (byte) 255,
                (byte) 255}).flip();
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, 1, 1, 2, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                texels);
        int sampler = glGenSamplers();
        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindSampler(0, sampler);

        int program = link(QUAD_VERTEX, LAYERED);
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "layers"), 0);
        glUniform1f(glGetUniformLocation(program, "layer"), 1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindVertexArray(quad);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0L);
        console.info("gles: layer 1 reads " + pixel() + ", sampler filter nearest "
                + (glGetSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER) == GL_NEAREST));
        return program;
    }

    /** gl es 2.0 style: vertices and indices straight from client memory */
    private void clientArrays() {
        int program = link(SOLID_VERTEX, SOLID);
        glBindAttribLocation(program, 0, "position");
        glLinkProgram(program);
        glUseProgram(program);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer corners = stack.floats(-1, -1, 1, -1, 1, 1, -1, 1);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, corners);
            glDrawElements(GL_TRIANGLES, stack.shorts((short) 0, (short) 1, (short) 2,
                    (short) 0, (short) 2, (short) 3));
        }
        console.info("gles: client arrays read " + pixel());
        glDisableVertexAttribArray(0);
    }

    /** a vertex shader's output captured, then read through a mapped range */
    private void transformFeedback() {
        int program = glCreateProgram();
        glAttachShader(program, compile(GL_VERTEX_SHADER, DOUBLING));
        glAttachShader(program, compile(GL_FRAGMENT_SHADER, NOTHING));
        glTransformFeedbackVaryings(program, "doubled", GL_INTERLEAVED_ATTRIBS);
        glLinkProgram(program);
        glUseProgram(program);

        int array = glGenVertexArrays();
        glBindVertexArray(array);
        int input = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, input);
        glBufferData(GL_ARRAY_BUFFER, 12, GL_STATIC_DRAW);
        ByteBuffer written = glMapBufferRange(GL_ARRAY_BUFFER, 0, 12,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        written.asFloatBuffer().put(new float[] {1, 2, 3});
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 1, GL_FLOAT, false, 0, 0L);

        int output = glGenBuffers();
        glBindBuffer(GL_TRANSFORM_FEEDBACK_BUFFER, output);
        glBufferData(GL_TRANSFORM_FEEDBACK_BUFFER, 12, GL_STATIC_READ);
        int feedback = glGenTransformFeedbacks();
        glBindTransformFeedback(GL_TRANSFORM_FEEDBACK, feedback);
        glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0, output);
        glEnable(GL_RASTERIZER_DISCARD);
        glBeginTransformFeedback(GL_POINTS);
        glDrawArrays(GL_POINTS, 0, 3);
        glEndTransformFeedback();
        glDisable(GL_RASTERIZER_DISCARD);
        glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        glBindTransformFeedback(GL_TRANSFORM_FEEDBACK, 0);

        glBindBuffer(GL_COPY_READ_BUFFER, output);
        FloatBuffer read = glMapBufferRange(GL_COPY_READ_BUFFER, 0, 12, GL_MAP_READ_BIT)
                .asFloatBuffer();
        console.info("gles: transform feedback doubles 1 2 3 to " + read.get(0) + " "
                + read.get(1) + " " + read.get(2));
        glUnmapBuffer(GL_COPY_READ_BUFFER);
    }

    /** an occlusion query, whose answer arrives once the page has had its turn */
    private void query(int quad) {
        int program = link(QUAD_VERTEX, NOTHING);
        glUseProgram(program);
        glBindVertexArray(quad);
        int query = glGenQueries();
        glBeginQuery(GL_ANY_SAMPLES_PASSED, query);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0L);
        glEndQuery(GL_ANY_SAMPLES_PASSED);
        console.info("gles: samples passed " + glGetQueryObjectui(query, GL_QUERY_RESULT));
        glDeleteQueries(query);
    }

    private void fence() {
        long sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        int status = glClientWaitSync(sync, GL_SYNC_FLUSH_COMMANDS_BIT, 2_000_000_000L);
        console.info("gles: fence signaled "
                + (status == GL_ALREADY_SIGNALED || status == GL_CONDITION_SATISFIED));
        glDeleteSync(sync);
    }

    private void blit(int framebuffer) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, SIZE, SIZE, 0, 0, SIZE, SIZE, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        console.info("gles: blit reads " + pixel());
    }

    /** the middle pixel of the read framebuffer, as red green blue alpha */
    private static String pixel() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer rgba = stack.malloc(4);
            glReadPixels(SIZE / 2, SIZE / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
            return (rgba.get(0) & 0xFF) + " " + (rgba.get(1) & 0xFF) + " "
                    + (rgba.get(2) & 0xFF) + " " + (rgba.get(3) & 0xFF);
        }
    }

    private static int link(String vertex, String fragment) {
        int program = glCreateProgram();
        glAttachShader(program, compile(GL_VERTEX_SHADER, vertex));
        glAttachShader(program, compile(GL_FRAGMENT_SHADER, fragment));
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            throw new IllegalStateException("link failed: " + glGetProgramInfoLog(program));
        }
        return program;
    }

    private static int compile(int stage, String source) {
        int shader = glCreateShader(stage);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            throw new IllegalStateException("compile failed: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
