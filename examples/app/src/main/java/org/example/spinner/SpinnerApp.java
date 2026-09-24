package org.example.spinner;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * a spinning square with a keyboard-driven speed and a mouse-driven hue
 */
public final class SpinnerApp {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;

    private static final int STRIDE = 3 * Float.BYTES + 4;
    private static final int COLOR_OFFSET = 3 * Float.BYTES;
    private static final float SIZE = 70.0f;

    private int vertexBuffer;
    private int indexBuffer;
    private float angle;
    private float speed = 1.5f;

    public static void main(String[] arguments) throws Exception {
        new SpinnerApp().run(frameLimit(arguments));
    }

    private static int frameLimit(String[] arguments) {
        for (int i = 0; i + 1 < arguments.length; i++) {
            if (arguments[i].equals("--frames")) {
                return Integer.parseInt(arguments[i + 1]);
            }
        }
        return Integer.MAX_VALUE;
    }

    private void run(int frameLimit) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Spinner");
        Display.create();
        Keyboard.create();

        System.out.println("spinner: " + Display.getWidth() + "x" + Display.getHeight());

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buildSquare();

        int frame = 0;
        while (!Display.isCloseRequested() && frame < frameLimit) {
            readInput();
            render();
            Display.update();
            Display.sync(60);
            frame++;
        }

        System.out.println("spinner: " + frame + " frames");
        GL15.glDeleteBuffers(vertexBuffer);
        GL15.glDeleteBuffers(indexBuffer);
        Display.destroy();
    }

    private void buildSquare() {
        ByteBuffer vertices = BufferUtils.createByteBuffer(4 * STRIDE);
        corner(vertices, -SIZE, -SIZE, 230, 70, 60);
        corner(vertices, -SIZE, SIZE, 240, 190, 60);
        corner(vertices, SIZE, SIZE, 60, 200, 120);
        corner(vertices, SIZE, -SIZE, 70, 130, 230);
        vertices.flip();

        IntBuffer indices = BufferUtils.createIntBuffer(6);
        indices.put(new int[] {0, 1, 2, 0, 2, 3});
        indices.flip();

        vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        indexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
    }

    private static void corner(ByteBuffer target, float x, float y, int red, int green, int blue) {
        target.putFloat(x).putFloat(y).putFloat(0.0f);
        target.put((byte) red).put((byte) green).put((byte) blue).put((byte) 255);
    }

    private void readInput() {
        while (Keyboard.next()) {
            if (!Keyboard.getEventKeyState()) {
                continue;
            }
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_UP) {
                speed = Math.min(speed + 0.5f, 8.0f);
            } else if (key == Keyboard.KEY_DOWN) {
                speed = Math.max(speed - 0.5f, 0.0f);
            } else if (key == Keyboard.KEY_ESCAPE) {
                Display.destroy();
                return;
            }
        }
        angle += speed;
    }

    private void render() {
        GL11.glClearColor(0.07f, 0.08f, 0.12f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glPushMatrix();
        GL11.glTranslatef(WIDTH / 2.0f, HEIGHT / 2.0f, 0.0f);
        GL11.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        drawSquare();
        GL11.glPopMatrix();

        drawCursorMark();
    }

    private void drawSquare() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0L);
        GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, COLOR_OFFSET);

        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0L);

        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void drawCursorMark() {
        float x = Mouse.getX();
        float y = HEIGHT - Mouse.getY();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.85f);
        GL11.glVertex2f(x - 4.0f, y - 4.0f);
        GL11.glVertex2f(x - 4.0f, y + 4.0f);
        GL11.glVertex2f(x + 4.0f, y + 4.0f);
        GL11.glVertex2f(x + 4.0f, y - 4.0f);
        GL11.glEnd();
    }
}
