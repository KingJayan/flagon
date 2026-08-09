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

    /** floats per vertex: xyz, rgba, then st for each texture unit. */
    public static final int VERTEX_FLOATS = 7 + 2 * VertexArrays.UNITS;

    private static final int VERTEX_STRIDE = VERTEX_FLOATS * Float.BYTES;
    private static final int COLOR_OFFSET = 3 * Float.BYTES;
    private static final int TEX_COORD_OFFSET = 7 * Float.BYTES;

    private static final String VERTEX_SHADER = """
            #version 300 es
            precision highp float;

            layout(location = 0) in vec3 in_position;
            layout(location = 1) in vec4 in_color;
            layout(location = 2) in vec2 in_texCoord0;
            layout(location = 3) in vec2 in_texCoord1;
            layout(location = 4) in vec2 in_texCoord2;
            layout(location = 5) in vec2 in_texCoord3;

            uniform mat4 u_modelView;
            uniform mat4 u_projection;
            uniform float u_pointSize;

            out vec4 v_color;
            out vec2 v_texCoord[4];

            void main() {
                v_color = in_color;
                v_texCoord[0] = in_texCoord0;
                v_texCoord[1] = in_texCoord1;
                v_texCoord[2] = in_texCoord2;
                v_texCoord[3] = in_texCoord3;
                gl_PointSize = u_pointSize;
                gl_Position = u_projection * u_modelView * vec4(in_position, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            in vec4 v_color;
            in vec2 v_texCoord[4];

            uniform sampler2D u_texture0;
            uniform sampler2D u_texture1;
            uniform sampler2D u_texture2;
            uniform sampler2D u_texture3;
            uniform bool u_textureEnabled[4];
            uniform int u_textureEnv[4];
            uniform bool u_alphaTestEnabled;
            uniform float u_alphaTestThreshold;

            out vec4 out_color;

            // gl combines the units in order, each one taking the result of the last as its
            // incoming fragment. the modes are GL_MODULATE, GL_REPLACE and GL_ADD.
            vec4 combine(vec4 incoming, vec4 sampled, int mode) {
                if (mode == 1) {
                    return sampled;
                }
                if (mode == 2) {
                    return vec4(incoming.rgb + sampled.rgb, incoming.a * sampled.a);
                }
                return incoming * sampled;
            }

            void main() {
                vec4 color = v_color;
                if (u_textureEnabled[0]) {
                    color = combine(color, texture(u_texture0, v_texCoord[0]), u_textureEnv[0]);
                }
                if (u_textureEnabled[1]) {
                    color = combine(color, texture(u_texture1, v_texCoord[1]), u_textureEnv[1]);
                }
                if (u_textureEnabled[2]) {
                    color = combine(color, texture(u_texture2, v_texCoord[2]), u_textureEnv[2]);
                }
                if (u_textureEnabled[3]) {
                    color = combine(color, texture(u_texture3, v_texCoord[3]), u_textureEnv[3]);
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
    private final int pointSizeUniform;
    private final int[] textureEnabledUniforms = new int[VertexArrays.UNITS];
    private final int[] textureEnvUniforms = new int[VertexArrays.UNITS];
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
        pointSizeUniform = device.uniformLocation(program, "u_pointSize");
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            textureEnabledUniforms[unit] =
                    device.uniformLocation(program, "u_textureEnabled[" + unit + "]");
            textureEnvUniforms[unit] = device.uniformLocation(program, "u_textureEnv[" + unit + "]");
        }
        alphaTestEnabledUniform = device.uniformLocation(program, "u_alphaTestEnabled");
        alphaTestThresholdUniform = device.uniformLocation(program, "u_alphaTestThreshold");

        device.useProgram(program);
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            device.uniform1i(device.uniformLocation(program, "u_texture" + unit), unit);
        }
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
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            device.attributePointer(VertexArrays.TEX_COORD + unit, 2, AttributeType.FLOAT, false,
                    VERTEX_STRIDE, TEX_COORD_OFFSET + unit * 2 * Float.BYTES);
        }

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
        device.uniform1f(pointSizeUniform, render.pointSize());
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            device.uniform1i(textureEnabledUniforms[unit], render.isTextureEnabled(unit) ? 1 : 0);
            device.uniform1i(textureEnvUniforms[unit], render.textureEnv(unit));
        }
        device.uniform1i(alphaTestEnabledUniform, render.isAlphaTestEnabled() ? 1 : 0);
        device.uniform1f(alphaTestThresholdUniform, render.alphaTestThreshold());
    }

    private void bindArrays(VertexArrays arrays, RenderState render) {
        bindSlot(VertexArrays.POSITION, arrays.position(), 0.0f, 0.0f, 0.0f, 1.0f);
        bindSlot(VertexArrays.COLOR, arrays.color(),
                render.red(), render.green(), render.blue(), render.alpha());
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            bindSlot(VertexArrays.TEX_COORD + unit, arrays.texCoord(unit),
                    render.s(unit), render.t(unit), 0.0f, 1.0f);
        }
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
