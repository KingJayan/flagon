package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 create a display, loop until it closes, present each frame
 */
public final class LwjglDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    @Override
    public String id() {
        return "lwjgl";
    }

    @Override
    public String description() {
        return "renders through the LWJGL compatibility layer";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — LWJGL demo");
        Display.create();

        Platforms.get().console().info(
                "display created at " + Display.getWidth() + "x" + Display.getHeight());

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame(frame);
            Display.update();
            frame++;
            if (frame % 60 == 0) {
                Platforms.get().console().debug("frame " + frame);
            }
        }

        Platforms.get().console().info("rendered " + frame + " frames");
        Display.destroy();
    }

    private void drawFrame(int frame) {
        GL11.glClearColor(0.10f, 0.15f, 0.30f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // a fixed green panel, so a screenshot has something stable to assert on. drawn with
        // GL_QUADS, which webgl has no equivalent for, so the index expansion is covered too.
        quads(WIDTH * 0.25f, HEIGHT * 0.25f, WIDTH * 0.75f, HEIGHT * 0.75f, 0.0f, 0.8f, 0.2f);

        // and a marker that moves, so the frame loop is visibly running
        float travel = WIDTH - 40.0f;
        float x = (frame * 4.0f) % travel;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, HEIGHT - 30.0f, 0.0f);
        triangleStrip(0.0f, 0.0f, 20.0f, 20.0f, 0.9f, 0.2f, 0.1f);
        GL11.glPopMatrix();
    }

    private void quads(float left, float top, float right, float bottom,
                       float red, float green, float blue) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(red, green, blue);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
    }

    private void triangleStrip(float left, float top, float right, float bottom,
                               float red, float green, float blue) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor3f(red, green, blue);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(right, bottom);
        GL11.glEnd();
    }
}
