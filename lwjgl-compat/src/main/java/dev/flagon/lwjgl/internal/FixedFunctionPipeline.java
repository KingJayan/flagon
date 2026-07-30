package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.Primitive;

/**
 * reproduces gl 1.x fixed-function output with a single shader.
 *
 * <p>webgl2 has no fixed-function pipeline at all, so the parts of it desktop code relies on —
 * the matrix stacks, per-vertex colour, one texture unit, and the alpha test — are folded into
 * one program whose uniforms carry the state gl would have kept internally. features are toggled
 * by uniform rather than by recompiling, which keeps state changes cheap.
 */
public final class FixedFunctionPipeline {

    /** floats per vertex: xyz, rgba, st. */
    public static final int VERTEX_FLOATS = 9;

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

        modelViewUniform = device.uniformLocation(program, "u_modelView");
        projectionUniform = device.uniformLocation(program, "u_projection");
        textureUniform = device.uniformLocation(program, "u_texture");
        textureEnabledUniform = device.uniformLocation(program, "u_textureEnabled");
        alphaTestEnabledUniform = device.uniformLocation(program, "u_alphaTestEnabled");
        alphaTestThresholdUniform = device.uniformLocation(program, "u_alphaTestThreshold");

        device.useProgram(program);
        device.bindArrayBuffer(buffer);
        for (int location = 0; location < 3; location++) {
            device.enableAttribute(location);
        }
        device.attributePointer(0, 3, VERTEX_FLOATS, 0);
        device.attributePointer(1, 4, VERTEX_FLOATS, 3);
        device.attributePointer(2, 2, VERTEX_FLOATS, 7);
        device.uniform1i(textureUniform, 0);
    }

    /** uploads and draws {@code vertexCount} vertices of interleaved data. */
    public void draw(Primitive primitive, float[] vertices, int floatCount, int vertexCount,
                     float[] modelView, float[] projection,
                     boolean textureEnabled, boolean alphaTestEnabled, float alphaTestThreshold) {
        device.useProgram(program);
        device.bindArrayBuffer(buffer);
        device.bufferData(vertices, floatCount, BufferUsage.STREAM);

        device.attributePointer(0, 3, VERTEX_FLOATS, 0);
        device.attributePointer(1, 4, VERTEX_FLOATS, 3);
        device.attributePointer(2, 2, VERTEX_FLOATS, 7);

        device.uniformMatrix4(modelViewUniform, modelView);
        device.uniformMatrix4(projectionUniform, projection);
        device.uniform1i(textureEnabledUniform, textureEnabled ? 1 : 0);
        device.uniform1i(alphaTestEnabledUniform, alphaTestEnabled ? 1 : 0);
        device.uniform1f(alphaTestThresholdUniform, alphaTestThreshold);

        device.drawArrays(primitive, 0, vertexCount);
    }

    public void dispose() {
        device.deleteBuffer(buffer);
        device.deleteProgram(program);
    }
}
