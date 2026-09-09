package dev.flagon.sandbox;

import static org.lwjgl.glfw.GLFW.*;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** renders into a texture through a framebuffer, then reads the result back. */
public final class FramebufferDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int TARGET = 64;

    @Override
    public String id() {
        return "framebuffer";
    }

    @Override
    public String description() {
        return "renders into a framebuffer's texture and reads it back";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        glfwInit();
        long window = glfwCreateWindow(WIDTH, HEIGHT, "Flagon — framebuffer", NULL, NULL);
        if (window == NULL) {
            console.info("fbo: no window");
            return;
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, TARGET, TARGET, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        int framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_TEXTURE_2D, texture, 0);

        int depth = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depth);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT16, TARGET, TARGET);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER, depth);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        console.info("fbo: status is "
                + (status == GL30.GL_FRAMEBUFFER_COMPLETE ? "complete" : "0x"
                        + Integer.toHexString(status)));
        console.info("fbo: bound " + (GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) == framebuffer));

        GL11.glViewport(0, 0, TARGET, TARGET);
        GL11.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        ByteBuffer read = ByteBuffer.allocate(4);
        read.order(ByteOrder.nativeOrder());
        GL11.glReadPixels(TARGET / 2, TARGET / 2, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, read);
        console.info("fbo: read back " + (read.get(0) & 0xFF) + "," + (read.get(1) & 0xFF)
                + "," + (read.get(2) & 0xFF));

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        console.info("fbo: back on the surface "
                + (GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) == 0));
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        GL11.glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        glfwSwapBuffers(window);

        GL30.glDeleteFramebuffers(framebuffer);
        GL30.glDeleteRenderbuffers(depth);
        GL11.glDeleteTextures(texture);
        glfwDestroyWindow(window);
        glfwTerminate();
        console.info("fbo: done");
    }
}
