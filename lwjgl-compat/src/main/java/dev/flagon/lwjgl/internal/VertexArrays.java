package dev.flagon.lwjgl.internal;

/**
 * the three client arrays the fixed-function shader has attributes for
 */
public final class VertexArrays {

    public static final int POSITION = 0;
    public static final int COLOR = 1;
    public static final int TEX_COORD = 2;
    public static final int SLOTS = 3;

    private final ClientArray[] slots = {
            new ClientArray(3),
            new ClientArray(4),
            new ClientArray(2),
    };

    public ClientArray slot(int slot) {
        return slots[slot];
    }

    public ClientArray position() {
        return slots[POSITION];
    }

    public ClientArray color() {
        return slots[COLOR];
    }

    public ClientArray texCoord() {
        return slots[TEX_COORD];
    }

    /** @return true when geometry can be drawn at all; gl draws nothing without positions */
    public boolean canDraw() {
        return position().isEnabled() && position().isBound();
    }

    public void reset() {
        for (ClientArray slot : slots) {
            slot.reset();
        }
    }
}
