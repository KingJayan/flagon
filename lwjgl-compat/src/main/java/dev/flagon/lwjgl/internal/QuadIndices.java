package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;

/**
 * the element buffer that turns quads into triangle pairs.
 *
 * <p>the pattern depends only on the quad count and the first vertex, and the indices for
 * {@code n} quads are a prefix of those for any larger {@code n}, so it is uploaded once and
 * re-uploaded only when it has to grow.
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

    /** binds an element buffer covering {@code quads} quads whose first vertex is {@code first}. */
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

    /**
     * for quad {@code q} based at {@code first + 4q}, the triangles are (0,1,2) and (0,2,3),
     * which keeps gl's winding order so face culling behaves the same.
     */
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
