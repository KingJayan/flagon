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

    private int layout;
    private final float[] heldColor = new float[4];
    private final float[] heldS = new float[VertexArrays.UNITS];
    private final float[] heldT = new float[VertexArrays.UNITS];
    private final float[] heldNormal = new float[3];

    void begin(DrawMode mode) {
        if (building) {
            throw new IllegalStateException("glBegin called inside glBegin/glEnd");
        }
        building = true;
        primitive = mode;
        floatCount = 0;
        vertexCount = 0;
        layout = 0;
    }

    /**
     * gl carries one value per attribute, so a block that never sets an attribute holds whatever
     * the state held when it opened. that value fills the gap when the block sets the attribute
     * later and the vertices already built have to grow.
     */
    void hold(float red, float green, float blue, float alpha, float[] s, float[] t,
              float normalX, float normalY, float normalZ) {
        heldColor[0] = red;
        heldColor[1] = green;
        heldColor[2] = blue;
        heldColor[3] = alpha;
        System.arraycopy(s, 0, heldS, 0, VertexArrays.UNITS);
        System.arraycopy(t, 0, heldT, 0, VertexArrays.UNITS);
        heldNormal[0] = normalX;
        heldNormal[1] = normalY;
        heldNormal[2] = normalZ;
    }

    /** call before the state changes, so a held value still matches the vertices already built */
    void touch(int attribute) {
        if (!building || ImmediateLayout.has(layout, attribute)) {
            return;
        }
        int widened = layout | attribute;
        if (vertexCount > 0) {
            widen(widened);
        }
        layout = widened;
        floatCount = vertexCount * ImmediateLayout.floats(layout);
    }

    void vertex(float x, float y, float z,
                float red, float green, float blue, float alpha,
                float[] s, float[] t,
                float normalX, float normalY, float normalZ) {
        if (!building) {
            throw new IllegalStateException("glVertex called outside glBegin/glEnd");
        }
        room(floatCount + ImmediateLayout.floats(layout));
        int at = floatCount;
        vertices[at++] = x;
        vertices[at++] = y;
        vertices[at++] = z;
        if (ImmediateLayout.has(layout, ImmediateLayout.COLOR)) {
            vertices[at++] = red;
            vertices[at++] = green;
            vertices[at++] = blue;
            vertices[at++] = alpha;
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (ImmediateLayout.has(layout, ImmediateLayout.texCoord(unit))) {
                vertices[at++] = s[unit];
                vertices[at++] = t[unit];
            }
        }
        if (ImmediateLayout.has(layout, ImmediateLayout.NORMAL)) {
            vertices[at++] = normalX;
            vertices[at++] = normalY;
            vertices[at++] = normalZ;
        }
        floatCount = at;
        vertexCount++;
    }

    /**
     * moves every vertex built so far into the wider layout, last one first so nothing is
     * overwritten before it is read. an attribute the block had not set yet takes the held value.
     */
    private void widen(int widened) {
        int was = ImmediateLayout.floats(layout);
        int now = ImmediateLayout.floats(widened);
        room(vertexCount * now);
        float[] record = new float[now];
        for (int vertex = vertexCount - 1; vertex >= 0; vertex--) {
            int from = vertex * was;
            System.arraycopy(vertices, from, record, 0, 3);
            if (ImmediateLayout.has(widened, ImmediateLayout.COLOR)) {
                carry(record, ImmediateLayout.colorOffset(widened), from, ImmediateLayout.COLOR,
                        ImmediateLayout.colorOffset(layout), heldColor, 0, 4);
            }
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                int attribute = ImmediateLayout.texCoord(unit);
                if (!ImmediateLayout.has(widened, attribute)) {
                    continue;
                }
                int at = ImmediateLayout.texCoordOffset(widened, unit);
                if (ImmediateLayout.has(layout, attribute)) {
                    System.arraycopy(vertices,
                            from + ImmediateLayout.texCoordOffset(layout, unit), record, at, 2);
                } else {
                    record[at] = heldS[unit];
                    record[at + 1] = heldT[unit];
                }
            }
            if (ImmediateLayout.has(widened, ImmediateLayout.NORMAL)) {
                carry(record, ImmediateLayout.normalOffset(widened), from, ImmediateLayout.NORMAL,
                        ImmediateLayout.normalOffset(layout), heldNormal, 0, 3);
            }
            System.arraycopy(record, 0, vertices, vertex * now, now);
        }
    }

    private void carry(float[] record, int at, int from, int attribute, int had,
                       float[] held, int heldAt, int count) {
        if (ImmediateLayout.has(layout, attribute)) {
            System.arraycopy(vertices, from + had, record, at, count);
        } else {
            System.arraycopy(held, heldAt, record, at, count);
        }
    }

    private void room(int floats) {
        if (floats > vertices.length) {
            vertices = Arrays.copyOf(vertices, Math.max(vertices.length * 2, floats));
        }
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

    DrawMode mode() {
        return primitive;
    }

    int layout() {
        return layout;
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
