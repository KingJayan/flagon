package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.ClientArray;
import dev.flagon.lwjgl.internal.Ops;
import dev.flagon.lwjgl.internal.VertexArrays;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.CompareFunc;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * gl 1.1 over webgl2: the core profile of {@link GL11C}, and the fixed-function half here
 */
public final class GL11 extends GL11C {

    private GL11() {
    }

    public static final int GL_MODELVIEW = 0x1700;
    public static final int GL_PROJECTION = 0x1701;
    public static final int GL_TEXTURE_ENV = 0x2300;
    public static final int GL_TEXTURE_ENV_MODE = 0x2200;
    public static final int GL_TEXTURE_ENV_COLOR = 0x2201;
    public static final int GL_TEXTURE_GEN_S = 0x0C60;
    public static final int GL_TEXTURE_GEN_T = 0x0C61;
    public static final int GL_TEXTURE_GEN_R = 0x0C62;
    public static final int GL_TEXTURE_GEN_Q = 0x0C63;
    public static final int GL_TEXTURE_GEN_MODE = 0x2500;
    public static final int GL_OBJECT_LINEAR = 0x2401;
    public static final int GL_OBJECT_PLANE = 0x2501;
    public static final int GL_EYE_LINEAR = 0x2400;
    public static final int GL_EYE_PLANE = 0x2502;
    public static final int GL_SPHERE_MAP = 0x2402;
    public static final int GL_S = 0x2000;
    public static final int GL_T = 0x2001;
    public static final int GL_R = 0x2002;
    public static final int GL_Q = 0x2003;
    public static final int GL_COLOR_LOGIC_OP = 0x0BF2;
    public static final int GL_CLEAR = 0x1500;
    public static final int GL_AND = 0x1501;
    public static final int GL_COPY = 0x1503;
    public static final int GL_XOR = 0x1506;
    public static final int GL_OR = 0x1507;
    public static final int GL_INVERT = 0x150A;
    public static final int GL_OR_REVERSE = 0x150B;
    public static final int GL_SET = 0x150F;
    public static final int GL_MODULATE = 0x2100;
    public static final int GL_ADD = 0x0104;
    public static final int GL_DECAL = 0x2101;
    public static final int GL_COMBINE = 0x8570;
    public static final int GL_VERTEX_ARRAY = 0x8074;
    public static final int GL_NORMAL_ARRAY = 0x8075;
    public static final int GL_COLOR_ARRAY = 0x8076;
    public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
    public static final int GL_LIGHT_MODEL_AMBIENT = 0x0B53;
    public static final int GL_SHININESS = 0x1601;
    public static final int GL_FOG_COLOR = 0x0B66;
    public static final int GL_FLAT = 0x1D00;
    public static final int GL_SMOOTH = 0x1D01;
    public static final int GL_COMPILE = 0x1300;
    public static final int GL_COMPILE_AND_EXECUTE = 0x1301;
    public static final int GL_CURRENT_BIT = 0x00000001;
    public static final int GL_POLYGON_BIT = 0x00000008;
    public static final int GL_VIEWPORT_BIT = 0x00000800;
    public static final int GL_TRANSFORM_BIT = 0x00001000;
    public static final int GL_ENABLE_BIT = 0x00002000;
    public static final int GL_TEXTURE_BIT = 0x00040000;
    public static final int GL_SCISSOR_BIT = 0x00080000;
    public static final int GL_ALL_ATTRIB_BITS = 0x000FFFFF;
    public static void glAlphaFunc(int func, float reference) {
        CompareFunc compare = compareFunc(func, "glAlphaFunc");
        if (recording() && state().record(Ops.ALPHA_FUNC, compare.ordinal(), reference)) {
            return;
        }
        state().alphaFunc(compare, reference);
    }
    // display lists

