package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
  reproduces gl 1.x fixed-function output w/ one shader
 */
public final class FixedFunctionPipeline {

    public static final int NO_CAPTURE = -1;

    public static final int VERTEX_FLOATS = 10 + 2 * VertexArrays.UNITS;

    // what a build compiles in beyond the common path; each slows every vertex or fragment it
    // reaches, so a draw takes the build with only what its state uses
    private static final int COMBINE = 1;
    private static final int GENERATE = 2;
    private static final int LIGHTING = 4;
    private static final int FOG = 8;
    private static final int MULTITEXTURE = 16;
    private static final int VARIANTS = 32;
    private static final String[] DEFINES = {"COMBINE", "GENERATE", "LIGHTING", "FOG",
        "MULTITEXTURE"};

    private final GraphicsDevice device;
    private final int buffer;
    private final int[] immediateArrays = new int[ImmediateLayout.MASKS];
    private int bufferFloats;
    // where the next immediate block goes; blocks follow one another until the buffer is full
    private int streamed;
    private final QuadIndices quadIndices;
    private final float[] normalMatrix = new float[9];

    private static final int NOTHING_WRITTEN = -1;

    // a megabyte of immediate vertices before the stream buffer starts over
    private static final int STREAM_FLOATS = 1 << 18;

    /**
     * the blocks one list compile captured in one layout, uploaded together as one buffer when
     * the list ends
     */
    private static final class Group {
        final int layout;
        float[] pending = new float[1024];
        int pendingFloats;
        int buffer = GraphicsDevice.NO_BUFFER;
        int array = GraphicsDevice.NO_VERTEX_ARRAY;
        int live;

        Group(int layout) {
            this.layout = layout;
        }
    }

    private record Captured(Group group, DrawMode mode, int first, int vertexCount) {
    }

    private final Group[] filling = new Group[ImmediateLayout.MASKS];

    private final List<Captured> captured = new ArrayList<>();
    private final List<Integer> freeCaptures = new ArrayList<>();

    private int normalMatrixFrom = NOTHING_WRITTEN;

    private final StagingArrays staging;

    private final float[] textureMatrices = new float[16 * VertexArrays.UNITS];
    private final Uniforms[] variants = new Uniforms[VARIANTS];
    private Uniforms at;

    private Borrowed[] borrowed = new Borrowed[16];

    public FixedFunctionPipeline(GraphicsDevice device) {
        this.device = device;
        this.at = variant(0);
        this.buffer = device.createBuffer();
        this.quadIndices = new QuadIndices(device);
        this.staging = new StagingArrays(device, VertexArrays.SLOTS);
    }

    public int capture(DrawMode mode, float[] vertices, int floatCount, int vertexCount,
                       int layout) {
        if (mode.usableVertices(vertexCount) == 0) {
            return NO_CAPTURE;
        }
        Group group = filling[layout];
        if (group == null) {
            group = new Group(layout);
            filling[layout] = group;
        }
        int vertexFloats = ImmediateLayout.floats(layout);
        int quad = vertexFloats * DrawMode.QUAD_VERTICES;
        // a whole number of quads in, so quad indices can start at the block
        int start = (group.pendingFloats + quad - 1) / quad * quad;
        if (start + floatCount > group.pending.length) {
            group.pending = Arrays.copyOf(group.pending,
                    Math.max(start + floatCount, group.pending.length * 2));
        }
        System.arraycopy(vertices, 0, group.pending, start, floatCount);
        group.pendingFloats = start + floatCount;
        group.live++;
        Captured block = new Captured(group, mode, start / vertexFloats, vertexCount);
        if (freeCaptures.isEmpty()) {
            captured.add(block);
            return captured.size() - 1;
        }
        int at = freeCaptures.remove(freeCaptures.size() - 1);
        captured.set(at, block);
        return at;
    }

