package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;

/**
 * reproduces gl 1.x fixed-function output with a single shader.
 */
public final class FixedFunctionPipeline {

    /** floats per vertex: xyz, rgba, st. */
    public static final int VERTEX_FLOATS = 9;

    private static final int VERTEX_STRIDE = VERTEX_FLOATS * Float.BYTES;
    private static final int COLOR_OFFSET = 3 * Float.BYTES;
    private static final int TEX_COORD_OFFSET = 7 * Float.BYTES;

    private static final String VERTEX_SHADER = """
            #version 300 es
            precision highp float;

            layout(location = 0) in vec3 in_position;
            layout(location = 1) in vec4 in_color;
            layout(location = 2) in vec2 in_texCoord;

            uniform mat4 u_modelView;
            uniform mat4 u_projection;

            out vec4 v_color;
            out vec2 v_texCoord;

            void main() {
                v_color = in_color;
                v_texCoord = in_texCoord;
                gl_Position = u_projection * u_modelView * vec4(in_position, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            in vec4 v_color;
            in vec2 v_texCoord;

            uniform sampler2D u_texture;
            uniform bool u_textureEnabled;
            uniform bool u_alphaTestEnabled;
            uniform float u_alphaTestThreshold;

            out vec4 out_color;

            void main() {
                vec4 color = v_color;
                if (u_textureEnabled) {
                    color *= texture(u_texture, v_texCoord);
                }
                // gl's alpha test discards, it does not blend; discard is the faithful mapping
                if (u_alphaTestEnabled && color.a <= u_alphaTestThreshold) {
                    discard;
                }
                out_color = color;
            }
            """;

    private final GraphicsDevice device;
    private final int program;
    private final int buffer;
    private final QuadIndices quadIndices;

    private final StagingArrays staging;

    private final int modelViewUniform;
    private final int projectionUniform;
    private final int textureUniform;
    private final int textureEnabledUniform;
    private final int alphaTestEnabledUniform;
    private final int alphaTestThresholdUniform;

    public FixedFunctionPipeline(GraphicsDevice device) {
        this.device = device;
        this.program = device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        this.buffer = device.createBuffer();
        this.quadIndices = new QuadIndices(device);
        this.staging = new StagingArrays(device, VertexArrays.SLOTS);

        modelViewUniform = device.uniformLocation(program, "u_modelView");
        projectionUniform = device.uniformLocation(program, "u_projection");
        textureUniform = device.uniformLocation(program, "u_texture");
        textureEnabledUniform = device.uniformLocation(program, "u_textureEnabled");
        alphaTestEnabledUniform = device.uniformLocation(program, "u_alphaTestEnabled");
        alphaTestThresholdUniform = device.uniformLocation(program, "u_alphaTestThreshold");

        device.useProgram(program);
        device.uniform1i(textureUniform, 0);
    }

    /** uploads and draws {@code vertexCount} vertices of interleaved data. */
    public void drawImmediate(DrawMode mode, float[] vertices, int floatCount, int vertexCount,
                              RenderState render) {
        int drawable = mode.usableVertices(vertexCount);
        if (drawable == 0) {
            return;
        }
        begin(render);

        device.bindBuffer(BufferTarget.ARRAY, buffer);
        device.bufferData(BufferTarget.ARRAY, vertices, floatCount, BufferUsage.STREAM);
        for (int slot = 0; slot < VertexArrays.SLOTS; slot++) {
            device.enableAttribute(slot);
        }
        device.attributePointer(VertexArrays.POSITION, 3, AttributeType.FLOAT, false,
                VERTEX_STRIDE, 0);
        device.attributePointer(VertexArrays.COLOR, 4, AttributeType.FLOAT, false,
                VERTEX_STRIDE, COLOR_OFFSET);
        device.attributePointer(VertexArrays.TEX_COORD, 2, AttributeType.FLOAT, false,
                VERTEX_STRIDE, TEX_COORD_OFFSET);

        issue(mode, 0, drawable);
    }

    /** draws {@code count} vertices from {@code first}, read through the client arrays. */
    public void drawArrays(DrawMode mode, int first, int count, VertexArrays arrays,
                           RenderState render) {
        int drawable = mode.usableVertices(count);
        if (drawable == 0 || !arrays.canDraw()) {
            return;
        }
        begin(render);
        bindArrays(arrays, render);
        issue(mode, first, drawable);
    }

    /**
     * draws through the client arrays, indexed by the element buffer the caller has bound.
     */
    public void drawElements(DrawMode mode, int count, IndexType indexType, int offsetInBytes,
                             VertexArrays arrays, RenderState render) {
        if (count == 0 || !arrays.canDraw()) {
            return;
        }
        if (mode.isIndexed()) {
            throw new UnsupportedOperationException(
                    "glDrawElements cannot emulate GL_QUADS: expanding quads into triangle pairs "
                            + "means rewriting the index buffer, which belongs to the caller. draw "
                            + "the quads with glDrawArrays, or supply triangle indices");
        }
        begin(render);
        bindArrays(arrays, render);
        device.drawElements(mode.primitive(), count, indexType, offsetInBytes);
    }

    private void issue(DrawMode mode, int first, int drawable) {
        if (mode.isIndexed()) {
            int quads = drawable / DrawMode.QUAD_VERTICES;
            quadIndices.bind(quads, first);
            device.drawElements(mode.primitive(), quads * DrawMode.QUAD_INDICES,
                    IndexType.UNSIGNED_INT, 0);
        } else {
            device.drawArrays(mode.primitive(), first, drawable);
        }
    }

    private void begin(RenderState render) {
        device.useProgram(program);
        device.uniformMatrix4(modelViewUniform, render.modelView());
        device.uniformMatrix4(projectionUniform, render.projection());
        device.uniform1i(textureEnabledUniform, render.isTextureEnabled() ? 1 : 0);
        device.uniform1i(alphaTestEnabledUniform, render.isAlphaTestEnabled() ? 1 : 0);
        device.uniform1f(alphaTestThresholdUniform, render.alphaTestThreshold());
    }

    private void bindArrays(VertexArrays arrays, RenderState render) {
        bindSlot(VertexArrays.POSITION, arrays.position(), 0.0f, 0.0f, 0.0f, 1.0f);
        bindSlot(VertexArrays.COLOR, arrays.color(),
                render.red(), render.green(), render.blue(), render.alpha());
        bindSlot(VertexArrays.TEX_COORD, arrays.texCoord(), render.s(), render.t(), 0.0f, 1.0f);
    }

    /**
     * a disabled array means gl feeds every vertex the current constant — the color set by
     * {@code glColor}, the coordinate set by {@code glTexCoord} — rather than nothing at all.
     */
    private void bindSlot(int slot, ClientArray array, float x, float y, float z, float w) {
        if (!array.isEnabled() || !array.isBound()) {
            device.disableAttribute(slot);
            device.constantAttribute(slot, x, y, z, w);
            return;
        }
        device.enableAttribute(slot);
        staging.bind(slot, array);
    }

    public void dispose() {
        staging.dispose();
        quadIndices.dispose();
        device.deleteBuffer(buffer);
        device.deleteProgram(program);
    }
}
