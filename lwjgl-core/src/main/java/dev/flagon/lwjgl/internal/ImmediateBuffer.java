package dev.flagon.lwjgl.internal;

import java.util.Arrays;

/**
 * the vertices a glBegin/glEnd block builds, in the layout the fixed-function pipeline draws
 */
final class ImmediateBuffer {

    private static final int INITIAL_VERTICES = 256;

    private float[] vertices = new float[FixedFunctionPipeline.VERTEX_FLOATS * INITIAL_VERTICES];
    private int floatCount;
    private int vertexCount;
    private DrawMode primitive;
    private boolean building;

    void begin(DrawMode mode) {
        if (building) {
            throw new IllegalStateException("glBegin called inside glBegin/glEnd");
        }
        building = true;
        primitive = mode;
        floatCount = 0;
        vertexCount = 0;
    }

    void end() {
        if (!building) {
            throw new IllegalStateException("glEnd called without glBegin");
        }
        building = false;
    }

    boolean isBuilding() {
        return building;
    }

    void vertex(float x, float y, float z,
                float red, float green, float blue, float alpha,
                float[] s, float[] t,
                float normalX, float normalY, float normalZ) {
        if (!building) {
            throw new IllegalStateException("glVertex called outside glBegin/glEnd");
        }
        if (floatCount + FixedFunctionPipeline.VERTEX_FLOATS > vertices.length) {
            vertices = Arrays.copyOf(vertices, vertices.length * 2);
        }
        vertices[floatCount++] = x;
        vertices[floatCount++] = y;
        vertices[floatCount++] = z;
        vertices[floatCount++] = red;
        vertices[floatCount++] = green;
        vertices[floatCount++] = blue;
        vertices[floatCount++] = alpha;
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            vertices[floatCount++] = s[unit];
            vertices[floatCount++] = t[unit];
        }
        vertices[floatCount++] = normalX;
        vertices[floatCount++] = normalY;
        vertices[floatCount++] = normalZ;
        vertexCount++;
    }

    DrawMode mode() {
        return primitive;
    }

    float[] vertices() {
        return vertices;
    }

    int floatCount() {
        return floatCount;
    }

    int vertexCount() {
        return vertexCount;
    }

    /** a copy of what was built, for a display list to keep after the buffer is reused */
    float[] captured() {
        return Arrays.copyOf(vertices, floatCount);
    }
}
