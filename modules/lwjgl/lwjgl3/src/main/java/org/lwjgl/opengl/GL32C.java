package org.lwjgl.opengl;

/**
 * gl 3.2 base-vertex drawing, all of which is core profile
 */
public class GL32C {

    protected GL32C() {
    }

    public static final int GL_CONTEXT_PROFILE_MASK = 0x9126;
    public static final int GL_DEPTH_CLAMP = 0x864F;
    public static final int GL_MAX_SAMPLE_MASK_WORDS = 0x8E59;
    public static final int GL_TEXTURE_2D_MULTISAMPLE = 0x9100;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;

    public static void glDrawElementsBaseVertex(int mode, int count, int type, long offset,
                                                int baseVertex) {
        throw new UnsupportedOperationException(
                "glDrawElementsBaseVertex is not emulated; webgl2 has no base vertex. add the "
                        + "offset to the indices, or bind the vertex data at the right offset");
    }
    public static void glDrawRangeElementsBaseVertex(int mode, int start, int end, int count,
                                                     int type, long offset, int baseVertex) {
        glDrawElementsBaseVertex(mode, count, type, offset, baseVertex);
    }
    public static void glDrawElementsInstancedBaseVertex(int mode, int count, int type, long offset,
                                                         int instances, int baseVertex) {
        glDrawElementsBaseVertex(mode, count, type, offset, baseVertex);
    }
}
