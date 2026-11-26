package dev.flagon.lwjgl.internal;

/**
 * where each attribute sits in a vertex, for every layout mask. the mask has few bits, so every
 * answer is precomputed and the draw path only indexes a table.
 */
final class ImmediateLayout {

    static final int COLOR = 1;
    static final int NORMAL = 2;
    static final int TEX_COORD = 4;

    static final int MASKS = TEX_COORD << VertexArrays.UNITS;

    private static final int[] FLOATS = new int[MASKS];
    private static final int[] NORMAL_OFFSET = new int[MASKS];
    private static final int[][] TEX_COORD_OFFSET = new int[MASKS][VertexArrays.UNITS];

    static {
        for (int mask = 0; mask < MASKS; mask++) {
            int at = 3 + (has(mask, COLOR) ? 4 : 0);
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                TEX_COORD_OFFSET[mask][unit] = at;
                if (has(mask, texCoord(unit))) {
                    at += 2;
                }
            }
            NORMAL_OFFSET[mask] = at;
            FLOATS[mask] = at + (has(mask, NORMAL) ? 3 : 0);
        }
    }

    private ImmediateLayout() {
    }

    static int texCoord(int unit) {
        return TEX_COORD << unit;
    }

    static boolean has(int mask, int attribute) {
        return (mask & attribute) != 0;
    }

    static int floats(int mask) {
        return FLOATS[mask];
    }

    static int colorOffset(int mask) {
        return 3;
    }

    static int texCoordOffset(int mask, int unit) {
        return TEX_COORD_OFFSET[mask][unit];
    }

    static int normalOffset(int mask) {
        return NORMAL_OFFSET[mask];
    }
}
