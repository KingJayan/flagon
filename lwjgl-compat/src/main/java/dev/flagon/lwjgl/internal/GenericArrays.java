package dev.flagon.lwjgl.internal;

/**
 * the numbered vertex attributes a shader program reads, as set by {@code glVertexAttribPointer}.
 *
 * <p>separate from {@link VertexArrays} because they are separate state in gl too: the fixed
 * function arrays feed {@code gl_Vertex} and its neighbours, these feed whatever the program
 * declares. which set a draw reads from is decided by whether a program is bound.
 */
public final class GenericArrays {

    private final ClientArray[] slots;

    public GenericArrays(int count) {
        slots = new ClientArray[count];
        for (int slot = 0; slot < count; slot++) {
            // gl's own default: an attribute with no pointer set reads four components
            slots[slot] = new ClientArray(4);
        }
    }

    public int count() {
        return slots.length;
    }

    public ClientArray slot(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IllegalArgumentException(
                    "vertex attribute " + index + " is outside the " + slots.length
                            + " this context supports");
        }
        return slots[index];
    }

    /** @return true when at least one attribute could supply vertices */
    public boolean canDraw() {
        for (ClientArray slot : slots) {
            if (slot.isEnabled() && slot.isBound()) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        for (ClientArray slot : slots) {
            slot.reset();
        }
    }
}
