package dev.flagon.lwjgl.internal;

import java.util.Arrays;

/**
 * glTexGen: which of each unit's s, t, r and q coordinates the vertex shader makes from the
 * position, and the planes it makes them with. the defaults are gl's.
 */
public final class TextureGeneration {

    // a coordinate's mode, as the shader numbers them; OFF is also what a disabled one reads
    public static final int OFF = 0;
    public static final int OBJECT_LINEAR = 1;
    public static final int EYE_LINEAR = 2;
    public static final int SPHERE_MAP = 3;

    public static final int COORDINATES = 4;

    private final Revision revision;
    private final boolean[] enabled = new boolean[VertexArrays.UNITS * COORDINATES];
    private final int[] modes = new int[VertexArrays.UNITS * COORDINATES];
    private final int[] codes = new int[VertexArrays.UNITS * COORDINATES];
    private final float[] objectPlanes = new float[VertexArrays.UNITS * COORDINATES * 4];
    private final float[] eyePlanes = new float[VertexArrays.UNITS * COORDINATES * 4];
    private int generating;

    TextureGeneration(Revision revision) {
        this.revision = revision;
        Arrays.fill(modes, EYE_LINEAR);
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            // s takes x and t takes y; r and q planes start at zero
            int s = unit * COORDINATES * 4;
            objectPlanes[s] = eyePlanes[s] = 1.0f;
            objectPlanes[s + 5] = eyePlanes[s + 5] = 1.0f;
        }
    }

    /** whether any coordinate of any unit is generated */
    public boolean generates() {
        return generating > 0;
    }

    public boolean isEnabled(int unit, int coordinate) {
        return enabled[unit * COORDINATES + coordinate];
    }

    public void setEnabled(int unit, int coordinate, boolean on) {
        int at = unit * COORDINATES + coordinate;
        if (enabled[at] != on) {
            enabled[at] = on;
            generating += on ? 1 : -1;
            recode(at);
        }
    }

    public int mode(int unit, int coordinate) {
        return modes[unit * COORDINATES + coordinate];
    }

    public void setMode(int unit, int coordinate, int mode) {
        int at = unit * COORDINATES + coordinate;
        if (modes[at] != mode) {
            modes[at] = mode;
            recode(at);
        }
    }

    public void setPlane(int unit, int coordinate, boolean eye, float a, float b, float c,
                         float d) {
        float[] planes = eye ? eyePlanes : objectPlanes;
        int at = (unit * COORDINATES + coordinate) * 4;
        planes[at] = a;
        planes[at + 1] = b;
        planes[at + 2] = c;
        planes[at + 3] = d;
        revision.bump(Revision.GENERATION);
    }

    /** each coordinate's mode, or OFF where it is not enabled; four to a unit */
    public int[] codes() {
        return codes;
    }

    /** four floats to a plane, four planes to a unit */
    public float[] objectPlanes() {
        return objectPlanes;
    }

    public float[] eyePlanes() {
        return eyePlanes;
    }

    private void recode(int at) {
        codes[at] = enabled[at] ? modes[at] : OFF;
        revision.bump(Revision.GENERATION);
    }
}
