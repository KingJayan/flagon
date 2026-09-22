package dev.flagon.lwjgl.internal;

/**
 * counts the changes to the state one draw reads, so a draw can tell what stood still
 */
final class Revision {

    static final int MATRIX = 0;
    static final int LIGHTING = 1;
    static final int FOG = 2;
    static final int TEXTURE = 3;
    static final int ALPHA = 4;
    static final int POINT = 5;
    static final int GROUPS = 6;

    private final int[] counts = new int[GROUPS];

    void bump(int group) {
        counts[group]++;
    }

    int count(int group) {
        return counts[group];
    }
}
