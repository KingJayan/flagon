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
    private int uploadedFirst = -1;

    QuadIndices(GraphicsDevice device) {
        this.device = device;
        this.buffer = device.createBuffer();
    }

    void bind(int quads, int first) {
        device.bindBuffer(BufferTarget.ELEMENT, buffer);
        if (first == uploadedFirst && quads <= uploadedQuads) {
            return;
        }
        indices = new int[quads * DrawMode.QUAD_INDICES];
        write(indices, quads, first);
        device.bufferData(BufferTarget.ELEMENT, indices, indices.length, BufferUsage.STATIC);
        uploadedQuads = quads;
        uploadedFirst = first;
    }

    static void write(int[] indices, int quads, int first) {
        for (int quad = 0, out = 0; quad < quads; quad++) {
            int base = first + quad * DrawMode.QUAD_VERTICES;
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
