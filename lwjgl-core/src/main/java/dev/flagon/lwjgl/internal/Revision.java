package dev.flagon.lwjgl.internal;

/**
 * counts the changes to the state one draw reads, so a draw can tell nothing moved
 */
final class Revision {

    private int count;

    void bump() {
        count++;
    }

    int count() {
        return count;
    }
}
