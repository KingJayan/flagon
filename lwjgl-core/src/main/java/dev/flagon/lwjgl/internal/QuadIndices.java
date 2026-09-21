package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

/**
 * the element buffer that turns quads into triangle pairs
 */
final class QuadIndices {

    private final GraphicsDevice device;
    private final int buffer;

    private int[] indices = new int[0];
    private int uploadedQuads;

    QuadIndices(GraphicsDevice device) {
        this.device = device;
        this.buffer = device.createBuffer();
    }

    /** the indices always count from zero; a draw reaches its own vertices through the offset */
    void bind(int quads) {
        device.bindBuffer(BufferTarget.ELEMENT, buffer);
        if (quads <= uploadedQuads) {
            return;
        }
        indices = new int[quads * DrawMode.QUAD_INDICES];
        write(indices, quads);
        device.bufferData(BufferTarget.ELEMENT, indices, indices.length, BufferUsage.STATIC);
        uploadedQuads = quads;
    }

    static void write(int[] indices, int quads) {
        for (int quad = 0, out = 0; quad < quads; quad++) {
            int base = quad * DrawMode.QUAD_VERTICES;
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base;
            indices[out++] = base + 2;
            indices[out++] = base + 3;
        }
    }

    void dispose() {
        device.deleteBuffer(buffer);
    }
}
