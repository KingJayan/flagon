package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;

/** the attribute divisor, which 3.3 promoted out of ARB_instanced_arrays. */
public final class GL33 {

    public static final int GL_VERTEX_ATTRIB_ARRAY_DIVISOR = 0x88FE;
    public static final int GL_TIMESTAMP = 0x8E28;
    public static final int GL_SAMPLER_BINDING = 0x8919;
    public static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;

    private GL33() {
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        if (divisor < 0) {
            throw new IllegalArgumentException(
                    "glVertexAttribDivisor: divisor must not be negative: " + divisor);
        }
        GLState.get().attributes().slot(index).setDivisor(divisor);
    }
}
