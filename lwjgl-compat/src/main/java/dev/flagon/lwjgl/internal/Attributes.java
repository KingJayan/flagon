package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.Winding;

/**
 * a snapshot of the server state {@code glPushAttrib} saves
 */
public record Attributes(
        int mask,
        boolean depthTest,
        boolean blend,
        boolean cullFace,
        boolean scissorTest,
        boolean texture,
        boolean alphaTest,
        float clearRed, float clearGreen, float clearBlue, float clearAlpha,
        double clearDepth,
        boolean maskRed, boolean maskGreen, boolean maskBlue, boolean maskAlpha,
        boolean depthWrite,
        CompareFunc depthFunc,
        BlendFactor blendSource, BlendFactor blendDestination,
        CompareFunc alphaFunc, float alphaReference,
        CullFace culledFaces, Winding frontFace,
        int viewportX, int viewportY, int viewportWidth, int viewportHeight,
        int scissorX, int scissorY, int scissorWidth, int scissorHeight,
        float red, float green, float blue, float alpha,
        float s, float t,
        int matrixMode,
        int texture2d) {

    public static final int CURRENT_BIT = 0x00000001;
    public static final int POLYGON_BIT = 0x00000008;
    public static final int DEPTH_BUFFER_BIT = 0x00000100;
    public static final int VIEWPORT_BIT = 0x00000800;
    public static final int TRANSFORM_BIT = 0x00001000;
    public static final int ENABLE_BIT = 0x00002000;
    public static final int COLOR_BUFFER_BIT = 0x00004000;
    public static final int TEXTURE_BIT = 0x00040000;
    public static final int SCISSOR_BIT = 0x00080000;
    public static final int ALL_ATTRIB_BITS = 0x000FFFFF;

    private boolean saved(int bit) {
        return (mask & bit) != 0;
    }

    /** writes back every group the mask named through the setters that also drive the device */
    public void restore(GLState state) {
        if (saved(ENABLE_BIT)) {
            state.setDepthTest(depthTest);
            state.setBlend(blend);
            state.setCullFace(cullFace);
            state.setScissorTest(scissorTest);
            state.setTextureEnabled(texture);
            state.setAlphaTestEnabled(alphaTest);
        }
        if (saved(CURRENT_BIT)) {
            state.color(red, green, blue, alpha);
            state.texCoord(0, s, t);
        }
        if (saved(COLOR_BUFFER_BIT)) {
            state.setBlend(blend);
            state.setBlendFunc(blendSource, blendDestination);
            state.setColorMask(maskRed, maskGreen, maskBlue, maskAlpha);
            state.clearColor(clearRed, clearGreen, clearBlue, clearAlpha);
            state.setAlphaTestEnabled(alphaTest);
            state.restoreAlphaFunc(alphaFunc, alphaReference);
        }
        if (saved(DEPTH_BUFFER_BIT)) {
            state.setDepthTest(depthTest);
            state.setDepthFunc(depthFunc);
            state.setDepthWrite(depthWrite);
            state.clearDepth(clearDepth);
        }
        if (saved(VIEWPORT_BIT)) {
            state.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
        }
        if (saved(SCISSOR_BIT)) {
            state.setScissorTest(scissorTest);
            state.setScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        }
        if (saved(TRANSFORM_BIT)) {
            state.matrixMode(matrixMode);
        }
        if (saved(TEXTURE_BIT)) {
            state.setTextureEnabled(texture);
            state.bindTexture(texture2d);
        }
        if (saved(POLYGON_BIT)) {
            state.setCullFace(cullFace);
            state.setCulledFaces(culledFaces);
            state.setFrontFace(frontFace);
        }
    }
}
