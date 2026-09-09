package dev.flagon.lwjgl.internal;

/**
 #'d vertex attributes a shader program reads, as set by {@code glVertexAttribPointer}
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
