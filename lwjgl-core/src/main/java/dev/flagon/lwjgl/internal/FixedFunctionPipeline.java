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
    private final int[] immediateArrays = new int[ImmediateLayout.MASKS];
    private int bufferFloats;
    private final QuadIndices quadIndices;
    private final float[] normalMatrix = new float[9];

    private static final int NOTHING_WRITTEN = -1;

    private record Captured(int buffer, int array, DrawMode mode, int vertexCount, int layout) {
    }

    private final List<Captured> captured = new ArrayList<>();
    private final List<Integer> freeCaptures = new ArrayList<>();

    private final int[] written = new int[Revision.GROUPS];
    private int normalMatrixFrom = NOTHING_WRITTEN;

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

    private Borrowed[] borrowed = new Borrowed[16];

    public FixedFunctionPipeline(GraphicsDevice device) {
        this.device = device;
        this.program = device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        this.buffer = device.createBuffer();
        this.quadIndices = new QuadIndices(device);
        this.staging = new StagingArrays(device, VertexArrays.SLOTS);
        Arrays.fill(written, NOTHING_WRITTEN);

        modelViewUniform = device.uniformLocation(program, "u_modelView");
        projectionUniform = device.uniformLocation(program, "u_projection");
        pointSizeUniform = device.uniformLocation(program, "u_pointSize");
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            textureEnabledUniforms[unit] =
                    device.uniformLocation(program, "u_textureEnabled[" + unit + "]");
            textureEnvUniforms[unit] = device.uniformLocation(program, "u_textureEnv[" + unit
                    + "]");
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

    public int capture(DrawMode mode, float[] vertices, int floatCount, int vertexCount,
                       int layout) {
        if (mode.usableVertices(vertexCount) == 0) {
            return NO_CAPTURE;
        }
        int buffer = device.createBuffer();
        device.bindBuffer(BufferTarget.ARRAY, buffer);
        device.bufferData(BufferTarget.ARRAY, vertices, floatCount, BufferUsage.STATIC);
        int array = describeImmediateLayout(layout, buffer);
        Captured block = new Captured(buffer, array, mode, vertexCount, layout);
        if (freeCaptures.isEmpty()) {
            captured.add(block);
            return captured.size() - 1;
        }
        int at = freeCaptures.remove(freeCaptures.size() - 1);
        captured.set(at, block);
        return at;
    }

    public void drawCaptured(int block, RenderState render, int lent) {
        if (block == NO_CAPTURE) {
            return;
        }
        Captured held = captured.get(block);
        begin(render, lent);
        device.bindVertexArray(held.array());
        constants(held.layout(), render);
        issue(held.mode(), 0, held.mode().usableVertices(held.vertexCount()));
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
    }

    public void release(int block) {
        if (block == NO_CAPTURE || captured.get(block) == null) {
            return;
        }
        Captured held = captured.get(block);
        device.deleteVertexArray(held.array());
        device.deleteBuffer(held.buffer());
        captured.set(block, null);
        freeCaptures.add(block);
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
        stream(vertices, floatCount);
        constants(layout, render);
        issue(mode, 0, drawable);
        device.bindVertexArray(GraphicsDevice.NO_VERTEX_ARRAY);
    }

    private void stream(float[] vertices, int floatCount) {
        if (floatCount > bufferFloats) {
            bufferFloats = Math.max(floatCount, bufferFloats * 2);
            device.bufferData(BufferTarget.ARRAY, bufferFloats * Float.BYTES, BufferUsage.STREAM);
        }
        device.bufferSubData(BufferTarget.ARRAY, 0, vertices, floatCount);
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
        bindArrays(arrays, render, first, drawable, mode.isIndexed() ? first : 0);
        issue(mode, first, drawable);
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

    private void issue(DrawMode mode, int first, int drawable) {
        if (mode.isIndexed()) {
            int quads = drawable / DrawMode.QUAD_VERTICES;
            quadIndices.bind(quads);
            device.drawElements(mode.primitive(), quads * DrawMode.QUAD_INDICES,
                    IndexType.UNSIGNED_INT, 0);
        } else {
            device.drawArrays(mode.primitive(), first, drawable);
        }
    }

    private void begin(RenderState render, int lent) {
        if (lent != GraphicsDevice.NO_PROGRAM) {
            // the application owns a lent program and can link it again at any time
            Arrays.fill(written, NOTHING_WRITTEN);
            beginLent(render, lent);
            return;
        }
        device.useProgram(program);
        // gl keeps uniforms per program, so they still stand from the last draw through this one
        boolean matrix = moved(render, Revision.MATRIX);
        boolean texture = moved(render, Revision.TEXTURE);
        boolean alpha = moved(render, Revision.ALPHA);
        boolean point = moved(render, Revision.POINT);
        boolean lights = moved(render, Revision.LIGHTING);
        boolean fog = moved(render, Revision.FOG);
        keep(render);
        if (matrix) {
            device.uniformMatrix4(modelViewUniform, render.modelView());
            device.uniformMatrix4(projectionUniform, render.projection());
        }
        if (point) {
            device.uniform1f(pointSizeUniform, render.pointSize());
        }
        if (texture) {
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                device.uniform1i(textureEnabledUniforms[unit],
                        render.isTextureEnabled(unit) ? 1 : 0);
                device.uniform1i(textureEnvUniforms[unit], render.textureEnv(unit));
            }
        }
        if (alpha) {
            device.uniform1i(alphaTestEnabledUniform, render.isAlphaTestEnabled() ? 1 : 0);
            device.uniform1f(alphaTestThresholdUniform, render.alphaTestThreshold());
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
        return written[group] != render.revision(group);
    }

    private void keep(RenderState render) {
        for (int group = 0; group < Revision.GROUPS; group++) {
            written[group] = render.revision(group);
        }
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
        where.keep(render);
        if (matrix) {
            device.uniformMatrix4(where.modelView, render.modelView());
            device.uniformMatrix4(where.projection, render.projection());
            device.uniformMatrices(where.normalMatrix, 3, false, normalMatrix(render), 1);
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
                device.uniform1i(where.textureEnv[unit], render.textureEnv(unit));
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
        device.uniform1i(lightingEnabledUniform, lighting.isEnabled() ? 1 : 0);
        if (!lighting.isEnabled()) {
            return;
        }
        device.uniformMatrices(normalMatrixUniform, 3, false, normalMatrix(render), 1);
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

    private void bindArrays(VertexArrays arrays, RenderState render, int first, int count,
                            int base) {
        bindSlot(VertexArrays.POSITION, arrays.position(), first, count, base,
                0.0f, 0.0f, 0.0f, 1.0f);
        bindSlot(VertexArrays.COLOR, arrays.color(), first, count, base,
                render.red(), render.green(), render.blue(), render.alpha());
        for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
            bindSlot(VertexArrays.TEX_COORD + unit, arrays.texCoord(unit), first, count, base,
                    render.s(unit), render.t(unit), 0.0f, 1.0f);
        }
        bindSlot(VertexArrays.NORMAL, arrays.normal(), first, count, base,
                render.normalX(), render.normalY(), render.normalZ(), 0.0f);
    }

    private void bindSlot(int slot, ClientArray array, int first, int count, int base,
                          float x, float y, float z, float w) {
        if (!array.isEnabled() || !array.isBound()) {
            device.disableAttribute(slot);
            device.constantAttribute(slot, x, y, z, w);
            return;
        }
        device.enableAttribute(slot);
        staging.bind(slot, array, first, count, base);
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
        device.deleteProgram(program);
    }
}