    public static int glGenLists(int count) {
        return state().lists().generate(count);
    }
    public static void glNewList(int list, int mode) {
        state().lists().onPlay(GL11C::play);
        state().lists().begin(list, mode);
    }
    public static void glEndList() {
        state().lists().end();
    }
    public static void glCallList(int list) {
        if (recording() && state().record(Ops.CALL_LIST, list)) {
            return;
        }
        state().lists().call(list);
    }
    public static void glDeleteLists(int list, int count) {
        state().lists().delete(list, count);
    }
    public static boolean glIsList(int list) {
        return state().lists().exists(list);
    }
    // matrices

    public static void glMatrixMode(int mode) {
        if (recording() && state().record(Ops.MATRIX_MODE, mode)) {
            return;
        }
        state().matrixMode(mode);
    }
    public static void glLoadIdentity() {
        if (recording() && state().record(Ops.LOAD_IDENTITY)) {
            return;
        }
        state().loadIdentity();
    }
    public static void glPushMatrix() {
        if (recording() && state().record(Ops.PUSH_MATRIX)) {
            return;
        }
        state().pushMatrix();
    }
    public static void glPopMatrix() {
        if (recording() && state().record(Ops.POP_MATRIX)) {
            return;
        }
        state().popMatrix();
    }
    public static void glTranslatef(float x, float y, float z) {
        if (recording() && state().record(Ops.TRANSLATE, x, y, z)) {
            return;
        }
        state().translate(x, y, z);
    }
    public static void glRotatef(float degrees, float x, float y, float z) {
        if (recording() && state().record(Ops.ROTATE, degrees, x, y, z)) {
            return;
        }
        state().rotate(degrees, x, y, z);
    }
    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float) x, (float) y, (float) z);
    }
    public static void glScaled(double x, double y, double z) {
        glScalef((float) x, (float) y, (float) z);
    }
    public static void glScalef(float x, float y, float z) {
        if (recording() && state().record(Ops.SCALE, x, y, z)) {
            return;
        }
        state().scale(x, y, z);
    }
    public static void glOrtho(double left, double right, double bottom, double top,
                               double near, double far) {
        float l = (float) left;
        float r = (float) right;
        float b = (float) bottom;
        float tp = (float) top;
        float n = (float) near;
        float f = (float) far;
        if (recording() && state().record(Ops.ORTHO, l, r, b, tp, n, f)) {
            return;
        }
        state().ortho(l, r, b, tp, n, f);
    }
    public static void glFrustum(double left, double right, double bottom, double top,
                                 double near, double far) {
        float l = (float) left;
        float r = (float) right;
        float b = (float) bottom;
        float tp = (float) top;
        float n = (float) near;
        float f = (float) far;
        if (recording() && state().record(Ops.FRUSTUM, l, r, b, tp, n, f)) {
            return;
        }
        state().frustum(l, r, b, tp, n, f);
    }
    /** the matrix is column-major, sixteen floats read from the buffer's current position. */
    public static void glLoadMatrix(FloatBuffer matrix) {
        loadMatrix(read16(matrix, "glLoadMatrix"));
    }
    public static void glLoadMatrixf(float[] matrix) {
        loadMatrix(require16(matrix, "glLoadMatrixf"));
    }
    public static void glMultMatrix(FloatBuffer matrix) {
        multMatrix(read16(matrix, "glMultMatrix"));
    }
    public static void glMultMatrixf(float[] matrix) {
        multMatrix(require16(matrix, "glMultMatrixf"));
    }
    // the attribute stack

    public static void glPushAttrib(int mask) {
        if (recording() && state().record(Ops.PUSH_ATTRIB, mask)) {
            return;
        }
        state().pushAttrib(mask);
    }
    public static void glPopAttrib() {
        if (recording() && state().record(Ops.POP_ATTRIB)) {
            return;
        }
        state().popAttrib();
    }
    // immediate mode

    public static void glBegin(int mode) {
        state().begin(primitive(mode));
    }
    public static void glEnd() {
        state().end();
    }
    public static void glVertex2f(float x, float y) {
        state().vertex(x, y, 0.0f);
    }
    public static void glVertex3f(float x, float y, float z) {
        state().vertex(x, y, z);
    }
    public static void glColor3f(float red, float green, float blue) {
        glColor4f(red, green, blue, 1.0f);
    }
    public static void glColor4f(float red, float green, float blue, float alpha) {
        if (recording() && !state().isBuilding()
                && state().record(Ops.COLOR, red, green, blue, alpha)) {
            return;
        }
        state().color(red, green, blue, alpha);
    }
    public static void glTexCoord2f(float s, float t) {
        texCoord(0, s, t);
    }
    public static void glNormal3f(float x, float y, float z) {
        if (recording() && !state().isBuilding()
                && state().record(Ops.NORMAL, x, y, z)) {
            return;
        }
        state().normal(x, y, z);
    }
    // lighting, materials and fog

    public static void glLightf(int light, int parameter, float value) {
        int index = lightIndex(light, "glLightf");
        if (recording() && state().record(Ops.LIGHT_SCALAR, index, parameter, value)) {
            return;
        }
        lightScalar(index, parameter, value);
    }
    public static void glLighti(int light, int parameter, int value) {
        glLightf(light, parameter, value);
    }
    public static void glLight(int light, int parameter, FloatBuffer values) {
        glLight(light, parameter, read4(values, "glLight"));
    }
    public static void glLightfv(int light, int parameter, float[] values) {
        glLight(light, parameter, require4(values, "glLightfv"));
    }
    private static void glLight(int light, int parameter, float[] values) {
        int index = lightIndex(light, "glLight");
        if (recording() && state().record(Ops.LIGHT_VECTOR, index, parameter,
                values[0], values[1], values[2], values[3])) {
            return;
        }
        lightVector(index, parameter, values);
    }
    public static void glLightModel(int parameter, FloatBuffer values) {
        glLightModel(parameter, read4(values, "glLightModel"));
    }
    public static void glLightModelfv(int parameter, float[] values) {
        glLightModel(parameter, require4(values, "glLightModelfv"));
    }
    private static void glLightModel(int parameter, float[] values) {
        if (parameter != GL_LIGHT_MODEL_AMBIENT) {
            throw new UnsupportedOperationException(
                    "glLightModel emulates GL_LIGHT_MODEL_AMBIENT only, got 0x"
                            + Integer.toHexString(parameter));
        }
        if (recording() && state().record(Ops.MODEL_AMBIENT,
                values[0], values[1], values[2], values[3])) {
            return;
        }
        state().lighting().setModelAmbient(values);
    }
    public static void glMaterialf(int face, int parameter, float value) {
        requireBothFaces(face, "glMaterialf");
        if (parameter != GL_SHININESS) {
            throw new UnsupportedOperationException(
                    "glMaterialf emulates GL_SHININESS only, got 0x"
                            + Integer.toHexString(parameter));
        }
        if (recording() && state().record(Ops.SHININESS, value)) {
            return;
        }
        state().lighting().setShininess(value);
    }
    public static void glMateriali(int face, int parameter, int value) {
        glMaterialf(face, parameter, value);
    }
    public static void glMaterial(int face, int parameter, FloatBuffer values) {
        glMaterial(face, parameter, read4(values, "glMaterial"));
    }
    public static void glMaterialfv(int face, int parameter, float[] values) {
        glMaterial(face, parameter, require4(values, "glMaterialfv"));
    }
    private static void glMaterial(int face, int parameter, float[] values) {
        requireBothFaces(face, "glMaterial");
        if (parameter == GL_SHININESS) {
            glMaterialf(face, parameter, values[0]);
            return;
        }
        if (recording() && state().record(Ops.MATERIAL, parameter,
                values[0], values[1], values[2], values[3])) {
            return;
        }
        material(parameter, values);
    }
    public static void glColorMaterial(int face, int mode) {
        requireBothFaces(face, "glColorMaterial");
        if (mode != GL_AMBIENT_AND_DIFFUSE) {
            throw new UnsupportedOperationException(
                    "glColorMaterial emulates GL_AMBIENT_AND_DIFFUSE only, got 0x"
                            + Integer.toHexString(mode));
        }
    }
    public static void glShadeModel(int model) {
        if (model != GL_SMOOTH && model != GL_FLAT) {
            throw new IllegalArgumentException(
                    "glShadeModel takes GL_SMOOTH or GL_FLAT, got 0x" + Integer.toHexString(model));
        }
        if (recording() && state().record(Ops.SHADE_MODEL, model == GL_FLAT ? 1 : 0)) {
            return;
        }
        state().lighting().setFlatShading(model == GL_FLAT);
    }
    public static void glFogf(int parameter, float value) {
        if (recording() && state().record(Ops.FOG, parameter, value)) {
            return;
        }
        fog(parameter, value);
    }
    public static void glFogi(int parameter, int value) {
        glFogf(parameter, value);
    }
    public static void glFog(int parameter, FloatBuffer values) {
        glFog(parameter, read4(values, "glFog"));
    }
    public static void glFogfv(int parameter, float[] values) {
        glFog(parameter, require4(values, "glFogfv"));
    }
    private static void glFog(int parameter, float[] values) {
        if (parameter != GL_FOG_COLOR) {
            glFogf(parameter, values[0]);
            return;
        }
        if (recording() && state().record(Ops.FOG_COLOR,
                values[0], values[1], values[2], values[3])) {
            return;
        }
        state().lighting().setFogColor(values);
    }
    // vertex arrays

    public static void glEnableClientState(int array) {
        clientArray(array, "glEnableClientState").setEnabled(true);
    }
    public static void glDisableClientState(int array) {
        clientArray(array, "glDisableClientState").setEnabled(false);
    }
    public static void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().position().pointAt(pointer, size, AttributeType.FLOAT, false, stride);
    }
    public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
        state().arrays().position().pointAt(pointer, size,
                componentType(type, "glVertexPointer"), false, stride);
    }
    public static void glVertexPointer(int size, int type, int stride, long offset) {
        pointIntoBuffer(state().arrays().position(), size, componentType(type, "glVertexPointer"),
                false, stride, offset, "glVertexPointer");
    }
    public static void glColorPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().color().pointAt(pointer, size, AttributeType.FLOAT, false, stride);
    }
    /** the byte form is always normalised: gl reads integer color components as 0..1. */
    public static void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {
        AttributeType type = unsigned ? AttributeType.UNSIGNED_BYTE : AttributeType.BYTE;
        state().arrays().color().pointAt(pointer, size, type, true, stride);
    }
    public static void glColorPointer(int size, int type, int stride, ByteBuffer pointer) {
        AttributeType component = componentType(type, "glColorPointer");
        state().arrays().color().pointAt(pointer, size, component,
                component != AttributeType.FLOAT, stride);
    }
    public static void glColorPointer(int size, int type, int stride, long offset) {
        AttributeType component = componentType(type, "glColorPointer");
        pointIntoBuffer(state().arrays().color(), size, component,
                component != AttributeType.FLOAT, stride, offset, "glColorPointer");
    }
    public static void glNormalPointer(int stride, FloatBuffer pointer) {
        state().arrays().normal().pointAt(pointer, 3, AttributeType.FLOAT, false, stride);
    }
    public static void glNormalPointer(int type, int stride, ByteBuffer pointer) {
        state().arrays().normal().pointAt(pointer, 3,
                componentType(type, "glNormalPointer"), false, stride);
    }
    public static void glNormalPointer(int type, int stride, long offset) {
        pointIntoBuffer(state().arrays().normal(), 3, componentType(type, "glNormalPointer"),
                false, stride, offset, "glNormalPointer");
    }
    public static void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        state().arrays().texCoord(state().clientActiveTexture()).pointAt(pointer, size,
                AttributeType.FLOAT, false, stride);
    }
    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
        state().arrays().texCoord(state().clientActiveTexture()).pointAt(pointer, size,
                componentType(type, "glTexCoordPointer"), false, stride);
    }
    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        pointIntoBuffer(state().arrays().texCoord(state().clientActiveTexture()), size,
                componentType(type, "glTexCoordPointer"), false, stride, offset,
                "glTexCoordPointer");
    }
    private static ClientArray clientArray(int array, String call) {
        VertexArrays arrays = state().arrays();
        return switch (array) {
            case GL_VERTEX_ARRAY -> arrays.position();
            case GL_COLOR_ARRAY -> arrays.color();
            case GL_TEXTURE_COORD_ARRAY -> arrays.texCoord(state().clientActiveTexture());
            case GL_NORMAL_ARRAY -> arrays.normal();
            default -> throw new UnsupportedOperationException(
                    call + " does not yet emulate array 0x" + Integer.toHexString(array));
        };
    }
    /**
     * the texture environment decides how a unit's sample is combined with what came before it.
     * GL_MODULATE, GL_REPLACE and GL_ADD are the modes the compatibility shader implements.
     */
    public static void glTexEnvi(int target, int name, int value) {
        if (target != GL_TEXTURE_ENV) {
            throw new UnsupportedOperationException(
                    "glTexEnvi emulates GL_TEXTURE_ENV only, got 0x" + Integer.toHexString(target));
        }
        if (name != GL_TEXTURE_ENV_MODE) {
            throw new UnsupportedOperationException(
                    "glTexEnvi does not emulate parameter 0x" + Integer.toHexString(name));
        }
        int mode = switch (value) {
            case GL_MODULATE -> 0;
            case GL_REPLACE -> 1;
            case GL_ADD -> 2;
            default -> throw new UnsupportedOperationException(
                    "glTexEnvi does not emulate mode 0x" + Integer.toHexString(value)
                            + "; GL_MODULATE, GL_REPLACE and GL_ADD are the modes the "
                            + "compatibility shader implements");
        };
        if (recording() && state().record(Ops.TEXTURE_ENV, mode)) {
            return;
        }
        state().setTextureEnv(mode);
    }
    public static void glTexEnvf(int target, int name, float value) {
        glTexEnvi(target, name, (int) value);
    }
    /** the buffer form carries a colour for GL_TEXTURE_ENV_COLOR, which the shader ignores. */
    public static void glTexEnv(int target, int name, FloatBuffer values) {
        if (name == GL_TEXTURE_ENV_MODE) {
            glTexEnvi(target, name, (int) values.get(values.position()));
            return;
        }
        throw new UnsupportedOperationException(
                "glTexEnv does not emulate parameter 0x" + Integer.toHexString(name)
                        + "; the compatibility shader combines a sample with GL_TEXTURE_ENV_MODE "
                        + "alone");
    }
    /**
     * coordinate generation has no shader behind it: every texture coordinate here comes from the
     * coordinate array or glTexCoord.
     */
    public static void glTexGeni(int coordinate, int name, int value) {
        throw new UnsupportedOperationException(
                "glTexGeni is not emulated: webgl generates no texture coordinates, so the "
                        + "coordinate array or glTexCoord has to carry them");
    }
    public static void glTexGen(int coordinate, int name, FloatBuffer values) {
        glTexGeni(coordinate, name, 0);
    }
    /** webgl has no logic op stage; blending is the only way it combines with the framebuffer. */
    public static void glLogicOp(int operation) {
        throw new UnsupportedOperationException(
                "glLogicOp is not emulated: webgl combines a fragment with the framebuffer by "
                        + "blending only");
    }
    /** builds every level below the base from it. */
    public static void glGenerateMipmap(int target) {
        requireTexture2D(target, "glGenerateMipmap");
        state().refuseWhileRecording("glGenerateMipmap");
        device().generateMipmap();
    }
}
