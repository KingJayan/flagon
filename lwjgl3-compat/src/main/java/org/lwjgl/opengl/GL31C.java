package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLEnums;
import dev.flagon.lwjgl.internal.GLState;

/**
 * gl 3.1 instanced drawing, all of which is core profile
 */
public class GL31C {

    protected GL31C() {
    }

    public static final int GL_TEXTURE_BUFFER = 0x8C2A;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_PRIMITIVE_RESTART = 0x8F9D;
    public static final int GL_PRIMITIVE_RESTART_INDEX = 0x8F9E;
    public static void glDrawArraysInstanced(int mode, int first, int count, int instances) {
        GLState.get().drawArraysInstanced(
                GLEnums.drawMode(mode, "glDrawArraysInstanced"), first, count, instances);
    }
    public static void glDrawElementsInstanced(int mode, int count, int type, long offset,
                                               int instances) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "glDrawElementsInstanced: offset out of range: " + offset);
        }
        GLState.get().drawElementsInstanced(
                GLEnums.drawMode(mode, "glDrawElementsInstanced"), count,
                GLEnums.indexType(type, "glDrawElementsInstanced"), (int) offset, instances);
    }
}
