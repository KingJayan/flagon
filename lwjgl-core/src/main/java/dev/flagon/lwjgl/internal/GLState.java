package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BlendEquation;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsError;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.StencilOp;
import dev.flagon.platform.graphics.Winding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/* internal gl context state */
public final class GLState implements RenderState {

    public static final int MODELVIEW = 0x1700;
    public static final int PROJECTION = 0x1701;
    public static final int TEXTURE = 0x1702;

    private static GLState current;

    private final GraphicsDevice device;
    private final FixedFunctionPipeline pipeline;

    private final MatrixStack modelViewStack = new MatrixStack();
    private final MatrixStack projectionStack = new MatrixStack();
    private final float[][] textureMatrices = new float[VertexArrays.UNITS][];
    private final MatrixStack[] textureStacks = new MatrixStack[VertexArrays.UNITS];
    private final float[] modelView = Matrix4.identity();
    private final float[] projection = Matrix4.identity();
    private int matrixMode = MODELVIEW;

    private final boolean[] textureEnabled = new boolean[VertexArrays.UNITS];
    private int activeTexture;
    private int clientActiveTexture;
    private boolean alphaTestEnabled;
    private float alphaTestThreshold;
    private static final CompareFunc[] COMPARES = CompareFunc.values();
    private static final BlendFactor[] FACTORS = BlendFactor.values();
    private static final BlendEquation[] EQUATIONS = BlendEquation.values();
    private static final CullFace[] CULL_FACES = CullFace.values();
    private static final Winding[] WINDINGS = Winding.values();
    private static final StencilOp[] STENCIL_OPS = StencilOp.values();

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

    private static final int PIXEL_STORE_FIRST = 0x0CF0;
    private static final int PIXEL_STORE_NAMES = 0x0D05 - PIXEL_STORE_FIRST + 1;

    private static final int[] NO_LEVEL = new int[] {0, 0};

    private final List<Texture> textures = new ArrayList<>();
    private final int[] levelSize = new int[2];
    private final int[] proxySize = new int[] {0, 0};
    private final int[] pixelStore = new int[PIXEL_STORE_NAMES];
    private int pixelStoreSet;

    private final List<Attributes> attributeStack = new ArrayList<>();
    private int attributeDepth;
    private final DisplayLists displayLists = new DisplayLists();

    private final ImmediateBuffer immediate = new ImmediateBuffer();

    private final VertexArrays arrays = new VertexArrays();
    private final GenericArrays attributes;
    private final ShaderPipeline shaders;
    // gl names are dense small ints
    private final List<ShaderStage> shaderStages = new ArrayList<>();
    private final List<String> shaderSources = new ArrayList<>();
    private final BitSet fixedFunctionShaders = new BitSet();
    private final BitSet fixedFunctionPrograms = new BitSet();
    private final List<BitSet> attachedShaders = new ArrayList<>();
    private int[] writtenStages = new int[16];
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
    private final Revision revision = new Revision();
    private final Lighting lighting = new Lighting(revision);
    private final TextureEnvironment environment = new TextureEnvironment(revision);
    private final TextureGeneration generation = new TextureGeneration(revision);
    private boolean colorLogicOp;
    private boolean polygonOffsetLine;
    private boolean polygonOffsetPoint;
    private float normalX;
    private float normalY;
    private float normalZ = 1.0f;

