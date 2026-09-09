package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendEquation;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.Winding;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * the context state gl keeps internally and exposes only through its setters.
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

    private final boolean[] textureEnabled = new boolean[VertexArrays.UNITS];
    private final int[] textureEnv = new int[VertexArrays.UNITS];
    private int activeTexture;
    private int clientActiveTexture;
    private boolean alphaTestEnabled;
    private float alphaTestThreshold;
    private CompareFunc alphaTestFunc = CompareFunc.ALWAYS;

    private boolean normalize;
    private boolean rescaleNormal;

    private boolean depthTest;
    private boolean blend;
    private boolean cullFaceEnabled;
    private boolean scissorTest;
    private float clearRed;
    private float clearGreen;
    private float clearBlue;
    private float clearAlpha;
    private double clearDepthValue = 1.0;
    private boolean maskRed = true;
    private boolean maskGreen = true;
    private boolean maskBlue = true;
    private boolean maskAlpha = true;
    private boolean depthWrite = true;
    private CompareFunc depthFunc = CompareFunc.LESS;
    private BlendFactor blendSource = BlendFactor.ONE;
    private BlendFactor blendDestination = BlendFactor.ZERO;
    private BlendFactor blendSourceAlpha = BlendFactor.ONE;
    private BlendFactor blendDestinationAlpha = BlendFactor.ZERO;
    private BlendEquation blendEquation = BlendEquation.ADD;
    private BlendEquation blendEquationAlpha = BlendEquation.ADD;
    private final float[] blendColor = new float[4];
    private CullFace culledFaces = CullFace.BACK;
    private Winding frontFace = Winding.COUNTER_CLOCKWISE;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scissorX;
    private int scissorY;
    private int scissorWidth;
    private int scissorHeight;
    private final int[] boundTexture = new int[VertexArrays.UNITS];
    private final java.util.Set<Integer> automaticMipmaps = new java.util.HashSet<>();

    private final Deque<Attributes> attributeStack = new ArrayDeque<>();
    private final DisplayLists displayLists = new DisplayLists();

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
    private int vertexArray;

    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float alpha = 1.0f;
    private final float[] s = new float[VertexArrays.UNITS];
    private final float[] t = new float[VertexArrays.UNITS];
    private float pointSize = 1.0f;
    private float pointSizeMin;
    private float pointSizeMax = Float.MAX_VALUE;
    private final Lighting lighting = new Lighting();
    private float normalX;
    private float normalY;
    private float normalZ = 1.0f;

    private GLState(GraphicsDevice device) {
        this.device = device;
        this.pipeline = new FixedFunctionPipeline(device);
        this.attributes = new GenericArrays(device.maxAttributes());
        this.shaders = new ShaderPipeline(device, device.maxAttributes());
        this.viewportWidth = device.width();
        this.viewportHeight = device.height();
        this.scissorWidth = viewportWidth;
        this.scissorHeight = viewportHeight;
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

    public void texCoord(int unit, float newS, float newT) {
        s[VertexArrays.requireUnit(unit)] = newS;
        t[unit] = newT;
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
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            vertices[floatCount++] = s[unit];
            vertices[floatCount++] = t[unit];
        }
        vertices[floatCount++] = normalX;
        vertices[floatCount++] = normalY;
        vertices[floatCount++] = normalZ;
        vertexCount++;
    }

    public boolean isBuilding() {
        return building;
    }

    public void end() {
        if (!building) {
            throw new IllegalStateException("glEnd called without glBegin");
        }
        building = false;
        if (vertexCount == 0) {
            return;
        }
        // a list holds the vertices the block built, not the calls that built them
        if (displayLists.isRecording()) {
            float[] captured = java.util.Arrays.copyOf(vertices, floatCount);
            DrawMode mode = primitive;
            int count = vertexCount;
            if (record(() -> {
                pipeline.drawImmediate(mode, captured, captured.length, count, this);
                restoreBindings();
            })) {
                return;
            }
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

    public int createFramebuffer() {
        return device.createFramebuffer();
    }

    public void bindFramebuffer(int framebuffer) {
        device.bindFramebuffer(framebuffer);
    }

    public int boundFramebuffer() {
        return device.boundFramebuffer();
    }

    public void deleteFramebuffer(int framebuffer) {
        if (framebuffer == GraphicsDevice.NO_FRAMEBUFFER) {
            return;
        }
        device.deleteFramebuffer(framebuffer);
    }

    public int createVertexArray() {
        return device.createVertexArray();
    }

    public void bindVertexArray(int array) {
        vertexArray = array;
        device.bindVertexArray(array);
    }

    public int boundVertexArray() {
        return vertexArray;
    }

    public void deleteVertexArray(int array) {
        if (array == 0) {
            return;
        }
        if (vertexArray == array) {
            vertexArray = 0;
        }
        device.deleteVertexArray(array);
    }

    public void drawArraysInstanced(DrawMode mode, int first, int count, int instances) {
        requireNotBuilding("glDrawArraysInstanced");
        requireProgram("glDrawArraysInstanced");
        shaders.drawArraysInstanced(mode, first, count, instances, attributes);
        restoreBindings();
    }

    public void drawElementsInstanced(DrawMode mode, int count, IndexType indexType,
                                      int offsetInBytes, int instances) {
        requireNotBuilding("glDrawElementsInstanced");
        requireProgram("glDrawElementsInstanced");
        if (elementBuffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glDrawElementsInstanced needs indices in a buffer object; bind one with "
                            + "glBindBuffer first");
        }
        shaders.drawElementsInstanced(mode, count, indexType, offsetInBytes, instances, attributes);
        restoreBindings();
    }

    private void requireProgram(String call) {
        if (program == GraphicsDevice.NO_PROGRAM) {
            throw new UnsupportedOperationException(
                    call + " draws instances of attributes a program reads; bind one with "
                            + "glUseProgram first");
        }
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
        textureEnabled[activeTexture] = enabled;
    }

    @Override
    public boolean isTextureEnabled(int unit) {
        return textureEnabled[unit];
    }

    public void setTextureEnv(int mode) {
        textureEnv[activeTexture] = mode;
    }

    @Override
    public int textureEnv(int unit) {
        return textureEnv[unit];
    }

    public void activeTexture(int unit) {
        activeTexture = VertexArrays.requireUnit(unit);
        device.activeTexture(unit);
    }

    public int activeTexture() {
        return activeTexture;
    }

    public void clientActiveTexture(int unit) {
        clientActiveTexture = VertexArrays.requireUnit(unit);
    }

    public int clientActiveTexture() {
        return clientActiveTexture;
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
    public float s(int unit) {
        return s[unit];
    }

    @Override
    public float t(int unit) {
        return t[unit];
    }

    @Override
    public Lighting lighting() {
        return lighting;
    }

    /** the normal gl carries until the next glNormal3f, in object space. */
    public void normal(float x, float y, float z) {
        normalX = x;
        normalY = y;
        normalZ = z;
    }

    @Override
    public float normalX() {
        return normalX;
    }

    @Override
    public float normalY() {
        return normalY;
    }

    @Override
    public float normalZ() {
        return normalZ;
    }

    public void pointSize(float size) {
        pointSize = size;
    }

    /** the clamp gl applies to a point's size, not the size itself. */
    public void setPointSizeMin(float size) {
        pointSizeMin = size;
    }

    public void setPointSizeMax(float size) {
        pointSizeMax = size;
    }

    public float pointSizeMin() {
        return pointSizeMin;
    }

    public float pointSizeMax() {
        return pointSizeMax;
    }

    @Override
    public float pointSize() {
        return Math.min(Math.max(pointSize, pointSizeMin), pointSizeMax);
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
        blendSource = source;
        blendDestination = destination;
        device.setBlendFunc(source, destination);
    }

    public void setBlendFuncSeparate(BlendFactor sourceColor, BlendFactor destinationColor,
                                     BlendFactor sourceAlpha, BlendFactor destinationAlpha) {
        blendSource = sourceColor;
        blendDestination = destinationColor;
        blendSourceAlpha = sourceAlpha;
        blendDestinationAlpha = destinationAlpha;
        device.setBlendFuncSeparate(sourceColor, destinationColor, sourceAlpha, destinationAlpha);
    }

    public BlendFactor blendSourceAlpha() {
        return blendSourceAlpha;
    }

    public BlendFactor blendDestinationAlpha() {
        return blendDestinationAlpha;
    }

    public void setBlendEquation(BlendEquation equation) {
        blendEquation = equation;
        blendEquationAlpha = equation;
        device.setBlendEquation(equation);
    }

    public void setBlendEquationSeparate(BlendEquation color, BlendEquation alpha) {
        blendEquation = color;
        blendEquationAlpha = alpha;
        device.setBlendEquationSeparate(color, alpha);
    }

    public BlendEquation blendEquation() {
        return blendEquation;
    }

    public BlendEquation blendEquationAlpha() {
        return blendEquationAlpha;
    }

    public void setBlendColor(float red, float green, float blue, float alpha) {
        blendColor[0] = red;
        blendColor[1] = green;
        blendColor[2] = blue;
        blendColor[3] = alpha;
        device.setBlendColor(red, green, blue, alpha);
    }

    public float[] blendColor() {
        return blendColor.clone();
    }

    // mirrored server state

    public void setDepthTest(boolean enabled) {
        depthTest = enabled;
        device.setDepthTest(enabled);
    }

    public void setBlend(boolean enabled) {
        blend = enabled;
        device.setBlend(enabled);
    }

    public void setCullFace(boolean enabled) {
        cullFaceEnabled = enabled;
        device.setCullFace(enabled);
    }

    public void setScissorTest(boolean enabled) {
        scissorTest = enabled;
        device.setScissorTest(enabled);
    }

    public void setCulledFaces(CullFace faces) {
        culledFaces = faces;
        device.setCulledFaces(faces);
    }

    public void setFrontFace(Winding winding) {
        frontFace = winding;
        device.setFrontFace(winding);
    }

    public void setDepthFunc(CompareFunc func) {
        depthFunc = func;
        device.setDepthFunc(func);
    }

    public void setDepthWrite(boolean enabled) {
        depthWrite = enabled;
        device.setDepthWrite(enabled);
    }

    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        maskRed = red;
        maskGreen = green;
        maskBlue = blue;
        maskAlpha = alpha;
        device.setColorMask(red, green, blue, alpha);
    }

    public void clearColor(float red, float green, float blue, float alpha) {
        clearRed = red;
        clearGreen = green;
        clearBlue = blue;
        clearAlpha = alpha;
        device.clearColor(red, green, blue, alpha);
    }

    public void clearDepth(double depth) {
        clearDepthValue = depth;
        device.clearDepth(depth);
    }

    public void viewport(int x, int y, int width, int height) {
        viewportX = x;
        viewportY = y;
        viewportWidth = width;
        viewportHeight = height;
        device.viewport(x, y, width, height);
    }

    public void setScissor(int x, int y, int width, int height) {
        scissorX = x;
        scissorY = y;
        scissorWidth = width;
        scissorHeight = height;
        device.setScissor(x, y, width, height);
    }

    public void bindTexture(int texture) {
        boundTexture[activeTexture] = texture;
        device.bindTexture(activeTexture, texture);
    }

    public int boundTexture() {
        return boundTexture[activeTexture];
    }

    public void setAutomaticMipmaps(boolean enabled) {
        if (enabled) {
            automaticMipmaps.add(boundTexture());
        } else {
            automaticMipmaps.remove(boundTexture());
        }
    }

    public boolean automaticMipmaps() {
        return automaticMipmaps.contains(boundTexture());
    }

    public void deleteTexture(int texture) {
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (boundTexture[unit] == texture) {
                boundTexture[unit] = GraphicsDevice.NO_TEXTURE;
            }
        }
        automaticMipmaps.remove(texture);
        device.deleteTexture(texture);
    }

    void restoreAlphaFunc(CompareFunc func, float reference) {
        alphaTestFunc = func;
        alphaTestThreshold = reference;
    }

    // the attribute stack

    public void pushAttrib(int mask) {
        attributeStack.push(new Attributes(mask,
                depthTest, blend, cullFaceEnabled, scissorTest, textureEnabled[activeTexture],
                alphaTestEnabled,
                lighting.isEnabled(), lighting.isFogEnabled(), lighting.isColorMaterial(),
                lighting.lightEnabledFlagsCopy(),
                normalX, normalY, normalZ,
                clearRed, clearGreen, clearBlue, clearAlpha, clearDepthValue,
                maskRed, maskGreen, maskBlue, maskAlpha, depthWrite,
                depthFunc, blendSource, blendDestination, alphaTestFunc, alphaTestThreshold,
                culledFaces, frontFace,
                viewportX, viewportY, viewportWidth, viewportHeight,
                scissorX, scissorY, scissorWidth, scissorHeight,
                red, green, blue, alpha, s[0], t[0], matrixMode, boundTexture[activeTexture]));
    }

    public void popAttrib() {
        if (attributeStack.isEmpty()) {
            throw new IllegalStateException("glPopAttrib called with an empty attribute stack");
        }
        attributeStack.pop().restore(this);
    }

    // display lists

    public DisplayLists lists() {
        return displayLists;
    }

    /**
     * @return true when the call belongs in the list being recorded and must not also run now
     */
    public boolean record(Runnable call) {
        return displayLists.record(call);
    }

    public boolean isRecording() {
        return displayLists.isRecording();
    }

    public void refuseWhileRecording(String call) {
        if (displayLists.isRecording()) {
            throw new UnsupportedOperationException(
                    call + " cannot be compiled into a display list; call it outside "
                            + "glNewList/glEndList");
        }
    }

    // queries

    public void setNormalRescaling(int capability, boolean enabled) {
        if (capability == 0x0BA1) {
            normalize = enabled;
        } else {
            rescaleNormal = enabled;
        }
    }

    public boolean isEnabled(int capability) {
        return switch (capability) {
            case 0x0BA1 -> normalize;
            case 0x803A -> rescaleNormal;
            case 0x0B71 -> depthTest;
            case 0x0BE2 -> blend;
            case 0x0B44 -> cullFaceEnabled;
            case 0x0C11 -> scissorTest;
            case 0x0DE1 -> textureEnabled[activeTexture];
            case 0x0BC0 -> alphaTestEnabled;
            case 0x0B50 -> lighting.isEnabled();
            case 0x0B57 -> lighting.isColorMaterial();
            case 0x0B60 -> lighting.isFogEnabled();
            case 0x4000, 0x4001, 0x4002, 0x4003, 0x4004, 0x4005, 0x4006, 0x4007 ->
                    lighting.isLightEnabled(capability - 0x4000);
            default -> throw new UnsupportedOperationException(
                    "glIsEnabled does not emulate capability 0x" + Integer.toHexString(capability));
        };
    }

    public int[] viewportBox() {
        return new int[] {viewportX, viewportY, viewportWidth, viewportHeight};
    }

    public int[] scissorBox() {
        return new int[] {scissorX, scissorY, scissorWidth, scissorHeight};
    }

    public float[] clearColorValue() {
        return new float[] {clearRed, clearGreen, clearBlue, clearAlpha};
    }

    public float[] currentColor() {
        return new float[] {red, green, blue, alpha};
    }

    public int matrixMode() {
        return matrixMode;
    }

    public CompareFunc depthFunc() {
        return depthFunc;
    }

    public BlendFactor blendSource() {
        return blendSource;
    }

    public BlendFactor blendDestination() {
        return blendDestination;
    }

    public CullFace culledFaces() {
        return culledFaces;
    }

    public Winding frontFace() {
        return frontFace;
    }

    public boolean depthWrite() {
        return depthWrite;
    }

    public double clearDepthValue() {
        return clearDepthValue;
    }

    // matrix loading

    public void loadMatrix(float[] matrix) {
        System.arraycopy(matrix, 0, active(), 0, 16);
    }

    public void multMatrix(float[] matrix) {
        Matrix4.multiply(active(), matrix);
    }
}
