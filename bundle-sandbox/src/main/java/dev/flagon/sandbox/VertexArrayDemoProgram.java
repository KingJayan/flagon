package dev.flagon.sandbox;

import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 a static vbo with an interleaved byte format, an index buffer, and a client-memory array / frame
 */
public final class VertexArrayDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int MAX_FRAMES = 900;

    /** xyz as floats then rgba as bytes, the layout a classic vertex format uses. */
    private static final int STRIDE = 3 * Float.BYTES + 4;
    private static final int COLOR_OFFSET = 3 * Float.BYTES;

    private int vertexBuffer;
    private int indexBuffer;
    private FloatBuffer clientVertices;

    @Override
    public String id() {
        return "arrays";
    }

    @Override
    public String description() {
        return "renders through vertex arrays and buffer objects";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — vertex arrays");
        Display.create();

        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        buildStaticQuad();
        clientVertices = BufferUtils.createFloatBuffer(4 * 3);
        Platforms.get().console().info("vertex buffer " + vertexBuffer
                + ", index buffer " + indexBuffer);

        int frame = 0;
        while (!Display.isCloseRequested() && frame < MAX_FRAMES) {
            drawFrame(frame);
            Display.update();
            frame++;
        }

        GL15.glDeleteBuffers(vertexBuffer);
        GL15.glDeleteBuffers(indexBuffer);
        Platforms.get().console().info("rendered " + frame + " frames");
        Display.destroy();
    }

    /** a green quad in a vbo, drawn as two indexed triangles out of an element buffer. */
    private void buildStaticQuad() {
        float left = WIDTH * 0.10f;
        float right = WIDTH * 0.45f;
        float top = HEIGHT * 0.25f;
        float bottom = HEIGHT * 0.75f;

        ByteBuffer vertices = BufferUtils.createByteBuffer(4 * STRIDE);
        putVertex(vertices, left, top);
        putVertex(vertices, left, bottom);
        putVertex(vertices, right, bottom);
        putVertex(vertices, right, top);
        vertices.flip();

        IntBuffer indices = BufferUtils.createIntBuffer(6);
        indices.put(new int[] {0, 1, 2, 0, 2, 3}).flip();

        vertexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        indexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
    }

    private static void putVertex(ByteBuffer target, float x, float y) {
        target.putFloat(x).putFloat(y).putFloat(0.0f);
        target.put((byte) 0).put((byte) 204).put((byte) 51).put((byte) 255);
    }

    private void drawFrame(int frame) {
        GL11.glClearColor(0.10f, 0.15f, 0.30f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        drawStaticQuad();
        drawClientQuad(frame);
    }

    private void drawStaticQuad() {
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

    /**
     * the same pointer every frame over a buffer whose contents change, and no colour array at
     * all — so the quad takes the colour from glColor, as it would on the desktop.
     */
    private void drawClientQuad(int frame) {
        float left = WIDTH * 0.55f;
        float right = WIDTH * 0.90f;
        float top = HEIGHT * 0.25f;
        float bottom = top + (HEIGHT * 0.50f) * (0.5f + 0.5f * (frame % 120) / 119.0f);

        clientVertices.clear();
        clientVertices.put(left).put(top).put(0.0f);
        clientVertices.put(left).put(bottom).put(0.0f);
        clientVertices.put(right).put(bottom).put(0.0f);
        clientVertices.put(right).put(top).put(0.0f);
        clientVertices.flip();

        GL11.glColor4f(0.9f, 0.2f, 0.1f, 1.0f);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, clientVertices);

        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
    }
}