    /** uploads what the list being compiled captured, one buffer per layout */
    public void seal() {
        for (int layout = 0; layout < filling.length; layout++) {
            Group group = filling[layout];
            if (group != null) {
                filling[layout] = null;
                upload(group);
            }
        }
    }

    private void upload(Group group) {
        group.buffer = device.createBuffer();
        device.bindBuffer(BufferTarget.ARRAY, group.buffer);
        device.bufferData(BufferTarget.ARRAY, group.pending, group.pendingFloats,
                BufferUsage.STATIC);
        group.array = describeImmediateLayout(group.layout, group.buffer);
        group.pending = null;
    }

    public void drawCaptured(int block, RenderState render, int lent) {
        if (block == NO_CAPTURE) {
            return;
        }
        Captured held = captured.get(block);
        if (held.group().array == GraphicsDevice.NO_VERTEX_ARRAY) {
            // drawn while its list is still compiling
            seal();
        }
        begin(render, lent);
        device.bindVertexArray(held.group().array);
        constants(held.group().layout, render);
        issue(held.mode(), held.first(), held.mode().usableVertices(held.vertexCount()));
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
    }

    public void release(int block) {
        if (block == NO_CAPTURE || captured.get(block) == null) {
            return;
        }
        Group group = captured.get(block).group();
        captured.set(block, null);
        freeCaptures.add(block);
        if (--group.live == 0 && group.array != GraphicsDevice.NO_VERTEX_ARRAY) {
            device.deleteVertexArray(group.array);
            device.deleteBuffer(group.buffer);
        }
    }

    public void drawImmediate(DrawMode mode, float[] vertices, int floatCount, int vertexCount,
                              int layout, RenderState render, int lent) {
        int drawable = mode.usableVertices(vertexCount);
        if (drawable == 0) {
            return;
        }
        begin(render, lent);

        device.bindVertexArray(immediateArray(layout));
        device.bindBuffer(BufferTarget.ARRAY, buffer);
        int first = stream(vertices, floatCount, ImmediateLayout.floats(layout));
        constants(layout, render);
        issue(mode, first, drawable);
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
    }

    /**
     * writes a block after the last one rather than over it, which a draw may still be reading;
     * a full buffer is orphaned and filled again from the start
     *
     * @return the vertex the block starts at, a whole number of quads in
     */
    private int stream(float[] vertices, int floatCount, int vertexFloats) {
        int quad = vertexFloats * DrawMode.QUAD_VERTICES;
        int start = (streamed + quad - 1) / quad * quad;
        if (start + floatCount > bufferFloats) {
            bufferFloats = Math.max(Math.max(STREAM_FLOATS, bufferFloats), floatCount * 2);
            device.bufferData(BufferTarget.ARRAY, bufferFloats * Float.BYTES, BufferUsage.STREAM);
            start = 0;
        }
        device.bufferSubData(BufferTarget.ARRAY, start * Float.BYTES, vertices, floatCount);
        streamed = start + floatCount;
        return start / vertexFloats;
    }

    private void constants(int layout, RenderState render) {
        if (!ImmediateLayout.has(layout, ImmediateLayout.COLOR)) {
            device.constantAttribute(VertexArrays.COLOR,
                    render.red(), render.green(), render.blue(), render.alpha());
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (!ImmediateLayout.has(layout, ImmediateLayout.texCoord(unit))) {
                device.constantAttribute(VertexArrays.TEX_COORD + unit,
                        render.s(unit), render.t(unit), 0.0f, 1.0f);
            }
        }
        if (!ImmediateLayout.has(layout, ImmediateLayout.NORMAL)) {
            device.constantAttribute(VertexArrays.NORMAL,
                    render.normalX(), render.normalY(), render.normalZ(), 0.0f);
        }
    }

    private int immediateArray(int layout) {
        if (immediateArrays[layout] == GraphicsDevice.NO_VERTEX_ARRAY) {
            immediateArrays[layout] = describeImmediateLayout(layout);
        }
        return immediateArrays[layout];
    }

