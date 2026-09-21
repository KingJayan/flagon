package dev.flagon.lwjgl.internal;

import java.util.Arrays;

/**
 * glPushMatrix runs thousands of times a frame, so the stack is one flat array with an index
 */
final class MatrixStack {

    private static final int INITIAL_DEPTH = 16;

    private float[] slots = new float[16 * INITIAL_DEPTH];
    private int depth;

    void push(float[] matrix) {
        if ((depth + 1) * 16 > slots.length) {
            slots = Arrays.copyOf(slots, slots.length * 2);
        }
        System.arraycopy(matrix, 0, slots, depth * 16, 16);
        depth++;
    }

    void pop(float[] into) {
        depth--;
        System.arraycopy(slots, depth * 16, into, 0, 16);
    }

    boolean isEmpty() {
        return depth == 0;
    }
}
