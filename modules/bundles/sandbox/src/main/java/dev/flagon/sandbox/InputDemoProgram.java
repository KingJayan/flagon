package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 * drains the input queues the way a desktop game does: poll every event each frame, then read
 * held state for anything continuous.
 */
public final class InputDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 3600;
    private static final int MARKER = 24;

    @Override
    public String id() {
        return "input";
    }

    @Override
    public String description() {
        return "reports keyboard and mouse through the LWJGL compatibility layer";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — input demo");
        Display.create();
        Keyboard.create();
        Mouse.create();

        ConsoleSink console = Platforms.get().console();
        console.info("input ready");

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drainEvents(console);
            drawFrame();
            Display.update();
            frame++;
        }

        Display.destroy();
    }

    private void drainEvents(ConsoleSink console) {
        while (Keyboard.next()) {
            String name = Keyboard.getKeyName(Keyboard.getEventKey());
            if (Keyboard.getEventKeyState()) {
                console.info("key down " + name + " char="
                        + describe(Keyboard.getEventCharacter()));
            } else {
                console.info("key up " + name);
            }
        }

        while (Mouse.next()) {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0) {
                console.info("wheel " + wheel);
            }
            int button = Mouse.getEventButton();
            if (button >= 0) {
                console.info("button " + button
                        + (Mouse.getEventButtonState() ? " down at " : " up at ")
                        + Mouse.getEventX() + "," + Mouse.getEventY());
            }
        }
    }

    private static String describe(char character) {
        return character == Keyboard.CHAR_NONE ? "none" : String.valueOf(character);
    }

    private void drawFrame() {
        // held keys tint the background, which is state rather than events
        boolean held = Keyboard.isKeyDown(Keyboard.KEY_W);
        GL11.glClearColor(held ? 0.70f : 0.10f, 0.15f, 0.30f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        float x = Mouse.getX();
        float y = HEIGHT - Mouse.getY();
        boolean pressed = Mouse.isButtonDown(0);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(pressed ? 1.0f : 0.0f, 0.8f, 0.2f);
        GL11.glVertex2f(x - MARKER, y - MARKER);
        GL11.glVertex2f(x - MARKER, y + MARKER);
        GL11.glVertex2f(x + MARKER, y + MARKER);
        GL11.glVertex2f(x + MARKER, y - MARKER);
        GL11.glEnd();
    }
}
