package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.Primitive;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * the context state gl keeps internally and exposes only through its setters.
 *
 * <p>a singleton because gl's api is a singleton; {@link #attach} is called once the display
 * exists and {@link #detach} when it is destroyed.
 */
public final class GLState {

    public static final int MODELVIEW = 0x1700;
    public static final int PROJECTION = 0x1701;

    private static GLState current;

    private final GraphicsDevice device;
    private final FixedFunctionPipeline pipeline;

    private final Deque<float[]> modelViewStack = new ArrayDeque<>();
    private final Deque<float[]> projectionStack = new ArrayDeque<>();
    private float[] modelView = Matrix4.identity();
    private float[] projection = Matrix4.identity();
    private int matrixMode = MODELVIEW;

    private boolean textureEnabled;
    private boolean alphaTestEnabled;
    private float alphaTestThreshold;
    private CompareFunc alphaTestFunc = CompareFunc.ALWAYS;

    private float[] vertices = new float[FixedFunctionPipeline.VERTEX_FLOATS * 256];
    private int floatCount;
    private int vertexCount;
    private Primitive primitive;
    private boolean building;

    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float alpha = 1.0f;
    private float s;
    private float t;

    private GLState(GraphicsDevice device) {
        this.device = device;
        this.pipeline = new FixedFunctionPipeline(device);
    }

    public static void attach(GraphicsDevice device) {
        current = new GLState(device);
    }

    public static void detach() {
        if (current != null) {
            current.pipeline.dispose();
            current = null;
        }
    }

    public static boolean isAttached() {
        return current != null;
    }

    public static GLState get() {
        if (current == null) {
            throw new IllegalStateException(
                    "no gl context; Display.create() must be called before any GL call");
        }
        return current;
    }

    public GraphicsDevice device() {
        return device;
    }

    // matrices

    public void matrixMode(int mode) {
        if (mode != MODELVIEW && mode != PROJECTION) {
            throw new IllegalArgumentException(
                    "unsupported matrix mode 0x" + Integer.toHexString(mode)
                            + "; only GL_MODELVIEW and GL_PROJECTION are emulated");
        }
        matrixMode = mode;
    }

    private float[] active() {
        return matrixMode == PROJECTION ? projection : modelView;
    }

    private void setActive(float[] matrix) {
        if (matrixMode == PROJECTION) {
            projection = matrix;
        } else {
            modelView = matrix;
        }
    }

    public void pushMatrix() {
        stack().push(Matrix4.copy(active()));
    }

    public void popMatrix() {
        Deque<float[]> stack = stack();
        if (stack.isEmpty()) {
            throw new IllegalStateException("matrix stack underflow in "
                    + (matrixMode == PROJECTION ? "GL_PROJECTION" : "GL_MODELVIEW"));
        }
        setActive(stack.pop());
    }

    private Deque<float[]> stack() {
        return matrixMode == PROJECTION ? projectionStack : modelViewStack;
    }

    public void loadIdentity() {
        Matrix4.setIdentity(active());
    }

    public void translate(float x, float y, float z) {
        Matrix4.translate(active(), x, y, z);
    }

    public void rotate(float degrees, float x, float y, float z) {
        Matrix4.rotate(active(), degrees, x, y, z);
    }

    public void scale(float x, float y, float z) {
        Matrix4.scale(active(), x, y, z);
    }

    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        Matrix4.ortho(active(), left, right, bottom, top, near, far);
    }

    public void frustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix4.frustum(active(), left, right, bottom, top, near, far);
    }

    public void multMatrix(float[] matrix) {
        Matrix4.multiply(active(), matrix);
    }

    public float[] modelView() {
        return modelView;
    }

    public float[] projection() {
        return projection;
    }

    // immediate mode

    public void begin(Primitive mode) {
        if (building) {
            throw new IllegalStateException("glBegin called inside glBegin/glEnd");
        }
        building = true;
        primitive = mode;
        floatCount = 0;
        vertexCount = 0;
    }

    public void color(float r, float g, float b, float a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    public void texCoord(float newS, float newT) {
        s = newS;
        t = newT;
    }

    public void vertex(float x, float y, float z) {
        if (!building) {
            throw new IllegalStateException("glVertex called outside glBegin/glEnd");
        }
        if (floatCount + FixedFunctionPipeline.VERTEX_FLOATS > vertices.length) {
            float[] grown = new float[vertices.length * 2];
            System.arraycopy(vertices, 0, grown, 0, floatCount);
            vertices = grown;
        }
        vertices[floatCount++] = x;
        vertices[floatCount++] = y;
        vertices[floatCount++] = z;
        vertices[floatCount++] = red;
        vertices[floatCount++] = green;
        vertices[floatCount++] = blue;
        vertices[floatCount++] = alpha;
        vertices[floatCount++] = s;
        vertices[floatCount++] = t;
        vertexCount++;
    }

    public void end() {
        if (!building) {
            throw new IllegalStateException("glEnd called without glBegin");
        }
        building = false;
        if (vertexCount == 0) {
            return;
        }
        pipeline.draw(primitive, vertices, floatCount, vertexCount, modelView, projection,
                textureEnabled, alphaTestEnabled, alphaTestThreshold);
    }

    // capability state

    public void setTextureEnabled(boolean enabled) {
        textureEnabled = enabled;
    }

    public boolean isTextureEnabled() {
        return textureEnabled;
    }

    public void setAlphaTestEnabled(boolean enabled) {
        alphaTestEnabled = enabled;
    }

    /**
     * the shader implements GL_GREATER, which is what desktop code uses the alpha test for.
     * any other comparison would silently render the wrong thing, so it is rejected instead.
     */
    public void alphaFunc(CompareFunc func, float reference) {
        if (func != CompareFunc.GREATER) {
            throw new UnsupportedOperationException(
                    "glAlphaFunc emulates GL_GREATER only, got " + func);
        }
        alphaTestFunc = func;
        alphaTestThreshold = reference;
    }

    public CompareFunc alphaTestFunc() {
        return alphaTestFunc;
    }

    public void setBlendFunc(BlendFactor source, BlendFactor destination) {
        device.setBlendFunc(source, destination);
    }
}
