package dev.flagon.lwjgl.internal;

public final class VertexArrays {

    public static final int UNITS = 4;

    public static final int POSITION = 0;
    public static final int COLOR = 1;
    public static final int TEX_COORD = 2;
    public static final int NORMAL = TEX_COORD + UNITS;
    public static final int SLOTS = NORMAL + 1;

    private final ClientArray[] slots = new ClientArray[SLOTS];

    public VertexArrays() {
        slots[POSITION] = new ClientArray(3);
        slots[COLOR] = new ClientArray(4);
        for (int unit = 0; unit < UNITS; unit++) {
            slots[TEX_COORD + unit] = new ClientArray(2);
        }
        slots[NORMAL] = new ClientArray(3);
    }

    public ClientArray slot(int slot) {
        return slots[slot];
    }

    public ClientArray position() {
        return slots[POSITION];
    }

    public ClientArray color() {
        return slots[COLOR];
    }

    public ClientArray texCoord(int unit) {
        return slots[TEX_COORD + requireUnit(unit)];
    }

    public ClientArray normal() {
        return slots[NORMAL];
    }

    public static int requireUnit(int unit) {
        if (unit < 0 || unit >= UNITS) {
            throw new UnsupportedOperationException(
                    "texture unit " + unit + " is out of range; the fixed-function pipeline here "
                            + "has " + UNITS + " units");
        }
        return unit;
    }

    public boolean canDraw() {
        return position().isEnabled() && position().isBound();
    }

    public void reset() {
        for (ClientArray slot : slots) {
            slot.reset();
        }
    }
}
