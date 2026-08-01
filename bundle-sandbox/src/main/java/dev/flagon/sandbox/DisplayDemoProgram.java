package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 * the display lifecycle a desktop program expects: a paced loop, a destroy, and another display
 * opened on the same page afterwards.
 */
public final class DisplayDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int PACED_FRAMES = 30;
    private static final int SECOND_FRAMES = 5;
    private static final int RATE = 30;

    @Override
    public String id() {
        return "display";
    }

    @Override
    public String description() {
        return "paces a loop, destroys the display, and opens another one";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — display demo");
        Display.create();
        console.info("display ready " + Display.getWidth() + "x" + Display.getHeight());

        long start = Platforms.get().clock().nanoTime();
        for (int frame = 0; frame < PACED_FRAMES && !Display.isCloseRequested(); frame++) {
            paint(0.1f, 0.7f, 0.2f);
            Display.update();
            Display.sync(RATE);
        }
        long took = (Platforms.get().clock().nanoTime() - start) / 1_000_000L;
        console.info("paced " + PACED_FRAMES + " frames at " + Display.getSyncRate() + " in "
                + took + "ms");

        Display.destroy();
        console.info("display destroyed, created=" + Display.isCreated());

        Display.create();
        for (int frame = 0; frame < SECOND_FRAMES; frame++) {
            paint(0.8f, 0.1f, 0.1f);
            Display.update();
        }
        console.info("display again " + Display.getWidth() + "x" + Display.getHeight()
                + " closing=" + Display.isCloseRequested());
    }

    private static void paint(float red, float green, float blue) {
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glClearColor(red, green, blue, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }
}
