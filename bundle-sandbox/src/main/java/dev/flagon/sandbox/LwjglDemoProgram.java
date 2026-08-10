package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 create a display, loop until it closes, present each frame
 */
public final class LwjglDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    private int panelList;

    @Override
    public String id() {
        return "lwjgl";
    }

    @Override
    public String description() {
        return "renders through the LWJGL compatibility layer";
    }

    private static int frameLimit(String[] arguments) {
        for (int i = 0; i + 1 < arguments.length; i++) {
            if (arguments[i].equals("--frames")) {
                return Integer.parseInt(arguments[i + 1]);
            }
        }
        return MAX_FRAMES;
    }

    @Override
    public void run(String[] arguments) throws Exception {
        int frameLimit = frameLimit(arguments);

        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — LWJGL demo");
        Display.create();

        Platforms.get().console().info(
                "display created at " + Display.getWidth() + "x" + Display.getHeight());

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Platforms.get().console().info("gl " + GL11.glGetString(GL11.GL_VERSION)
                + " on " + GL11.glGetString(GL11.GL_RENDERER)
                + ", textures up to " + GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));

        buildPanelList();

        int frame = 0;
        while (!Display.isCloseRequested() && frame < frameLimit) {
            drawFrame(frame);
            Display.update();
            frame++;
            if (frame % 60 == 0) {
                Platforms.get().console().debug("frame " + frame);
            }
        }

        Platforms.get().console().info("center pixel " + centerPixel());

        Platforms.get().console().info("rendered " + frame + " frames, "
                + (GL11.glGetError() == GL11.GL_NO_ERROR ? "no gl errors" : "gl errors were raised"));
        Display.destroy();
    }

    // built once and replayed every frame, which is what a display list is for
    private void buildPanelList() {
        panelList = GL11.glGenLists(1);
        GL11.glNewList(panelList, GL11.GL_COMPILE);
        quads(200.0f, 100.0f, 280.0f, 140.0f, 0.0f, 0.8f, 0.8f);
        GL11.glEndList();
    }

    private void drawFrame(int frame) {
        GL11.glClearColor(0.10f, 0.15f, 0.30f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        quads(WIDTH * 0.25f, HEIGHT * 0.25f, WIDTH * 0.75f, HEIGHT * 0.75f, 0.0f, 0.8f, 0.2f);

        float travel = WIDTH - 40.0f;
        float x = (frame * 4.0f) % travel;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, HEIGHT - 30.0f, 0.0f);
        triangleStrip(0.0f, 0.0f, 20.0f, 20.0f, 0.9f, 0.2f, 0.1f);
        GL11.glPopMatrix();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(500, 300, 100, 40);
        quads(0.0f, 0.0f, WIDTH, HEIGHT, 0.9f, 0.9f, 0.1f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CW);
        quads(20.0f, 20.0f, 80.0f, 60.0f, 0.9f, 0.0f, 0.9f);
        GL11.glFrontFace(GL11.GL_CCW);
        quads(100.0f, 20.0f, 160.0f, 60.0f, 0.9f, 0.0f, 0.9f);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glCallList(panelList);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(0, 0, 1, 1);
        quads(300.0f, 300.0f, 340.0f, 320.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopAttrib();
        quads(300.0f, 300.0f, 340.0f, 320.0f, 0.2f, 0.4f, 1.0f);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
        quads(400.0f, 200.0f, 440.0f, 240.0f, 1.0f, 1.0f, 1.0f);
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        quads(380.0f, 180.0f, 460.0f, 260.0f, 1.0f, 0.4f, 0.0f);
        GL11.glDisable(GL11.GL_STENCIL_TEST);

        // the same translation glTranslatef would make, supplied as a matrix
        GL11.glPushMatrix();
        GL11.glLoadMatrix(translation(560.0f, 100.0f));
        quads(0.0f, 0.0f, 30.0f, 30.0f, 0.9f, 0.5f, 0.0f);
        GL11.glPopMatrix();

        drawLitQuad();
        drawFoggedQuad();
        drawBlendedQuad();
        drawCapabilityQuad();
        drawProjectedQuad();
    }

    // green only when a point survives the round trip out to the window and back through glu
    private void drawProjectedQuad() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluPerspective(60.0f, WIDTH / HEIGHT, 1.0f, 100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GLU.gluLookAt(0.0f, 0.0f, 8.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        FloatBuffer modelView = FloatBuffer.wrap(new float[16]);
        FloatBuffer projection = FloatBuffer.wrap(new float[16]);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        IntBuffer viewport = IntBuffer.wrap(new int[4]);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        FloatBuffer window = FloatBuffer.wrap(new float[3]);
        FloatBuffer object = FloatBuffer.wrap(new float[3]);
        boolean roundTrip = GLU.gluProject(2.0f, -1.0f, 3.0f, modelView, projection,
                viewport, window)
                && GLU.gluUnProject(window.get(0), window.get(1), window.get(2),
                        modelView, projection, viewport, object)
                && near(object.get(0), 2.0f) && near(object.get(1), -1.0f)
                && near(object.get(2), 3.0f);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        quads(600.0f, 150.0f, 640.0f, 190.0f, roundTrip ? 0.0f : 1.0f, roundTrip ? 0.6f : 0.0f,
                roundTrip ? 0.8f : 0.0f);
    }

    private static boolean near(float value, float expected) {
        return Math.abs(value - expected) < 0.01f;
    }

    // green only when the context reports the versions and the core extensions it is built on
    private void drawCapabilityQuad() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        boolean whole = capabilities.OpenGL11 && capabilities.OpenGL12 && capabilities.OpenGL13
                && capabilities.OpenGL14 && capabilities.OpenGL15 && capabilities.OpenGL20
                && capabilities.GL_ARB_multitexture && capabilities.GL_ARB_vertex_buffer_object
                && !capabilities.OpenGL30 && !capabilities.isSupported("GL_NV_make_believe");
        quads(600.0f, 100.0f, 640.0f, 140.0f, whole ? 0.0f : 1.0f, whole ? 1.0f : 0.0f, 0.0f);
    }

    // a constant blend color half mixed with a white quad, subtracted from the panel under it
    private void drawBlendedQuad() {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendColor(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glBlendFunc(GL14.GL_CONSTANT_COLOR, GL11.GL_ZERO);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        quads(500.0f, 100.0f, 540.0f, 140.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawLitQuad() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, color(0.0f, 0.0f, 0.0f));
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, color(0.0f, 0.0f, 1.0f, 0.0f));
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, color(0.0f, 0.0f, 0.0f));
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, color(0.4f, 0.2f, 0.8f));
        GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        quads(20.0f, 260.0f, 80.0f, 300.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    private void drawFoggedQuad() {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, 0.0f);
        GL11.glFogf(GL11.GL_FOG_END, 1000.0f);
        GL11.glFog(GL11.GL_FOG_COLOR, color(0.0f, 0.0f, 0.0f));
        quads(90.0f, 260.0f, 140.0f, 300.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_FOG);
    }

    private static FloatBuffer color(float red, float green, float blue) {
        return color(red, green, blue, 1.0f);
    }

    private static FloatBuffer color(float x, float y, float z, float w) {
        return FloatBuffer.wrap(new float[] {x, y, z, w});
    }

    private static FloatBuffer translation(float x, float y) {
        float[] matrix = new float[16];
        matrix[0] = 1.0f;
        matrix[5] = 1.0f;
        matrix[10] = 1.0f;
        matrix[15] = 1.0f;
        matrix[12] = x;
        matrix[13] = y;
        return FloatBuffer.wrap(matrix);
    }

    /** gl reads the framebuffer from the bottom left, so this is the middle of the green panel. */
    private static String centerPixel() {
        ByteBuffer pixel = ByteBuffer.allocate(4);
        GL11.glReadPixels(320, 180, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        return (pixel.get(0) & 0xFF) + "," + (pixel.get(1) & 0xFF) + "," + (pixel.get(2) & 0xFF);
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
