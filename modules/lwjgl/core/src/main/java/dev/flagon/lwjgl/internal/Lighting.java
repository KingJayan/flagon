package dev.flagon.lwjgl.internal;

/**
  fixed-function lighting, material and fog state.
 */
public final class Lighting {

    public static final int LIGHTS = 8;
    public static final int TRACKS_AMBIENT = 1;
    public static final int TRACKS_DIFFUSE = 2;
    public static final int TRACKS_SPECULAR = 4;
    public static final int TRACKS_EMISSION = 8;

    public static final int LINEAR = 0;
    public static final int EXP = 1;
    public static final int EXP2 = 2;

    private boolean enabled;
    private final boolean[] lightEnabled = new boolean[LIGHTS];
    private final int[] lightEnabledInts = new int[LIGHTS];

    private final float[] position = new float[LIGHTS * 4];
    private final float[] ambient = new float[LIGHTS * 4];
    private final float[] diffuse = new float[LIGHTS * 4];
    private final float[] specular = new float[LIGHTS * 4];
    private final float[] attenuation = new float[LIGHTS * 3];

    private final float[] modelAmbient = {0.2f, 0.2f, 0.2f, 1.0f};

    private final float[] materialAmbient = {0.2f, 0.2f, 0.2f, 1.0f};
    private final float[] materialDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
    private final float[] materialSpecular = {0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] materialEmission = {0.0f, 0.0f, 0.0f, 1.0f};
    private float shininess;
    private boolean colorMaterial;
    private int colorMaterialTracks = TRACKS_AMBIENT | TRACKS_DIFFUSE;
    private boolean flatShading;

    private boolean fogEnabled;
    private int fogMode = EXP;
    private final float[] fogColor = new float[4];
    private float fogDensity = 1.0f;
    private float fogStart;
    private float fogEnd = 1.0f;

    private final Revision revision;

    public Lighting() {
        this(new Revision());
    }

    Lighting(Revision revision) {
        this.revision = revision;
        for (int light = 0; light < LIGHTS; light++) {
            position[light * 4 + 2] = 1.0f;
            attenuation[light * 3] = 1.0f;
            if (light == 0) {
                set(diffuse, 0, 1.0f, 1.0f, 1.0f, 1.0f);
                set(specular, 0, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private static void set(float[] target, int light, float x, float y, float z, float w) {
        target[light * 4] = x;
        target[light * 4 + 1] = y;
        target[light * 4 + 2] = z;
        target[light * 4 + 3] = w;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        revision.bump(Revision.LIGHTING);
        enabled = value;
    }

    public void setLightEnabled(int light, boolean value) {
        revision.bump(Revision.LIGHTING);
        lightEnabled[requireLight(light)] = value;
        lightEnabledInts[light] = value ? 1 : 0;
    }

    public boolean isLightEnabled(int light) {
        return lightEnabled[requireLight(light)];
    }

    void copyLightEnabledInto(boolean[] flags) {
        System.arraycopy(lightEnabled, 0, flags, 0, LIGHTS);
    }

    public int[] lightEnabledFlags() {
        return lightEnabledInts;
    }

    public static int requireLight(int light) {
        if (light < 0 || light >= LIGHTS) {
            throw new UnsupportedOperationException(
                    "light " + light + " is out of range; gl has GL_LIGHT0 through GL_LIGHT"
                            + (LIGHTS - 1));
        }
        return light;
    }

    /** the position gl stores is the one it was given, transformed by the modelview then. */
    public void setPosition(int light, float[] eyeSpace) {
        revision.bump(Revision.LIGHTING);
        System.arraycopy(eyeSpace, 0, position, requireLight(light) * 4, 4);
    }

    public void setAmbient(int light, float[] rgba) {
        revision.bump(Revision.LIGHTING);
        System.arraycopy(rgba, 0, ambient, requireLight(light) * 4, 4);
    }

    public void setDiffuse(int light, float[] rgba) {
        revision.bump(Revision.LIGHTING);
        System.arraycopy(rgba, 0, diffuse, requireLight(light) * 4, 4);
    }

    public void setSpecular(int light, float[] rgba) {
        revision.bump(Revision.LIGHTING);
        System.arraycopy(rgba, 0, specular, requireLight(light) * 4, 4);
    }

    /** constant, linear and quadratic, the three factors gl divides a light's reach by. */
    public void setAttenuation(int light, int which, float value) {
        revision.bump(Revision.LIGHTING);
        attenuation[requireLight(light) * 3 + which] = value;
    }

    public float[] positions() {
        return position;
    }

    public float[] ambients() {
        return ambient;
    }

    public float[] diffuses() {
        return diffuse;
    }

    public float[] speculars() {
        return specular;
    }

    public float[] attenuations() {
        return attenuation;
    }

    public float[] modelAmbient() {
        return modelAmbient;
    }

    public void setModelAmbient(float[] rgba) {
        revision.bump(Revision.LIGHTING);
        System.arraycopy(rgba, 0, modelAmbient, 0, 4);
    }

    public float[] materialAmbient() {
        return materialAmbient;
    }

    public float[] materialDiffuse() {
        return materialDiffuse;
    }

    public float[] materialSpecular() {
        return materialSpecular;
    }

    public float[] materialEmission() {
        return materialEmission;
    }

    public float shininess() {
        return shininess;
    }

    public void setShininess(float value) {
        revision.bump(Revision.LIGHTING);
        shininess = value;
    }

    public boolean isFlatShading() {
        return flatShading;
    }

    public void setFlatShading(boolean value) {
        flatShading = value;
        revision.bump(Revision.LIGHTING);
    }

    public boolean isColorMaterial() {
        return colorMaterial;
    }

    public void setColorMaterial(boolean value) {
        revision.bump(Revision.LIGHTING);
        colorMaterial = value;
    }

    /** the materials the vertex color stands in for, as TRACKS_ bits */
    public int colorMaterialTracks() {
        return colorMaterial ? colorMaterialTracks : 0;
    }

    public void setColorMaterialTracks(int tracks) {
        revision.bump(Revision.LIGHTING);
        colorMaterialTracks = tracks;
    }

    public boolean isFogEnabled() {
        return fogEnabled;
    }

    public void setFogEnabled(boolean value) {
        revision.bump(Revision.FOG);
        fogEnabled = value;
    }

    public int fogMode() {
        return fogMode;
    }

    public void setFogMode(int mode) {
        revision.bump(Revision.FOG);
        fogMode = mode;
    }

    public float[] fogColor() {
        return fogColor;
    }

    public void setFogColor(float[] rgba) {
        revision.bump(Revision.FOG);
        System.arraycopy(rgba, 0, fogColor, 0, 4);
    }

    public float fogDensity() {
        return fogDensity;
    }

    public void setFogDensity(float value) {
        revision.bump(Revision.FOG);
        fogDensity = value;
    }

    public float fogStart() {
        return fogStart;
    }

    public void setFogStart(float value) {
        revision.bump(Revision.FOG);
        fogStart = value;
    }

    public float fogEnd() {
        return fogEnd;
    }

    public void setFogEnd(float value) {
        revision.bump(Revision.FOG);
        fogEnd = value;
    }
}
