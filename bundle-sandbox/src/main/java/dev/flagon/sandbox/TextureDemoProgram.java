package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 uploads a texture whose texels are known, then draws it back at known places
 */
public final class TextureDemoProgram implements Program {

    private static final int WIDTH = 940;
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
    private int tiled;
    private int mipmapped;
    private int red;
    private int grey;
    private int rgb;
    private int luminance;

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
        tiled = upload(2, 2, TEXELS);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        mipmapped = uploadMipmapped();
        red = upload(1, 1, new int[] {0xFFFF0000});
        grey = upload(1, 1, new int[] {0xFF808080});
        rgb = uploadBytes(1, 1, 0x1907, new byte[] {0x20, (byte) 0xA0, 0x40});
        luminance = uploadBytes(1, 1, GL11.GL_LUMINANCE, new byte[] {(byte) 0x80});
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
        GL11.glDeleteTextures(tiled);
        GL11.glDeleteTextures(mipmapped);
        GL11.glDeleteTextures(red);
        GL11.glDeleteTextures(grey);
        GL11.glDeleteTextures(rgb);
        GL11.glDeleteTextures(luminance);
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

    private static int uploadBytes(int width, int height, int format, byte[] pixels) {
        int name = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, name);
        java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(pixels.length);
        buffer.put(pixels).flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0,
                format, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        return name;
    }

    /**
     the base level is red and every level below it is blue, so which level was sampled is
     visible in the colour rather than in the sharpness
     */
    private static int uploadMipmapped() {
        int name = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, name);
        for (int level = 0, size = 64; size >= 1; level++, size /= 2) {
            int[] pixels = new int[size * size];
            java.util.Arrays.fill(pixels, level == 0 ? 0xFFFF0000 : 0xFF0000FF);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, GL11.GL_RGBA, size, size, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_NEAREST_MIPMAP_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAX_LEVEL, 6);
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

        // texture coordinates past 1, so the wrap mode decides what is sampled
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tiled);
        tiledQuad(620.0f, 40.0f, 700.0f, 320.0f, 2.0f);

        // one to one, so the base level is sampled; then minified, so a lower level is
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mipmapped);
        texturedQuad(720.0f, 40.0f, 784.0f, 104.0f);
        texturedQuad(720.0f, 150.0f, 728.0f, 158.0f);

        multiTextured();

        // uploaded three bytes and one byte a texel, widened to the rgba the device takes
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, rgb);
        texturedQuad(800.0f, 150.0f, 860.0f, 210.0f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, luminance);
        texturedQuad(870.0f, 150.0f, 930.0f, 210.0f);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
     two units at once: the second modulates the first, then replaces it, so which mode was in
     force is visible in the colour
     */
    private void multiTextured() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, grey);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, red);

        multiTexturedQuad(800.0f, 40.0f, 860.0f, 104.0f);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        multiTexturedQuad(870.0f, 40.0f, 930.0f, 104.0f);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private static void multiTexturedQuad(float left, float top, float right, float bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        corner(left, top, 0.0f, 0.0f);
        corner(left, bottom, 0.0f, 1.0f);
        corner(right, bottom, 1.0f, 1.0f);
        corner(right, top, 1.0f, 0.0f);
        GL11.glEnd();
    }

    private static void corner(float x, float y, float s, float t) {
        GL11.glTexCoord2f(s, t);
        GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, s, t);
        GL11.glVertex2f(x, y);
    }

    private static void tiledQuad(float left, float top, float right, float bottom, float times) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(left, top);
        GL11.glTexCoord2f(0.0f, times);
        GL11.glVertex2f(left, bottom);
        GL11.glTexCoord2f(times, times);
        GL11.glVertex2f(right, bottom);
        GL11.glTexCoord2f(times, 0.0f);
        GL11.glVertex2f(right, top);
        GL11.glEnd();
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