    private GLState(GraphicsDevice device) {
        this.device = device;
        this.pipeline = new FixedFunctionPipeline(device);
        this.displayLists.onRelease(pipeline::release);
        this.displayLists.onEnd(pipeline::seal);
        this.attributes = new GenericArrays(device.maxAttributes());
        this.shaders = new ShaderPipeline(device, device.maxAttributes());
        this.viewportWidth = device.width();
        this.viewportHeight = device.height();
        this.scissorWidth = viewportWidth;
        this.scissorHeight = viewportHeight;
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            textureMatrices[unit] = Matrix4.identity();
            textureStacks[unit] = new MatrixStack();
        }
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
        if (mode != MODELVIEW && mode != PROJECTION && mode != TEXTURE) {
            throw new IllegalArgumentException(
                    "unsupported matrix mode 0x" + Integer.toHexString(mode)
                            + "; GL_MODELVIEW, GL_PROJECTION and GL_TEXTURE are emulated");
        }
        matrixMode = mode;
    }

    // gl_texture means the matrix of whichever unit is active, so it is looked up every time
    private float[] active() {
        return switch (matrixMode) {
            case PROJECTION -> projection;
            case TEXTURE -> textureMatrices[activeTexture];
            default -> modelView;
        };
    }

    private float[] mutable() {
        revision.bump(matrixMode == TEXTURE ? Revision.TEXTURE_MATRIX : Revision.MATRIX);
        return active();
    }

    @Override
    public int revision(int group) {
        return revision.count(group);
    }

    public void pushMatrix() {
        stack().push(active());
    }

    public void popMatrix() {
        MatrixStack stack = stack();
        if (stack.isEmpty()) {
            throw new IllegalStateException("matrix stack underflow in " + switch (matrixMode) {
                case PROJECTION -> "GL_PROJECTION";
                case TEXTURE -> "GL_TEXTURE";
                default -> "GL_MODELVIEW";
            });
        }
        stack.pop(mutable());
    }

    private MatrixStack stack() {
        return switch (matrixMode) {
            case PROJECTION -> projectionStack;
            case TEXTURE -> textureStacks[activeTexture];
            default -> modelViewStack;
        };
    }

    public void loadIdentity() {
        Matrix4.setIdentity(mutable());
    }

    public void translate(float x, float y, float z) {
        Matrix4.translate(mutable(), x, y, z);
    }

    public void rotate(float degrees, float x, float y, float z) {
        Matrix4.rotate(mutable(), degrees, x, y, z);
    }

    public void scale(float x, float y, float z) {
        Matrix4.scale(mutable(), x, y, z);
    }

    public void ortho(float left, float right, float bottom, float top, float near, float far) {
        Matrix4.ortho(mutable(), left, right, bottom, top, near, far);
    }

    public void frustum(float left, float right, float bottom, float top, float near, float far) {
        Matrix4.frustum(mutable(), left, right, bottom, top, near, far);
    }

    @Override
    public float[] modelView() {
        return modelView;
    }

    @Override
    public float[] projection() {
        return projection;
    }

    @Override
    public float[] textureMatrix(int unit) {
        return textureMatrices[unit];
    }

    // immediate mode

    public void begin(DrawMode mode) {
        if (program != GraphicsDevice.NO_PROGRAM && !readsFixedFunction()
                && !immediate.isBuilding()) {
            throw new UnsupportedOperationException(
                    "glBegin draws through the fixed-function pipeline, which the program bound by "
                            + "glUseProgram has replaced. call glUseProgram(0) first, or feed the "
                            + "program a vertex buffer of your own");
        }
        immediate.begin(mode);
        immediate.hold(red, green, blue, alpha, s, t, normalX, normalY, normalZ);
    }

    public void color(float r, float g, float b, float a) {
        immediate.touch(ImmediateLayout.COLOR);
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    public void texCoord(int unit, float newS, float newT) {
        immediate.touch(ImmediateLayout.texCoord(VertexArrays.requireUnit(unit)));
        s[unit] = newS;
        t[unit] = newT;
    }

    public void vertex(float x, float y, float z) {
        immediate.vertex(x, y, z, red, green, blue, alpha, s, t, normalX, normalY, normalZ);
    }

    public boolean isBuilding() {
        return immediate.isBuilding();
    }

    public void end() {
        immediate.end();
        if (immediate.vertexCount() == 0) {
            return;
        }
        // a list holds the vertices the block built, not the calls that built them
        if (displayLists.isRecording()) {
            int block = pipeline.capture(immediate.mode(), immediate.vertices(),
                    immediate.floatCount(), immediate.vertexCount(), immediate.layout());
            displayLists.own(block);
            if (record(Ops.DRAW_BLOCK, block, lent())) {
                return;
            }
        }
        pipeline.drawImmediate(immediate.mode(), immediate.vertices(), immediate.floatCount(),
                immediate.vertexCount(), immediate.layout(), this, lent());
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
        if (displayLists.isRecording()) {
            compileArrays(mode, first, count);
            return;
        }
        if (readsFixedFunction()) {
            pipeline.drawArrays(mode, first, count, arrays, this, program);
        } else if (program != GraphicsDevice.NO_PROGRAM) {
            shaders.drawArrays(mode, first, count, attributes);
        } else {
            pipeline.drawArrays(mode, first, count, arrays, this, GraphicsDevice.NO_PROGRAM);
        }
        restoreBindings();
    }

    // a list keeps what the arrays held when it was compiled, so it builds a block from them
    private void compileArrays(DrawMode mode, int first, int count) {
        ClientArray position = arrays.position();
        if (!position.isEnabled() || count <= 0) {
            return;
        }
        float[] held = {red, green, blue, alpha, normalX, normalY, normalZ};
        float[] heldS = s.clone();
        float[] heldT = t.clone();
        begin(mode);
        ClientArray colors = arrays.color();
        ClientArray normals = arrays.normal();
        for (int vertex = first; vertex < first + count; vertex++) {
            if (colors.isEnabled()) {
                color(colors.read(vertex, 0), colors.read(vertex, 1), colors.read(vertex, 2),
                        colors.components() > 3 ? colors.read(vertex, 3) : 1);
            }
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                ClientArray coords = arrays.texCoord(unit);
                if (coords.isEnabled()) {
                    texCoord(unit, coords.read(vertex, 0),
                            coords.components() > 1 ? coords.read(vertex, 1) : 0);
                }
            }
            if (normals.isEnabled()) {
                normal(normals.read(vertex, 0), normals.read(vertex, 1), normals.read(vertex, 2));
            }
            vertex(position.read(vertex, 0), position.read(vertex, 1),
                    position.components() > 2 ? position.read(vertex, 2) : 0);
        }
        end();
        red = held[0];
        green = held[1];
        blue = held[2];
        alpha = held[3];
        normalX = held[4];
        normalY = held[5];
        normalZ = held[6];
        System.arraycopy(heldS, 0, s, 0, s.length);
        System.arraycopy(heldT, 0, t, 0, t.length);
    }

    public boolean readsFixedFunction() {
        return program != GraphicsDevice.NO_PROGRAM && fixedFunctionPrograms.get(program);
    }

    public void drawElements(DrawMode mode, int count, IndexType indexType, int offsetInBytes) {
        requireNotBuilding("glDrawElements");
        if (elementBuffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glDrawElements needs indices in a buffer object; webgl has no client-side "
                            + "index arrays, so bind one with glBindBuffer first");
        }
        if (readsFixedFunction()) {
            pipeline.drawElements(mode, count, indexType, offsetInBytes, arrays, this, program);
        } else if (program != GraphicsDevice.NO_PROGRAM) {
            shaders.drawElements(mode, count, indexType, offsetInBytes, attributes);
        } else {
            pipeline.drawElements(mode, count, indexType, offsetInBytes, arrays, this,
                    GraphicsDevice.NO_PROGRAM);
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
        put(shaderStages, shader, stage);
    }

    public ShaderStage shaderStage(int shader) {
        ShaderStage stage = at(shaderStages, shader);
        if (stage == null) {
            throw new IllegalArgumentException(
                    "not a shader created by glCreateShader: " + shader);
        }
        return stage;
    }

    public void forgetShader(int shader) {
        put(shaderStages, shader, null);
        put(shaderSources, shader, null);
        fixedFunctionShaders.clear(shader);
    }

    public void sourced(int shader, String translated) {
        put(shaderSources, shader, translated);
    }

    private static <T> void put(List<T> held, int name, T value) {
        while (held.size() <= name) {
            held.add(null);
        }
        held.set(name, value);
    }

    private static <T> T at(List<T> held, int name) {
        return name >= 0 && name < held.size() ? held.get(name) : null;
    }

    /**
     * gl links whichever stages it was given and leaves the rest fixed-function; webgl needs
     * both, so the missing one is written here from the state the other stage names.
     */
    public void completeProgram(int target) {
        BitSet attached = at(attachedShaders, target);
        if (attached == null || attached.cardinality() != 1) {
            return;
        }
        int supplied = attached.nextSetBit(0);
        ShaderStage stage = at(shaderStages, supplied);
        boolean vertex = stage == ShaderStage.VERTEX;
        if (vertex == (stage == ShaderStage.FRAGMENT)) {
            return;
        }
        String source = at(shaderSources, supplied);
        if (source == null) {
            return;
        }
        ShaderStage missing = vertex ? ShaderStage.FRAGMENT : ShaderStage.VERTEX;
        int written = device.createShader(missing);
        device.shaderSource(written, FixedFunctionStage.companionFor(source,
                vertex ? ShaderStage.VERTEX : ShaderStage.FRAGMENT));
        device.compileShader(written);
        if (!device.isShaderCompiled(written)) {
            String why = device.shaderInfoLog(written);
            device.deleteShader(written);
            throw new IllegalStateException("the " + missing
                    + " stage gl would have run itself did not compile: " + why);
        }
        trackShader(written, missing);
        attachShader(target, written);
        device.attachShader(target, written);
        if (target >= writtenStages.length) {
            writtenStages = Arrays.copyOf(writtenStages,
                    Math.max(target + 1, writtenStages.length * 2));
        }
        writtenStages[target] = written;
    }

    private int lent() {
        return readsFixedFunction() ? program : GraphicsDevice.NO_PROGRAM;
    }

    public void markFixedFunction(int shader, boolean reads) {
        fixedFunctionShaders.set(shader, reads);
    }

    public void attachShader(int target, int shader) {
        BitSet attached = at(attachedShaders, target);
        if (attached == null) {
            attached = new BitSet();
            put(attachedShaders, target, attached);
        }
        attached.set(shader);
    }

    public void detachShader(int target, int shader) {
        BitSet attached = at(attachedShaders, target);
        if (attached != null) {
            attached.clear(shader);
        }
    }

    public void linkedProgram(int linked) {
        BitSet attached = at(attachedShaders, linked);
        boolean reads = false;
        for (int shader = attached == null ? -1 : attached.nextSetBit(0); shader >= 0;
                shader = attached.nextSetBit(shader + 1)) {
            reads |= fixedFunctionShaders.get(shader);
        }
        fixedFunctionPrograms.set(linked, reads);
    }

    public void forgetProgram(int deleted) {
        put(attachedShaders, deleted, null);
        fixedFunctionPrograms.clear(deleted);
        int written = deleted < writtenStages.length ? writtenStages[deleted] : 0;
        if (written != 0) {
            writtenStages[deleted] = 0;
            forgetShader(written);
            device.deleteShader(written);
        }
        pipeline.forgetProgram(deleted);
    }

    private void requireNotBuilding(String call) {
        if (immediate.isBuilding()) {
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
        if (textureEnabled[activeTexture] == enabled) {
            return;
        }
        revision.bump(Revision.TEXTURE);
        textureEnabled[activeTexture] = enabled;
    }

    @Override
    public boolean isTextureEnabled(int unit) {
        return textureEnabled[unit];
    }

    public void setTextureEnv(int mode) {
        environment.setMode(activeTexture, mode);
    }

    public void setTextureCombine(int at, int code) {
        environment.setCode(activeTexture, at, code);
    }

    public void setTextureEnvColor(float red, float green, float blue, float alpha) {
        environment.setColor(activeTexture, red, green, blue, alpha);
    }

    public void setTextureScale(boolean alpha, float scale) {
        environment.setScale(activeTexture, alpha, scale);
    }

    @Override
    public TextureEnvironment textureEnvironment() {
        return environment;
    }

    public void setTextureGenerated(int coordinate, boolean enabled) {
        generation.setEnabled(activeTexture, coordinate, enabled);
    }

    public void setTextureGenMode(int coordinate, int mode) {
        generation.setMode(activeTexture, coordinate, mode);
    }

    /** gl keeps an eye plane in eye space, so it goes through the modelview's inverse now */
    public void setTextureGenPlane(int coordinate, boolean eye, float a, float b, float c,
                                   float d) {
        float[] inverse = eye ? Matrix4.invert(modelView()) : null;
        if (inverse == null) {
            generation.setPlane(activeTexture, coordinate, eye, a, b, c, d);
            return;
        }
        float[] plane = new float[4];
        for (int column = 0; column < 4; column++) {
            int from = column * 4;
            plane[column] = a * inverse[from] + b * inverse[from + 1] + c * inverse[from + 2]
                    + d * inverse[from + 3];
        }
        generation.setPlane(activeTexture, coordinate, true, plane[0], plane[1], plane[2],
                plane[3]);
    }

    @Override
    public TextureGeneration textureGeneration() {
        return generation;
    }

    /**
     * capabilities webgl has no stage for: the logic op draws as copy, and polygons only ever
     * fill, so a line or point offset has nothing to move. glIsEnabled still answers for them.
     */
    public void setUnstaged(int capability, boolean enabled) {
        switch (capability) {
            case 0x0BF2 -> colorLogicOp = enabled;
            case 0x2A02 -> polygonOffsetLine = enabled;
            case 0x2A01 -> polygonOffsetPoint = enabled;
            default -> throw new IllegalArgumentException(
                    "0x" + Integer.toHexString(capability) + " has a stage to set");
        }
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
        if (alphaTestEnabled == enabled) {
            return;
        }
        revision.bump(Revision.ALPHA);
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

    public void normal(float x, float y, float z) {
        immediate.touch(ImmediateLayout.NORMAL);
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
        if (pointSize == size) {
            return;
        }
        revision.bump(Revision.POINT);
        pointSize = size;
    }

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

    public void alphaFunc(CompareFunc func, float reference) {
        if (func != CompareFunc.GREATER) {
            throw new UnsupportedOperationException(
                    "glAlphaFunc emulates GL_GREATER only, got " + func);
        }
        setAlphaFunc(func, reference);
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

    public void blendColor(float[] into) {
        System.arraycopy(blendColor, 0, into, 0, 4);
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
        texture(boundTexture(), true).automaticMipmaps = enabled;
    }

    public boolean automaticMipmaps() {
        Texture known = texture(boundTexture(), false);
        return known != null && known.automaticMipmaps;
    }

    public void deleteTexture(int texture) {
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (boundTexture[unit] == texture) {
                boundTexture[unit] = GraphicsDevice.NO_TEXTURE;
            }
        }
        if (texture >= 0 && texture < textures.size()) {
            textures.set(texture, null);
        }
        device.deleteTexture(texture);
    }

    public void recordLevelSize(int level, int width, int height) {
        Texture known = texture(boundTexture(), true);
        if (known.levels.length < (level + 1) * 2) {
            known.levels = Arrays.copyOf(known.levels, (level + 1) * 2);
        }
        known.levels[level * 2] = width;
        known.levels[level * 2 + 1] = height;
    }

    public void recordProxySize(int width, int height) {
        proxySize[0] = width;
        proxySize[1] = height;
    }

    public int[] proxySize() {
        return proxySize;
    }

    public int[] levelSize(int level) {
        Texture known = texture(boundTexture(), false);
        if (known == null || known.levels.length < (level + 1) * 2) {
            return NO_LEVEL;
        }
        levelSize[0] = known.levels[level * 2];
        levelSize[1] = known.levels[level * 2 + 1];
        return levelSize;
    }

    public void setPixelStore(int name, int value) {
        int at = name - PIXEL_STORE_FIRST;
        pixelStore[at] = value;
        pixelStoreSet |= 1 << at;
    }

    public int pixelStore(int name, int fallback) {
        int at = name - PIXEL_STORE_FIRST;
        if (at < 0 || at >= PIXEL_STORE_NAMES || (pixelStoreSet & (1 << at)) == 0) {
            return fallback;
        }
        return pixelStore[at];
    }

    private Texture texture(int name, boolean create) {
        if (name < 0) {
            return null;
        }
        if (name >= textures.size()) {
            if (!create) {
                return null;
            }
            while (textures.size() <= name) {
                textures.add(null);
            }
        }
        Texture known = textures.get(name);
        if (known == null && create) {
            known = new Texture();
            textures.set(name, known);
        }
        return known;
    }

    private static final class Texture {

        private boolean automaticMipmaps;
        private int[] levels = new int[0];
    }

    void restoreAlphaFunc(CompareFunc func, float reference) {
        setAlphaFunc(func, reference);
    }

    private void setAlphaFunc(CompareFunc func, float reference) {
        if (alphaTestFunc == func && alphaTestThreshold == reference) {
            return;
        }
        revision.bump(Revision.ALPHA);
        alphaTestFunc = func;
        alphaTestThreshold = reference;
    }

    // the attribute stack

    public void pushAttrib(int mask) {
        if (attributeDepth == attributeStack.size()) {
            attributeStack.add(new Attributes());
        }
        attributeStack.get(attributeDepth++).hold(this, mask,
                depthTest, blend, cullFaceEnabled, scissorTest, textureEnabled[activeTexture],
                alphaTestEnabled,
                lighting.isEnabled(), lighting.isFogEnabled(), lighting.isColorMaterial(),
                normalX, normalY, normalZ,
                clearRed, clearGreen, clearBlue, clearAlpha, clearDepthValue,
                maskRed, maskGreen, maskBlue, maskAlpha, depthWrite,
                depthFunc, blendSource, blendDestination, alphaTestFunc, alphaTestThreshold,
                culledFaces, frontFace,
                viewportX, viewportY, viewportWidth, viewportHeight,
                scissorX, scissorY, scissorWidth, scissorHeight,
                red, green, blue, alpha, s[0], t[0], matrixMode, boundTexture[activeTexture]);
    }

    public void popAttrib() {
        if (attributeDepth == 0) {
            throw new IllegalStateException("glPopAttrib called with an empty attribute stack");
        }
        attributeStack.get(--attributeDepth).restore(this);
    }

    // errors gl itself raises, held until read as a gl error is: the first one sticks

    private GraphicsError error = GraphicsError.NONE;

    public void fail(GraphicsError raised) {
        if (error == GraphicsError.NONE) {
            error = raised;
        }
    }

    /** this layer's error first, then the device's */
    public GraphicsError takeError() {
        GraphicsError raised = error;
        error = GraphicsError.NONE;
        return raised == GraphicsError.NONE ? device.takeError() : raised;
    }

    // display lists

    public DisplayLists lists() {
        return displayLists;
    }

    public boolean record(int op) {
        return displayLists.record(op);
    }
    public boolean record(int op, int first) {
        return displayLists.record(op, first);
    }
    public boolean record(int op, int first, int second) {
        return displayLists.record(op, first, second);
    }
    public boolean record(int op, int first, int second, int third) {
        return displayLists.record(op, first, second, third);
    }
    public boolean record(int op, int first, int second, int third, int fourth) {
        return displayLists.record(op, first, second, third, fourth);
    }
    public boolean record(int op, float a) {
        return displayLists.record(op, a);
    }
    public boolean record(int op, int first, float a) {
        return displayLists.record(op, first, a);
    }
    public boolean record(int op, int first, int second, float a) {
        return displayLists.record(op, first, second, a);
    }
    public boolean record(int op, float a, float b) {
        return displayLists.record(op, a, b);
    }
    public boolean record(int op, int first, float a, float b) {
        return displayLists.record(op, first, a, b);
    }
    public boolean record(int op, float a, float b, float c) {
        return displayLists.record(op, a, b, c);
    }
    public boolean record(int op, float a, float b, float c, float d) {
        return displayLists.record(op, a, b, c, d);
    }
    public boolean record(int op, int first, float a, float b, float c, float d) {
        return displayLists.record(op, first, a, b, c, d);
    }
    public boolean record(int op, int first, int second, float a, float b, float c, float d) {
        return displayLists.record(op, first, second, a, b, c, d);
    }
    public boolean record(int op, float a, float b, float c, float d, float e, float g) {
        return displayLists.record(op, a, b, c, d, e, g);
    }
    public boolean record(int op, float[] many) {
        return displayLists.record(op, many);
    }

    public boolean play(int op, int[] codes, int at, float[] values, int from) {
        switch (op) {
            case Ops.CALL_LIST -> displayLists.call(codes[at]);
            case Ops.MATRIX_MODE -> matrixMode(codes[at]);
            case Ops.LOAD_IDENTITY -> loadIdentity();
            case Ops.PUSH_MATRIX -> pushMatrix();
            case Ops.POP_MATRIX -> popMatrix();
            case Ops.TRANSLATE -> translate(values[from], values[from + 1], values[from + 2]);
            case Ops.ROTATE -> rotate(values[from], values[from + 1], values[from + 2],
                    values[from + 3]);
            case Ops.SCALE -> scale(values[from], values[from + 1], values[from + 2]);
            case Ops.ORTHO -> ortho(values[from], values[from + 1], values[from + 2],
                    values[from + 3], values[from + 4], values[from + 5]);
            case Ops.FRUSTUM -> frustum(values[from], values[from + 1], values[from + 2],
                    values[from + 3], values[from + 4], values[from + 5]);
            case Ops.PUSH_ATTRIB -> pushAttrib(codes[at]);
            case Ops.POP_ATTRIB -> popAttrib();
            case Ops.COLOR -> color(values[from], values[from + 1], values[from + 2],
                    values[from + 3]);
            case Ops.NORMAL -> normal(values[from], values[from + 1], values[from + 2]);
            case Ops.ALPHA_FUNC -> alphaFunc(COMPARES[codes[at]], values[from]);
            case Ops.TEXTURE_ENV -> setTextureEnv(codes[at]);
            case Ops.TEXTURE_COMBINE -> setTextureCombine(codes[at], codes[at + 1]);
            case Ops.TEXTURE_ENV_COLOR -> setTextureEnvColor(values[from], values[from + 1],
                    values[from + 2], values[from + 3]);
            case Ops.TEXTURE_SCALE -> setTextureScale(codes[at] != 0, values[from]);
            case Ops.TEXTURE_GEN_MODE -> setTextureGenMode(codes[at], codes[at + 1]);
            case Ops.TEXTURE_GEN_PLANE -> setTextureGenPlane(codes[at], codes[at + 1] != 0,
                    values[from], values[from + 1], values[from + 2], values[from + 3]);
            case Ops.MODEL_AMBIENT -> lighting.setModelAmbient(four(values, from));
            case Ops.SHININESS -> lighting.setShininess(values[from]);
            case Ops.SHADE_MODEL -> lighting.setFlatShading(codes[at] != 0);
            case Ops.COLOR_MATERIAL -> lighting.setColorMaterialTracks(codes[at]);
            case Ops.FOG_COLOR -> lighting.setFogColor(four(values, from));
            case Ops.DRAW_BLOCK -> drawBlock(codes[at], codes[at + 1]);
            case Ops.ACTIVE_TEXTURE -> activeTexture(codes[at]);
            case Ops.BLEND_FUNC -> setBlendFunc(FACTORS[codes[at]], FACTORS[codes[at + 1]]);
            case Ops.BLEND_FUNC_SEPARATE -> setBlendFuncSeparate(FACTORS[codes[at]],
                    FACTORS[codes[at + 1]], FACTORS[codes[at + 2]], FACTORS[codes[at + 3]]);
            case Ops.BLEND_EQUATION -> setBlendEquation(EQUATIONS[codes[at]]);
            case Ops.BLEND_EQUATION_SEPARATE -> setBlendEquationSeparate(EQUATIONS[codes[at]],
                    EQUATIONS[codes[at + 1]]);
            case Ops.BLEND_COLOR -> setBlendColor(values[from], values[from + 1],
                    values[from + 2], values[from + 3]);
            case Ops.CLEAR_COLOR -> clearColor(values[from], values[from + 1],
                    values[from + 2], values[from + 3]);
            case Ops.CLEAR_DEPTH -> clearDepth(values[from]);
            case Ops.VIEWPORT -> viewport(codes[at], codes[at + 1], codes[at + 2],
                    codes[at + 3]);
            case Ops.SCISSOR -> setScissor(codes[at], codes[at + 1], codes[at + 2],
                    codes[at + 3]);
            case Ops.CULLED_FACES -> setCulledFaces(CULL_FACES[codes[at]]);
            case Ops.FRONT_FACE -> setFrontFace(WINDINGS[codes[at]]);
            case Ops.COLOR_MASK -> setColorMask(codes[at] != 0, codes[at + 1] != 0,
                    codes[at + 2] != 0, codes[at + 3] != 0);
            case Ops.DEPTH_WRITE -> setDepthWrite(codes[at] != 0);
            case Ops.DEPTH_FUNC -> setDepthFunc(COMPARES[codes[at]]);
            case Ops.STENCIL_FUNC -> device().setStencilFunc(COMPARES[codes[at]],
                    codes[at + 1], codes[at + 2]);
            case Ops.STENCIL_OP -> device().setStencilOp(STENCIL_OPS[codes[at]],
                    STENCIL_OPS[codes[at + 1]], STENCIL_OPS[codes[at + 2]]);
            case Ops.STENCIL_MASK -> device().setStencilMask(codes[at]);
            case Ops.CLEAR_STENCIL -> device().clearStencil(codes[at]);
            case Ops.POLYGON_OFFSET -> device().setPolygonOffset(values[from], values[from + 1]);
            case Ops.LINE_WIDTH -> device().setLineWidth(values[from]);
            case Ops.POINT_SIZE -> pointSize(values[from]);
            case Ops.LOAD_MATRIX -> loadMatrix(sixteen(values, from));
            case Ops.MULT_MATRIX -> multMatrix(sixteen(values, from));
            case Ops.TEX_COORD -> texCoord(codes[at], values[from], values[from + 1]);
            case Ops.BIND_TEXTURE -> bindTexture(codes[at]);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void drawBlock(int block, int lent) {
        pipeline.drawCaptured(block, this, lent);
        restoreBindings();
    }

    private final float[] played = new float[16];

    private float[] four(float[] values, int from) {
        System.arraycopy(values, from, played, 0, 4);
        return played;
    }

    private float[] sixteen(float[] values, int from) {
        System.arraycopy(values, from, played, 0, 16);
        return played;
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
            case 0x0C60, 0x0C61, 0x0C62, 0x0C63 ->
                    generation.isEnabled(activeTexture, capability - 0x0C60);
            case 0x0BC0 -> alphaTestEnabled;
            case 0x0B50 -> lighting.isEnabled();
            case 0x0B57 -> lighting.isColorMaterial();
            case 0x0B60 -> lighting.isFogEnabled();
            case 0x0BF2 -> colorLogicOp;
            case 0x2A02 -> polygonOffsetLine;
            case 0x2A01 -> polygonOffsetPoint;
            case 0x4000, 0x4001, 0x4002, 0x4003, 0x4004, 0x4005, 0x4006, 0x4007 ->
                    lighting.isLightEnabled(capability - 0x4000);
            default -> throw new UnsupportedOperationException(
                    "glIsEnabled does not emulate capability 0x" + Integer.toHexString(capability));
        };
    }

    public void viewportBox(int[] into) {
        into[0] = viewportX;
        into[1] = viewportY;
        into[2] = viewportWidth;
        into[3] = viewportHeight;
    }

    public void scissorBox(int[] into) {
        into[0] = scissorX;
        into[1] = scissorY;
        into[2] = scissorWidth;
        into[3] = scissorHeight;
    }

    public void clearColorValue(float[] into) {
        into[0] = clearRed;
        into[1] = clearGreen;
        into[2] = clearBlue;
        into[3] = clearAlpha;
    }

    public void currentColor(float[] into) {
        into[0] = red;
        into[1] = green;
        into[2] = blue;
        into[3] = alpha;
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
        System.arraycopy(matrix, 0, mutable(), 0, 16);
    }

    public void multMatrix(float[] matrix) {
        Matrix4.multiply(mutable(), matrix);
    }
}
