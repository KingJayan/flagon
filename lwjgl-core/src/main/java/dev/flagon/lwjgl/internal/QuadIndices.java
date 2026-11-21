package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

/* quads --> triangle pairs */
final class QuadIndices {

    private final GraphicsDevice device;
    private final int buffer;

    private int uploadedQuads;

    QuadIndices(GraphicsDevice device) {
        this.device = device;
        this.buffer = device.createBuffer();
    }

    void bind(int quads) {
        device.bindBuffer(BufferTarget.ELEMENT, buffer);
        if (quads <= uploadedQuads) {
            return;
        }
        // immediate blocks move through a ring, so the quads needed keep rising a little
        quads = Math.max(quads, uploadedQuads * 2);
        int[] indices = new int[quads * DrawMode.QUAD_INDICES];
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
