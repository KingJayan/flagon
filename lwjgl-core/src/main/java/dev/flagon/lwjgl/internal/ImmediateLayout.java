package dev.flagon.lwjgl.internal;

final class ImmediateLayout {

    static final int COLOR = 1;
    static final int NORMAL = 2;
    static final int TEX_COORD = 4;

    static final int MASKS = TEX_COORD << VertexArrays.UNITS;

    private ImmediateLayout() {
    }

    static int texCoord(int unit) {
        return TEX_COORD << unit;
    }

    static boolean has(int mask, int attribute) {
        return (mask & attribute) != 0;
    }

    static int floats(int mask) {
        int floats = 3;
        if (has(mask, COLOR)) {
            floats += 4;
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (has(mask, texCoord(unit))) {
                floats += 2;
            }
        }
        if (has(mask, NORMAL)) {
            floats += 3;
        }
        return floats;
    }

    static int colorOffset(int mask) {
        return 3;
    }

    static int texCoordOffset(int mask, int unit) {
        int at = 3 + (has(mask, COLOR) ? 4 : 0);
        for (int before = 0; before < unit; before++) {
            if (has(mask, texCoord(before))) {
                at += 2;
            }
        }
        return at;
    }

    static int normalOffset(int mask) {
        return floats(mask) - 3;
    }
}
