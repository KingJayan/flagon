package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 uploads a texture whose texels are known, then draws it back at known places
 */
public final class TextureDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    /** 0xAARRGGBB, the order glTexImage2D takes. row 0 is the top of the quad. */
    private static final int[] TEXELS = {
            0xFFFF0000, 0xFF00FF00,
            0xFF0000FF, 0xFF000000,
    };

    /** the bottom right texel, patched in afterwards rather than uploaded with the rest */
    private static final int[] CORNER = {0xFFFFFF00};

    /** half-opaque white, so the alpha byte has to land in the right place to blend. */
    private static final int[] FADED = {0x80FFFFFF};

    private int texture;
    private int faded;

    @Override
    public String id() {
        return "texture";
    }

    @Override
    public String description() {
        return "uploads and samples a texture";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — texture");
        Display.create();

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        texture = upload(2, 2, TEXELS);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 1, 1, 1, 1,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, CORNER);
        faded = upload(1, 1, FADED);
        Platforms.get().console().info("uploaded texture " + texture + " (2x2) and " + faded
                + " (1x1)");

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame();
            Display.update();
            frame++;
        }

        GL11.glDeleteTextures(texture);
        GL11.glDeleteTextures(faded);
        Platforms.get().console().info("rendered " + frame + " frames");
        Display.destroy();
    }

    private static int upload(int width, int height, int[] pixels) {
        int name = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, name);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        // nearest, so every screen pixel of the quad is exactly one texel
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        return name;
    }

    private void drawFrame() {
        GL11.glClearColor(0.06f, 0.06f, 0.10f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        texturedQuad(40.0f, 40.0f, 360.0f, 320.0f);

        // the same texels tinted, so the sampler feeds the fixed-function color too
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        texturedQuad(400.0f, 40.0f, 480.0f, 320.0f);

        // half-opaque white over the clear color
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, faded);
        texturedQuad(520.0f, 40.0f, 600.0f, 320.0f);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static void texturedQuad(float left, float top, float right, float bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(left, top);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(left, bottom);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(right, bottom);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
    }
}
