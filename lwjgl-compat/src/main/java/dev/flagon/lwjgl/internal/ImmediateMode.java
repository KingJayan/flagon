package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.Primitive;

/**
 * what {@code glBegin} accepts, which is a superset of what the device can draw.
 *
 * <p>gl's {@code GL_QUADS} has no webgl equivalent, so it maps onto triangles and is drawn
 * through an index buffer instead of straight from the vertex array.
 */
public enum ImmediateMode {

    POINTS(Primitive.POINTS),
    LINES(Primitive.LINES),
    LINE_STRIP(Primitive.LINE_STRIP),
    LINE_LOOP(Primitive.LINE_LOOP),
    TRIANGLES(Primitive.TRIANGLES),
    TRIANGLE_STRIP(Primitive.TRIANGLE_STRIP),
    TRIANGLE_FAN(Primitive.TRIANGLE_FAN),
    QUADS(Primitive.TRIANGLES);

    /** vertices per quad, and the indices the two triangles covering one need. */
    public static final int QUAD_VERTICES = 4;
    public static final int QUAD_INDICES = 6;

    private final Primitive primitive;

    ImmediateMode(Primitive primitive) {
        this.primitive = primitive;
    }

    public Primitive primitive() {
        return primitive;
    }

    public boolean isIndexed() {
        return this == QUADS;
    }

    /**
     * gl discards a trailing incomplete primitive rather than drawing a malformed one.
     *
     * @return how many of {@code vertexCount} vertices actually get drawn
     */
    public int usableVertices(int vertexCount) {
        return this == QUADS ? vertexCount - (vertexCount % QUAD_VERTICES) : vertexCount;
    }

    /**
     * fills {@code indices} with the triangle pairs covering {@code quads} quads: for quad
     * {@code q} based at {@code 4q}, the triangles are (0,1,2) and (0,2,3), which keeps gl's
     * winding order so face culling behaves the same.
     */
    public static void writeQuadIndices(int[] indices, int quads) {
        for (int quad = 0, out = 0; quad < quads; quad++) {
            int base = quad * QUAD_VERTICES;
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base;
            indices[out++] = base + 2;
            indices[out++] = base + 3;
        }
    }
}
