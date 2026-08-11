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

    public static final int VERTEX_FLOATS = 10 + 2 * VertexArrays.UNITS;

    private static final int VERTEX_STRIDE = VERTEX_FLOATS * Float.BYTES;
    private static final int COLOR_OFFSET = 3 * Float.BYTES;
    private static final int TEX_COORD_OFFSET = 7 * Float.BYTES;
    private static final int NORMAL_OFFSET = TEX_COORD_OFFSET + 2 * VertexArrays.UNITS * Float.BYTES;

    private static final String VERTEX_SHADER = """
            #version 300 es
            precision highp float;

            layout(location = 0) in vec3 in_position;
            layout(location = 1) in vec4 in_color;
            layout(location = 2) in vec2 in_texCoord0;
            layout(location = 3) in vec2 in_texCoord1;
            layout(location = 4) in vec2 in_texCoord2;
            layout(location = 5) in vec2 in_texCoord3;
            layout(location = 6) in vec3 in_normal;

            uniform mat4 u_modelView;
            uniform mat4 u_projection;
            uniform mat3 u_normalMatrix;
            uniform float u_pointSize;

            uniform bool u_lightingEnabled;
            uniform int u_lightEnabled[8];
            uniform vec4 u_lightPosition[8];
            uniform vec4 u_lightAmbient[8];
            uniform vec4 u_lightDiffuse[8];
            uniform vec4 u_lightSpecular[8];
            uniform vec3 u_lightAttenuation[8];
            uniform vec4 u_lightModelAmbient;

            uniform vec4 u_materialAmbient;
            uniform vec4 u_materialDiffuse;
            uniform vec4 u_materialSpecular;
            uniform vec4 u_materialEmission;
            uniform float u_shininess;
            uniform bool u_colorMaterial;

            out vec4 v_color;
            out vec2 v_texCoord[4];
            out float v_eyeDistance;

            // gl lights per vertex, so the whole model is evaluated here rather than per fragment
            vec4 lit(vec3 eye, vec3 normal) {
                vec4 ambientMaterial = u_colorMaterial ? in_color : u_materialAmbient;
                vec4 diffuseMaterial = u_colorMaterial ? in_color : u_materialDiffuse;

                vec3 color = u_materialEmission.rgb
                        + u_lightModelAmbient.rgb * ambientMaterial.rgb;
                vec3 toEye = normalize(-eye);

                for (int light = 0; light < 8; light++) {
                    if (u_lightEnabled[light] == 0) {
                        continue;
                    }
                    vec4 position = u_lightPosition[light];
                    vec3 toLight;
                    float attenuation = 1.0;
                    if (position.w == 0.0) {
                        toLight = normalize(position.xyz);
                    } else {
                        vec3 offset = position.xyz - eye;
                        float distance = length(offset);
                        toLight = offset / max(distance, 0.0001);
                        vec3 factors = u_lightAttenuation[light];
                        attenuation = 1.0 / max(factors.x + factors.y * distance
                                + factors.z * distance * distance, 0.0001);
                    }

                    color += attenuation * u_lightAmbient[light].rgb * ambientMaterial.rgb;

                    float lambert = max(dot(normal, toLight), 0.0);
                    color += attenuation * lambert
                            * u_lightDiffuse[light].rgb * diffuseMaterial.rgb;

                    if (lambert > 0.0 && u_shininess > 0.0) {
                        vec3 halfway = normalize(toLight + toEye);
                        float gloss = pow(max(dot(normal, halfway), 0.0), u_shininess);
                        color += attenuation * gloss
                                * u_lightSpecular[light].rgb * u_materialSpecular.rgb;
                    }
                }
                // gl takes the alpha from the diffuse material alone
                return vec4(color, diffuseMaterial.a);
            }

            void main() {
                v_texCoord[0] = in_texCoord0;
                v_texCoord[1] = in_texCoord1;
                v_texCoord[2] = in_texCoord2;
                v_texCoord[3] = in_texCoord3;

                vec4 eye = u_modelView * vec4(in_position, 1.0);
                v_eyeDistance = length(eye.xyz);
                v_color = u_lightingEnabled
                        ? lit(eye.xyz, normalize(u_normalMatrix * in_normal))
                        : in_color;

                gl_PointSize = u_pointSize;
                gl_Position = u_projection * eye;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            in vec4 v_color;
            in vec2 v_texCoord[4];
            in float v_eyeDistance;

            uniform sampler2D u_texture0;
            uniform sampler2D u_texture1;
            uniform sampler2D u_texture2;
            uniform sampler2D u_texture3;
            uniform bool u_textureEnabled[4];
            uniform int u_textureEnv[4];
            uniform bool u_alphaTestEnabled;
            uniform float u_alphaTestThreshold;
            uniform bool u_fogEnabled;
            uniform int u_fogMode;
            uniform vec4 u_fogColor;
            uniform float u_fogDensity;
            uniform float u_fogStart;
            uniform float u_fogEnd;

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

            // GL_LINEAR, GL_EXP and GL_EXP2, the three curves gl fades a fragment out along
            float fogFactor(float distance) {
                if (u_fogMode == 0) {
                    return (u_fogEnd - distance) / max(u_fogEnd - u_fogStart, 0.0001);
                }
                if (u_fogMode == 2) {
                    float d = u_fogDensity * distance;
                    return exp(-d * d);
                }
                return exp(-u_fogDensity * distance);
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
                if (u_fogEnabled) {
                    // fog reaches the color but never the alpha
                    float blend = clamp(fogFactor(v_eyeDistance), 0.0, 1.0);
                    color = vec4(mix(u_fogColor.rgb, color.rgb, blend), color.a);
                }
                out_color = color;
            }
            """;

    private final GraphicsDevice device;
    private final int program;
    private final int buffer;
    private final int immediateArray;
    private final QuadIndices quadIndices;
    private final float[] normalMatrix = new float[9];

    private final StagingArrays staging;

    private final int modelViewUniform;
    private final int projectionUniform;
    private final int pointSizeUniform;
    private final int[] textureEnabledUniforms = new int[VertexArrays.UNITS];
    private final int[] textureEnvUniforms = new int[VertexArrays.UNITS];
    private final int alphaTestEnabledUniform;
    private final int alphaTestThresholdUniform;

    private final int normalMatrixUniform;
    private final int lightingEnabledUniform;
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
    private final int fogEnabledUniform;
    private final int fogModeUniform;
    private final int fogColorUniform;
    private final int fogDensityUniform;
    private final int fogStartUniform;
    private final int fogEndUniform;

    public FixedFunctionPipeline(GraphicsDevice device) {
        this.device = device;
        this.program = device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        this.buffer = device.createBuffer();
        this.immediateArray = describeImmediateLayout();
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

        normalMatrixUniform = device.uniformLocation(program, "u_normalMatrix");
        lightingEnabledUniform = device.uniformLocation(program, "u_lightingEnabled");
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
        fogEnabledUniform = device.uniformLocation(program, "u_fogEnabled");
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

    /** uploads and draws {@code vertexCount} vertices of interleaved data. */
    public void drawImmediate(DrawMode mode, float[] vertices, int floatCount, int vertexCount,
                              RenderState render) {
        int drawable = mode.usableVertices(vertexCount);
        if (drawable == 0) {
            return;
        }
        begin(render);

        device.bindVertexArray(immediateArray);
        device.bindBuffer(BufferTarget.ARRAY, buffer);
        device.bufferData(BufferTarget.ARRAY, vertices, floatCount, BufferUsage.STREAM);
        issue(mode, 0, drawable);
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
    }

    private int describeImmediateLayout() {
        int array = device.createVertexArray();
        device.bindVertexArray(array);
        device.bindBuffer(BufferTarget.ARRAY, buffer);
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
        device.attributePointer(VertexArrays.NORMAL, 3, AttributeType.FLOAT, false,
                VERTEX_STRIDE, NORMAL_OFFSET);
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
        return array;
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
        light(render);
        fog(render.lighting());
    }

    private void light(RenderState render) {
        Lighting lighting = render.lighting();
        device.uniform1i(lightingEnabledUniform, lighting.isEnabled() ? 1 : 0);
        if (!lighting.isEnabled()) {
            return;
        }
        device.uniformMatrices(normalMatrixUniform, 3, false,
                Matrix4.normalMatrix(render.modelView(), normalMatrix), 1);
        device.uniformInts(lightEnabledUniform, 1, lighting.lightEnabledFlags(), Lighting.LIGHTS);
        device.uniformFloats(lightPositionUniform, 4, lighting.positions(), Lighting.LIGHTS);
        device.uniformFloats(lightAmbientUniform, 4, lighting.ambients(), Lighting.LIGHTS);
        device.uniformFloats(lightDiffuseUniform, 4, lighting.diffuses(), Lighting.LIGHTS);
        device.uniformFloats(lightSpecularUniform, 4, lighting.speculars(), Lighting.LIGHTS);
        device.uniformFloats(lightAttenuationUniform, 3, lighting.attenuations(), Lighting.LIGHTS);
        uniform4(lightModelAmbientUniform, lighting.modelAmbient());
        uniform4(materialAmbientUniform, lighting.materialAmbient());
        uniform4(materialDiffuseUniform, lighting.materialDiffuse());
        uniform4(materialSpecularUniform, lighting.materialSpecular());
        uniform4(materialEmissionUniform, lighting.materialEmission());
        device.uniform1f(shininessUniform, lighting.shininess());
        device.uniform1i(colorMaterialUniform, lighting.isColorMaterial() ? 1 : 0);
    }

    private void fog(Lighting lighting) {
        device.uniform1i(fogEnabledUniform, lighting.isFogEnabled() ? 1 : 0);
        if (!lighting.isFogEnabled()) {
            return;
        }
        device.uniform1i(fogModeUniform, lighting.fogMode());
        uniform4(fogColorUniform, lighting.fogColor());
        device.uniform1f(fogDensityUniform, lighting.fogDensity());
        device.uniform1f(fogStartUniform, lighting.fogStart());
        device.uniform1f(fogEndUniform, lighting.fogEnd());
    }

    private void uniform4(int location, float[] rgba) {
        device.uniform4f(location, rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private void bindArrays(VertexArrays arrays, RenderState render) {
        bindSlot(VertexArrays.POSITION, arrays.position(), 0.0f, 0.0f, 0.0f, 1.0f);
        bindSlot(VertexArrays.COLOR, arrays.color(),
                render.red(), render.green(), render.blue(), render.alpha());
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            bindSlot(VertexArrays.TEX_COORD + unit, arrays.texCoord(unit),
                    render.s(unit), render.t(unit), 0.0f, 1.0f);
        }
        bindSlot(VertexArrays.NORMAL, arrays.normal(),
                render.normalX(), render.normalY(), render.normalZ(), 0.0f);
    }

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
        device.deleteVertexArray(immediateArray);
        device.deleteBuffer(buffer);
        device.deleteProgram(program);
    }
}