    private int describeImmediateLayout(int layout) {
        return describeImmediateLayout(layout, buffer);
    }

    private int describeImmediateLayout(int layout, int from) {
        int stride = ImmediateLayout.floats(layout) * Float.BYTES;
        int array = device.createVertexArray();
        device.bindVertexArray(array);
        device.bindBuffer(BufferTarget.ARRAY, from);
        for (int slot = 0; slot < VertexArrays.SLOTS; slot++) {
            device.disableAttribute(slot);
        }
        device.enableAttribute(VertexArrays.POSITION);
        device.attributePointer(VertexArrays.POSITION, 3, AttributeType.FLOAT, false, stride, 0);
        if (ImmediateLayout.has(layout, ImmediateLayout.COLOR)) {
            device.enableAttribute(VertexArrays.COLOR);
            device.attributePointer(VertexArrays.COLOR, 4, AttributeType.FLOAT, false, stride,
                    ImmediateLayout.colorOffset(layout) * Float.BYTES);
        }
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (ImmediateLayout.has(layout, ImmediateLayout.texCoord(unit))) {
                device.enableAttribute(VertexArrays.TEX_COORD + unit);
                device.attributePointer(VertexArrays.TEX_COORD + unit, 2, AttributeType.FLOAT,
                        false, stride,
                        ImmediateLayout.texCoordOffset(layout, unit) * Float.BYTES);
            }
        }
        if (ImmediateLayout.has(layout, ImmediateLayout.NORMAL)) {
            device.enableAttribute(VertexArrays.NORMAL);
            device.attributePointer(VertexArrays.NORMAL, 3, AttributeType.FLOAT, false, stride,
                    ImmediateLayout.normalOffset(layout) * Float.BYTES);
        }
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
        return array;
    }

    public void drawArrays(DrawMode mode, int first, int count, VertexArrays arrays,
                           RenderState render, int lent) {
        int drawable = mode.usableVertices(count);
        if (drawable == 0 || !arrays.canDraw()) {
            return;
        }
        begin(render, lent);
        // quads fold first into the attribute offsets, so their indices start at zero
        bindArrays(arrays, render, first, drawable, mode.isIndexed() ? first : 0);
        issue(mode, mode.isIndexed() ? 0 : first, drawable);
    }

    public void drawElements(DrawMode mode, int count, IndexType indexType, int offsetInBytes,
                             VertexArrays arrays, RenderState render, int lent) {
        if (count == 0 || !arrays.canDraw()) {
            return;
        }
        if (mode.isIndexed()) {
            throw new UnsupportedOperationException(
                    "glDrawElements cannot emulate GL_QUADS: expanding quads into triangle pairs "
                            + "means rewriting the index buffer, which belongs to the caller. draw "
                            + "the quads with glDrawArrays, or supply triangle indices");
        }
        begin(render, lent);
        bindArrays(arrays, render, 0, StagingArrays.WHOLE, 0);
        device.drawElements(mode.primitive(), count, indexType, offsetInBytes);
    }

    /** @param first for quads, a multiple of four: the indices skip that many quads */
    private void issue(DrawMode mode, int first, int drawable) {
        if (mode.isIndexed()) {
            int skipped = first / DrawMode.QUAD_VERTICES;
            int quads = drawable / DrawMode.QUAD_VERTICES;
            quadIndices.bind(skipped + quads);
            device.drawElements(mode.primitive(), quads * DrawMode.QUAD_INDICES,
                    IndexType.UNSIGNED_INT, skipped * DrawMode.QUAD_INDICES * Integer.BYTES);
        } else {
            device.drawArrays(mode.primitive(), first, drawable);
        }
    }

    private void begin(RenderState render, int lent) {
        if (lent != GraphicsDevice.NO_PROGRAM) {
            // the application owns a lent program and can link it again at any time
            for (Uniforms variant : variants) {
                if (variant != null) {
                    variant.forget();
                }
            }
            beginLent(render, lent);
            return;
        }
        at = variant(features(render));
        device.useProgram(at.program);
        // gl keeps uniforms per program, so they still stand from the last draw through this one
        boolean matrix = moved(render, Revision.MATRIX);
        boolean texture = moved(render, Revision.TEXTURE);
        boolean alpha = moved(render, Revision.ALPHA);
        boolean point = moved(render, Revision.POINT);
        boolean lights = moved(render, Revision.LIGHTING);
        boolean fog = moved(render, Revision.FOG);
        boolean textureMatrix = moved(render, Revision.TEXTURE_MATRIX);
        boolean environment = moved(render, Revision.ENVIRONMENT);
        boolean generation = moved(render, Revision.GENERATION);
        keep(render);
        if (matrix) {
            device.uniformMatrix4(at.modelViewUniform, render.modelView());
            device.uniformMatrix4(at.projectionUniform, render.projection());
        }
        if (textureMatrix) {
            device.uniformMatrices(at.textureMatrixUniform, 4, false, textureMatrices(render),
                    VertexArrays.UNITS);
        }
        if (point) {
            device.uniform1f(at.pointSizeUniform, render.pointSize());
        }
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                device.uniform1i(at.textureEnabledUniforms[unit],
                        render.isTextureEnabled(unit) ? 1 : 0);
                device.uniform1i(at.textureEnvUniforms[unit],
                        render.textureEnvironment().mode(unit));
            }
        }
        if (environment) {
            TextureEnvironment held = render.textureEnvironment();
            device.uniformInts(at.combineUniform, 4, held.codes(),
                    TextureEnvironment.CODES / 4 * VertexArrays.UNITS);
            device.uniformFloats(at.textureEnvColorUniform, 4, held.colors(), VertexArrays.UNITS);
            device.uniformFloats(at.combineScaleUniform, 2, held.scales(), VertexArrays.UNITS);
        }
        if (generation) {
            TextureGeneration held = render.textureGeneration();
            device.uniformInts(at.texGenUniform, 4, held.codes(), VertexArrays.UNITS);
            device.uniformFloats(at.objectPlaneUniform, 4, held.objectPlanes(),
                    VertexArrays.UNITS * TextureGeneration.COORDINATES);
            device.uniformFloats(at.eyePlaneUniform, 4, held.eyePlanes(),
                    VertexArrays.UNITS * TextureGeneration.COORDINATES);
        }
        if (alpha) {
            device.uniform1i(at.alphaTestEnabledUniform, render.isAlphaTestEnabled() ? 1 : 0);
            device.uniform1f(at.alphaTestThresholdUniform, render.alphaTestThreshold());
        }
        // the normal matrix comes from the modelview
        if (lights || matrix) {
            light(render);
        }
        if (fog) {
            fog(render.lighting());
        }
    }

    private boolean moved(RenderState render, int group) {
        return at.written[group] != render.revision(group);
    }

    private void keep(RenderState render) {
        for (int group = 0; group < Revision.GROUPS; group++) {
            at.written[group] = render.revision(group);
        }
    }

    private static boolean combines(RenderState render) {
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            if (render.isTextureEnabled(unit)
                    && render.textureEnvironment().mode(unit) == TextureEnvironment.COMBINE) {
                return true;
            }
        }
        return false;
    }

    private static int features(RenderState render) {
        int features = (combines(render) ? COMBINE : 0)
                | (render.textureGeneration().generates() ? GENERATE : 0)
                | (render.lighting().isEnabled() ? LIGHTING : 0)
                | (render.lighting().isFogEnabled() ? FOG : 0);
        for (int unit = 1; unit < VertexArrays.UNITS; unit++) {
            if (render.isTextureEnabled(unit)) {
                features |= MULTITEXTURE;
            }
        }
        return features;
    }

    private Uniforms variant(int features) {
        if (variants[features] == null) {
            StringBuilder defines = new StringBuilder("#version 300 es\n");
            for (int bit = 0; bit < DEFINES.length; bit++) {
                if ((features & 1 << bit) != 0) {
                    defines.append("#define ").append(DEFINES[bit]).append('\n');
                }
            }
            String header = defines.toString();
            variants[features] = new Uniforms(device, device.createProgram(
                    FixedFunctionShaders.VERTEX.replace("#version 300 es\n", header),
                    FixedFunctionShaders.FRAGMENT.replace("#version 300 es\n", header)));
        }
        return variants[features];
    }

    private float[] textureMatrices(RenderState render) {
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            System.arraycopy(render.textureMatrix(unit), 0, textureMatrices, unit * 16, 16);
        }
        return textureMatrices;
    }

    private float[] normalMatrix(RenderState render) {
        if (normalMatrixFrom != render.revision(Revision.MATRIX)) {
            normalMatrixFrom = render.revision(Revision.MATRIX);
            Matrix4.normalMatrix(render.modelView(), normalMatrix);
        }
        return normalMatrix;
    }

    /**
     * a shader the application wrote against gl's fixed-function state reads a subset of the
     * same uniforms; whatever it does not declare comes back as no location and is skipped.
     */
    private void beginLent(RenderState render, int lent) {
        device.useProgram(lent);
        Borrowed where = borrowedBy(lent);
        Lighting lighting = render.lighting();
        boolean matrix = where.moved(render, Revision.MATRIX);
        boolean texture = where.moved(render, Revision.TEXTURE);
        boolean alpha = where.moved(render, Revision.ALPHA);
        boolean fog = where.moved(render, Revision.FOG);
        boolean textureMatrix = where.moved(render, Revision.TEXTURE_MATRIX);
        where.keep(render);
        if (matrix) {
            device.uniformMatrix4(where.modelView, render.modelView());
            device.uniformMatrix4(where.projection, render.projection());
            device.uniformMatrices(where.normalMatrix, 3, false, normalMatrix(render), 1);
        }
        if (textureMatrix) {
            device.uniformMatrices(where.textureMatrix, 4, false, textureMatrices(render),
                    VertexArrays.UNITS);
        }
        if (fog) {
            uniform4(where.fogStructColor, lighting.fogColor());
            device.uniform1f(where.fogStructDensity, lighting.fogDensity());
            device.uniform1f(where.fogStructStart, lighting.fogStart());
            device.uniform1f(where.fogStructEnd, lighting.fogEnd());
            float span = lighting.fogEnd() - lighting.fogStart();
            device.uniform1f(where.fogStructScale, span == 0.0f ? 0.0f : 1.0f / span);
            device.uniform1i(where.fogEnabled, lighting.isFogEnabled() ? 1 : 0);
            device.uniform1i(where.fogMode, lighting.fogMode());
            uniform4(where.fogColor, lighting.fogColor());
            device.uniform1f(where.fogDensity, lighting.fogDensity());
            device.uniform1f(where.fogStart, lighting.fogStart());
            device.uniform1f(where.fogEnd, lighting.fogEnd());
        }
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                device.uniform1i(where.textureEnabled[unit],
                        render.isTextureEnabled(unit) ? 1 : 0);
                device.uniform1i(where.textureEnv[unit], render.textureEnvironment().mode(unit));
            }
        }
        if (alpha) {
            device.uniform1i(where.alphaTestEnabled, render.isAlphaTestEnabled() ? 1 : 0);
            device.uniform1f(where.alphaTestThreshold, render.alphaTestThreshold());
        }
    }

    private Borrowed borrowedBy(int lent) {
        if (lent >= borrowed.length) {
            borrowed = Arrays.copyOf(borrowed, Math.max(lent + 1, borrowed.length * 2));
        }
        Borrowed where = borrowed[lent];
        if (where == null) {
            where = locateIn(lent);
            borrowed[lent] = where;
        }
        return where;
    }

    private Borrowed locateIn(int lent) {
        Borrowed where = new Borrowed();
        where.modelView = device.uniformLocation(lent, "u_modelView");
        where.projection = device.uniformLocation(lent, "u_projection");
        where.normalMatrix = device.uniformLocation(lent, "u_normalMatrix");
        where.textureMatrix = device.uniformLocation(lent, "u_textureMatrix[0]");
        where.fogStructColor = device.uniformLocation(lent, "u_fog.color");
        where.fogStructDensity = device.uniformLocation(lent, "u_fog.density");
        where.fogStructStart = device.uniformLocation(lent, "u_fog.start");
        where.fogStructEnd = device.uniformLocation(lent, "u_fog.end");
        where.fogStructScale = device.uniformLocation(lent, "u_fog.scale");
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            where.textureEnabled[unit] =
                    device.uniformLocation(lent, "u_textureEnabled[" + unit + "]");
            where.textureEnv[unit] = device.uniformLocation(lent, "u_textureEnv[" + unit + "]");
            device.uniform1i(device.uniformLocation(lent, "u_texture" + unit), unit);
        }
        where.alphaTestEnabled = device.uniformLocation(lent, "u_alphaTestEnabled");
        where.alphaTestThreshold = device.uniformLocation(lent, "u_alphaTestThreshold");
        where.fogEnabled = device.uniformLocation(lent, "u_fogEnabled");
        where.fogMode = device.uniformLocation(lent, "u_fogMode");
        where.fogColor = device.uniformLocation(lent, "u_fogColor");
        where.fogDensity = device.uniformLocation(lent, "u_fogDensity");
        where.fogStart = device.uniformLocation(lent, "u_fogStart");
        where.fogEnd = device.uniformLocation(lent, "u_fogEnd");
        return where;
    }

    private static final class Borrowed {
        private final int[] written = new int[Revision.GROUPS];
        private int modelView;
        private int projection;
        private int normalMatrix;
        private int textureMatrix;
        private int fogStructColor;
        private int fogStructDensity;
        private int fogStructStart;
        private int fogStructEnd;
        private int fogStructScale;
        private final int[] textureEnabled = new int[VertexArrays.UNITS];
        private final int[] textureEnv = new int[VertexArrays.UNITS];
        private int alphaTestEnabled;
        private int alphaTestThreshold;
        private int fogEnabled;
        private int fogMode;
        private int fogColor;
        private int fogDensity;
        private int fogStart;
        private int fogEnd;

        Borrowed() {
            Arrays.fill(written, NOTHING_WRITTEN);
        }

        boolean moved(RenderState render, int group) {
            return written[group] != render.revision(group);
        }

        void keep(RenderState render) {
            for (int group = 0; group < Revision.GROUPS; group++) {
                written[group] = render.revision(group);
            }
        }
    }

    public void forgetProgram(int deleted) {
        if (deleted >= 0 && deleted < borrowed.length) {
            borrowed[deleted] = null;
        }
    }

    private void light(RenderState render) {
        Lighting lighting = render.lighting();
        device.uniform1i(at.flatShadingUniform, lighting.isFlatShading() ? 1 : 0);
        if (!lighting.isEnabled()) {
            return;
        }
        device.uniformMatrices(at.normalMatrixUniform, 3, false, normalMatrix(render), 1);
        device.uniformInts(at.lightEnabledUniform, 1, lighting.lightEnabledFlags(),
                Lighting.LIGHTS);
        device.uniformFloats(at.lightPositionUniform, 4, lighting.positions(), Lighting.LIGHTS);
        device.uniformFloats(at.lightAmbientUniform, 4, lighting.ambients(), Lighting.LIGHTS);
        device.uniformFloats(at.lightDiffuseUniform, 4, lighting.diffuses(), Lighting.LIGHTS);
        device.uniformFloats(at.lightSpecularUniform, 4, lighting.speculars(), Lighting.LIGHTS);
        device.uniformFloats(at.lightAttenuationUniform, 3, lighting.attenuations(),
                Lighting.LIGHTS);
        uniform4(at.lightModelAmbientUniform, lighting.modelAmbient());
        uniform4(at.materialAmbientUniform, lighting.materialAmbient());
        uniform4(at.materialDiffuseUniform, lighting.materialDiffuse());
        uniform4(at.materialSpecularUniform, lighting.materialSpecular());
        uniform4(at.materialEmissionUniform, lighting.materialEmission());
        device.uniform1f(at.shininessUniform, lighting.shininess());
        device.uniform1i(at.colorMaterialUniform, lighting.colorMaterialTracks());
    }

    private void fog(Lighting lighting) {
        if (!lighting.isFogEnabled()) {
            return;
        }
        device.uniform1i(at.fogModeUniform, lighting.fogMode());
        uniform4(at.fogColorUniform, lighting.fogColor());
        device.uniform1f(at.fogDensityUniform, lighting.fogDensity());
        device.uniform1f(at.fogStartUniform, lighting.fogStart());
        device.uniform1f(at.fogEndUniform, lighting.fogEnd());
    }

    private void uniform4(int location, float[] rgba) {
        device.uniform4f(location, rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private void bindArrays(VertexArrays arrays, RenderState render, int first, int count,
                            int base) {
        staging.begin();
        need(arrays.position(), first, count);
        need(arrays.color(), first, count);
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            need(arrays.texCoord(unit), first, count);
        }
        need(arrays.normal(), first, count);
        bindSlot(VertexArrays.POSITION, arrays.position(), base,
                0.0f, 0.0f, 0.0f, 1.0f);
        bindSlot(VertexArrays.COLOR, arrays.color(), base,
                render.red(), render.green(), render.blue(), render.alpha());
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            bindSlot(VertexArrays.TEX_COORD + unit, arrays.texCoord(unit), base,
                    render.s(unit), render.t(unit), 0.0f, 1.0f);
        }
        bindSlot(VertexArrays.NORMAL, arrays.normal(), base,
                render.normalX(), render.normalY(), render.normalZ(), 0.0f);
    }

    private void need(ClientArray array, int first, int count) {
        if (array.isEnabled() && array.isBound()) {
            staging.need(array, first, count);
        }
    }

    private void bindSlot(int slot, ClientArray array, int base,
                          float x, float y, float z, float w) {
        if (!array.isEnabled() || !array.isBound()) {
            device.disableAttribute(slot);
            device.constantAttribute(slot, x, y, z, w);
            return;
        }
        device.enableAttribute(slot);
        staging.bind(slot, array, base);
    }

    public void dispose() {
        staging.dispose();
        quadIndices.dispose();
        for (int block = 0; block < captured.size(); block++) {
            release(block);
        }
        for (int layout = 0; layout < immediateArrays.length; layout++) {
            if (immediateArrays[layout] != GraphicsDevice.NO_VERTEX_ARRAY) {
                device.deleteVertexArray(immediateArrays[layout]);
                immediateArrays[layout] = GraphicsDevice.NO_VERTEX_ARRAY;
            }
        }
        device.deleteBuffer(buffer);
        for (Uniforms variant : variants) {
            if (variant != null) {
                device.deleteProgram(variant.program);
            }
        }
    }

    /** where one build of the shader keeps its uniforms, and what it was last sent */
    private static final class Uniforms {
        private final int program;
        private final int[] written = new int[Revision.GROUPS];
        private final int modelViewUniform;
        private final int projectionUniform;
        private final int pointSizeUniform;
        private final int[] textureEnabledUniforms = new int[VertexArrays.UNITS];
        private final int[] textureEnvUniforms = new int[VertexArrays.UNITS];
        private final int textureMatrixUniform;
        private final int combineUniform;
        private final int textureEnvColorUniform;
        private final int combineScaleUniform;
        private final int texGenUniform;
        private final int objectPlaneUniform;
        private final int eyePlaneUniform;
        private final int alphaTestEnabledUniform;
        private final int alphaTestThresholdUniform;
        private final int flatShadingUniform;

        private final int normalMatrixUniform;
        private final int lightEnabledUniform;
        private final int lightPositionUniform;
        private final int lightAmbientUniform;
        private final int lightDiffuseUniform;
        private final int lightSpecularUniform;
        private final int lightAttenuationUniform;
        private final int lightModelAmbientUniform;
        private final int materialAmbientUniform;
        private final int materialDiffuseUniform;
        private final int materialSpecularUniform;
        private final int materialEmissionUniform;
        private final int shininessUniform;
        private final int colorMaterialUniform;
        private final int fogModeUniform;
        private final int fogColorUniform;
        private final int fogDensityUniform;
        private final int fogStartUniform;
        private final int fogEndUniform;

        Uniforms(GraphicsDevice device, int program) {
            this.program = program;
            forget();
            modelViewUniform = device.uniformLocation(program, "u_modelView");
            projectionUniform = device.uniformLocation(program, "u_projection");
            pointSizeUniform = device.uniformLocation(program, "u_pointSize");
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                textureEnabledUniforms[unit] =
                        device.uniformLocation(program, "u_textureEnabled[" + unit + "]");
                textureEnvUniforms[unit] = device.uniformLocation(program, "u_textureEnv[" + unit
                        + "]");
            }
            textureMatrixUniform = device.uniformLocation(program, "u_textureMatrix[0]");
            combineUniform = device.uniformLocation(program, "u_combine[0]");
            textureEnvColorUniform = device.uniformLocation(program, "u_textureEnvColor[0]");
            combineScaleUniform = device.uniformLocation(program, "u_combineScale[0]");
            texGenUniform = device.uniformLocation(program, "u_texGen[0]");
            objectPlaneUniform = device.uniformLocation(program, "u_objectPlane[0]");
            eyePlaneUniform = device.uniformLocation(program, "u_eyePlane[0]");
            alphaTestEnabledUniform = device.uniformLocation(program, "u_alphaTestEnabled");
            alphaTestThresholdUniform = device.uniformLocation(program, "u_alphaTestThreshold");
            flatShadingUniform = device.uniformLocation(program, "u_flatShading");

            normalMatrixUniform = device.uniformLocation(program, "u_normalMatrix");
            lightEnabledUniform = device.uniformLocation(program, "u_lightEnabled[0]");
            lightPositionUniform = device.uniformLocation(program, "u_lightPosition[0]");
            lightAmbientUniform = device.uniformLocation(program, "u_lightAmbient[0]");
            lightDiffuseUniform = device.uniformLocation(program, "u_lightDiffuse[0]");
            lightSpecularUniform = device.uniformLocation(program, "u_lightSpecular[0]");
            lightAttenuationUniform = device.uniformLocation(program, "u_lightAttenuation[0]");
            lightModelAmbientUniform = device.uniformLocation(program, "u_lightModelAmbient");
            materialAmbientUniform = device.uniformLocation(program, "u_materialAmbient");
            materialDiffuseUniform = device.uniformLocation(program, "u_materialDiffuse");
            materialSpecularUniform = device.uniformLocation(program, "u_materialSpecular");
            materialEmissionUniform = device.uniformLocation(program, "u_materialEmission");
            shininessUniform = device.uniformLocation(program, "u_shininess");
            colorMaterialUniform = device.uniformLocation(program, "u_colorMaterial");
            fogModeUniform = device.uniformLocation(program, "u_fogMode");
            fogColorUniform = device.uniformLocation(program, "u_fogColor");
            fogDensityUniform = device.uniformLocation(program, "u_fogDensity");
            fogStartUniform = device.uniformLocation(program, "u_fogStart");
            fogEndUniform = device.uniformLocation(program, "u_fogEnd");
            device.useProgram(program);
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                device.uniform1i(device.uniformLocation(program, "u_texture" + unit), unit);
            }
        }

        void forget() {
            Arrays.fill(written, NOTHING_WRITTEN);
        }
    }
}
