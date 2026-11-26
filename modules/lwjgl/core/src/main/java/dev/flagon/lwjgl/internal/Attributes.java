package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.Winding;

/** server state snapshot */
final class Attributes {

    private int mask;
    private boolean depthTest;
    private boolean blend;
    private boolean cullFace;
    private boolean scissorTest;
    private boolean texture;
    private boolean alphaTest;
    private boolean lighting;
    private boolean fog;
    private boolean colorMaterial;
    private final boolean[] lightEnabled = new boolean[Lighting.LIGHTS];
    private float normalX;
    private float normalY;
    private float normalZ;
    private float clearRed;
    private float clearGreen;
    private float clearBlue;
    private float clearAlpha;
    private double clearDepth;
    private boolean maskRed;
    private boolean maskGreen;
    private boolean maskBlue;
    private boolean maskAlpha;
    private boolean depthWrite;
    private CompareFunc depthFunc;
    private BlendFactor blendSource;
    private BlendFactor blendDestination;
    private CompareFunc alphaFunc;
    private float alphaReference;
    private CullFace culledFaces;
    private Winding frontFace;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scissorX;
    private int scissorY;
    private int scissorWidth;
    private int scissorHeight;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    private float s;
    private float t;
    private int matrixMode;
    private int texture2d;

    static final int CURRENT_BIT = 0x00000001;
    static final int POLYGON_BIT = 0x00000008;
    static final int DEPTH_BUFFER_BIT = 0x00000100;
    static final int VIEWPORT_BIT = 0x00000800;
    static final int TRANSFORM_BIT = 0x00001000;
    static final int ENABLE_BIT = 0x00002000;
    static final int COLOR_BUFFER_BIT = 0x00004000;
    static final int TEXTURE_BIT = 0x00040000;
    static final int SCISSOR_BIT = 0x00080000;
    static final int ALL_ATTRIB_BITS = 0x000FFFFF;

    void hold(GLState state, int mask, boolean depthTest, boolean blend, boolean cullFace,
            boolean scissorTest, boolean texture, boolean alphaTest, boolean lighting,
            boolean fog, boolean colorMaterial, float normalX, float normalY, float normalZ,
            float clearRed, float clearGreen, float clearBlue, float clearAlpha,
            double clearDepth, boolean maskRed, boolean maskGreen, boolean maskBlue,
            boolean maskAlpha, boolean depthWrite, CompareFunc depthFunc,
            BlendFactor blendSource, BlendFactor blendDestination, CompareFunc alphaFunc,
            float alphaReference, CullFace culledFaces, Winding frontFace, int viewportX,
            int viewportY, int viewportWidth, int viewportHeight, int scissorX, int scissorY,
            int scissorWidth, int scissorHeight, float red, float green, float blue,
            float alpha, float s, float t, int matrixMode, int texture2d) {
        this.mask = mask;
        this.depthTest = depthTest;
        this.blend = blend;
        this.cullFace = cullFace;
        this.scissorTest = scissorTest;
        this.texture = texture;
        this.alphaTest = alphaTest;
        this.lighting = lighting;
        this.fog = fog;
        this.colorMaterial = colorMaterial;
        state.lighting().copyLightEnabledInto(lightEnabled);
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.clearRed = clearRed;
        this.clearGreen = clearGreen;
        this.clearBlue = clearBlue;
        this.clearAlpha = clearAlpha;
        this.clearDepth = clearDepth;
        this.maskRed = maskRed;
        this.maskGreen = maskGreen;
        this.maskBlue = maskBlue;
        this.maskAlpha = maskAlpha;
        this.depthWrite = depthWrite;
        this.depthFunc = depthFunc;
        this.blendSource = blendSource;
        this.blendDestination = blendDestination;
        this.alphaFunc = alphaFunc;
        this.alphaReference = alphaReference;
        this.culledFaces = culledFaces;
        this.frontFace = frontFace;
        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.scissorX = scissorX;
        this.scissorY = scissorY;
        this.scissorWidth = scissorWidth;
        this.scissorHeight = scissorHeight;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.s = s;
        this.t = t;
        this.matrixMode = matrixMode;
        this.texture2d = texture2d;
    }

    private boolean saved(int bit) {
        return (mask & bit) != 0;
    }

    void restore(GLState state) {
        if (saved(ENABLE_BIT)) {
            state.setDepthTest(depthTest);
            state.setBlend(blend);
            state.setCullFace(cullFace);
            state.setScissorTest(scissorTest);
            state.setTextureEnabled(texture);
            state.setAlphaTestEnabled(alphaTest);
            state.lighting().setEnabled(lighting);
            state.lighting().setFogEnabled(fog);
            state.lighting().setColorMaterial(colorMaterial);
            for (int light = 0; light < lightEnabled.length; light++) {
                state.lighting().setLightEnabled(light, lightEnabled[light]);
            }
        }
        if (saved(CURRENT_BIT)) {
            state.color(red, green, blue, alpha);
            state.texCoord(0, s, t);
            state.normal(normalX, normalY, normalZ);
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
