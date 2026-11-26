package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.Primitive;

/**
 * what {@code glBegin} and {@code glDrawArrays} accept
 */
public enum DrawMode {

    POINTS(Primitive.POINTS),
    LINES(Primitive.LINES),
    LINE_STRIP(Primitive.LINE_STRIP),
    LINE_LOOP(Primitive.LINE_LOOP),
    TRIANGLES(Primitive.TRIANGLES),
    TRIANGLE_STRIP(Primitive.TRIANGLE_STRIP),
    TRIANGLE_FAN(Primitive.TRIANGLE_FAN),
    QUADS(Primitive.TRIANGLES);

    public static final int QUAD_VERTICES = 4;
    public static final int QUAD_INDICES = 6;

    private final Primitive primitive;

    DrawMode(Primitive primitive) {
        this.primitive = primitive;
    }

    public Primitive primitive() {
        return primitive;
    }

    public boolean isIndexed() {
        return this == QUADS;
    }

    public int usableVertices(int vertexCount) {
        return this == QUADS ? vertexCount - (vertexCount % QUAD_VERTICES) : vertexCount;
    }
}
