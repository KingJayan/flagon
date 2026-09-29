package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.lwjgl.internal.Ops;

/**
 * the core-profile half of gl 1.3: multitexture without the client-side units
 */
public class GL13C {

    protected GL13C() {
    }

    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;
    public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_TEXTURE5 = 0x84C5;
    public static final int GL_TEXTURE6 = 0x84C6;
    public static final int GL_TEXTURE7 = 0x84C7;
    public static final int GL_ACTIVE_TEXTURE = 0x84E0;
    static GLState state() {
        return GLState.get();
    }
    /** selects the unit that {@code glBindTexture} and the texture parameters act on. */
    public static void glActiveTexture(int unit) {
        int selected = unitOf(unit, "glActiveTexture");
        if (state().isRecording() && state().record(Ops.ACTIVE_TEXTURE, selected)) {
            return;
        }
        state().activeTexture(selected);
    }
    static int unitOf(int unit, String call) {
        if (unit < GL_TEXTURE0) {
            throw new IllegalArgumentException(
                    call + " takes a GL_TEXTUREi name, got 0x" + Integer.toHexString(unit));
        }
        return unit - GL_TEXTURE0;
    }
}
