package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.ShaderStage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * the context state gl keeps internally and exposes only through its setters.
 *
 * <p>a singleton because gl's api is a singleton; {@link #attach} is called once the display
 * exists and {@link #detach} when it is destroyed.
 */
public final class GLState implements RenderState {

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
    private DrawMode primitive;
    private boolean building;

    private final VertexArrays arrays = new VertexArrays();
    private final GenericArrays attributes;
    private final ShaderPipeline shaders;
    private final Map<Integer, ShaderStage> shaderStages = new HashMap<>();
    private int program = GraphicsDevice.NO_PROGRAM;
    private int arrayBuffer;
    private int elementBuffer;

    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float alpha = 1.0f;
    private float s;
    private float t;

    private GLState(GraphicsDevice device) {
        this.device = device;
        this.pipeline = new FixedFunctionPipeline(device);
        this.attributes = new GenericArrays(device.maxAttributes());
        this.shaders = new ShaderPipeline(device, device.maxAttributes());
    }

    public static void attach(GraphicsDevice device) {
        current = new GLState(device);
    }

    public static void detach() {
        if (current != null) {
            current.pipeline.dispose();
            current.shaders.dispose();
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

    @Override
    public float[] modelView() {
        return modelView;
    }

    @Override
    public float[] projection() {
        return projection;
    }

    // immediate mode

    public void begin(DrawMode mode) {
        if (building) {
            throw new IllegalStateException("glBegin called inside glBegin/glEnd");
        }
        if (program != GraphicsDevice.NO_PROGRAM) {
            throw new UnsupportedOperationException(
                    "glBegin draws through the fixed-function pipeline, which the program bound by "
                            + "glUseProgram has replaced. call glUseProgram(0) first, or feed the "
                            + "program a vertex buffer of your own");
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
        pipeline.drawImmediate(primitive, vertices, floatCount, vertexCount, this);
        restoreBindings();
    }

    // buffer objects and vertex arrays

    public VertexArrays arrays() {
        return arrays;
    }

    public int createBuffer() {
        return device.createBuffer();
    }

    public void deleteBuffer(int buffer) {
        if (buffer == GraphicsDevice.NO_BUFFER) {
            return;
        }
        // gl unbinds a deleted buffer from every target it was bound to
        if (arrayBuffer == buffer) {
            arrayBuffer = GraphicsDevice.NO_BUFFER;
        }
        if (elementBuffer == buffer) {
            elementBuffer = GraphicsDevice.NO_BUFFER;
        }
        device.deleteBuffer(buffer);
    }

    public void bindBuffer(BufferTarget target, int buffer) {
        if (target == BufferTarget.ELEMENT) {
            elementBuffer = buffer;
        } else {
            arrayBuffer = buffer;
        }
        device.bindBuffer(target, buffer);
    }

    public int boundBuffer(BufferTarget target) {
        return target == BufferTarget.ELEMENT ? elementBuffer : arrayBuffer;
    }

    public void drawArrays(DrawMode mode, int first, int count) {
        requireNotBuilding("glDrawArrays");
        if (program != GraphicsDevice.NO_PROGRAM) {
            shaders.drawArrays(mode, first, count, attributes);
        } else {
            pipeline.drawArrays(mode, first, count, arrays, this);
        }
        restoreBindings();
    }

    public void drawElements(DrawMode mode, int count, IndexType indexType, int offsetInBytes) {
        requireNotBuilding("glDrawElements");
        if (elementBuffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glDrawElements needs indices in a buffer object; webgl has no client-side "
                            + "index arrays, so bind one with glBindBuffer first");
        }
        if (program != GraphicsDevice.NO_PROGRAM) {
            shaders.drawElements(mode, count, indexType, offsetInBytes, attributes);
        } else {
            pipeline.drawElements(mode, count, indexType, offsetInBytes, arrays, this);
        }
        restoreBindings();
    }

    // shader programs

    /**
     * the bound program decides which pipeline a draw goes through: gl's own state feeds the
     * fixed-function shader, and a program of the application's replaces it wholesale.
     */
    public void useProgram(int newProgram) {
        program = newProgram;
        device.useProgram(newProgram);
    }

    public int program() {
        return program;
    }

    public GenericArrays attributes() {
        return attributes;
    }

    /**
     * which stage a shader name was created for
     */
    public void trackShader(int shader, ShaderStage stage) {
        shaderStages.put(shader, stage);
    }

    public ShaderStage shaderStage(int shader) {
        ShaderStage stage = shaderStages.get(shader);
        if (stage == null) {
            throw new IllegalArgumentException(
                    "not a shader created by glCreateShader: " + shader);
        }
        return stage;
    }

    public void forgetShader(int shader) {
        shaderStages.remove(shader);
    }

    private void requireNotBuilding(String call) {
        if (building) {
            throw new IllegalStateException(call + " called inside glBegin/glEnd");
        }
    }

    // the pipeline binds its own staging and index buffers to draw; the application's bindings
    // are gl state and have to survive that
    private void restoreBindings() {
        device.bindBuffer(BufferTarget.ARRAY, arrayBuffer);
        device.bindBuffer(BufferTarget.ELEMENT, elementBuffer);
    }

    // capability state

    public void setTextureEnabled(boolean enabled) {
        textureEnabled = enabled;
    }

    @Override
    public boolean isTextureEnabled() {
        return textureEnabled;
    }

    public void setAlphaTestEnabled(boolean enabled) {
        alphaTestEnabled = enabled;
    }

    @Override
    public boolean isAlphaTestEnabled() {
        return alphaTestEnabled;
    }

    @Override
    public float alphaTestThreshold() {
        return alphaTestThreshold;
    }

    @Override
    public float red() {
        return red;
    }

    @Override
    public float green() {
        return green;
    }

    @Override
    public float blue() {
        return blue;
    }

    @Override
    public float alpha() {
        return alpha;
    }

    @Override
    public float s() {
        return s;
    }

    @Override
    public float t() {
        return t;
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
