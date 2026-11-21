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
            uniform mat4 u_textureMatrix[4];

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
            // the materials the vertex color stands in for: ambient 1, diffuse 2, specular 4,
            // emission 8
            uniform int u_colorMaterial;

            out vec4 v_color;
            // gl es takes a flat value from the last vertex, as gl does by default
            flat out vec4 v_flatColor;
            out vec4 v_texCoord[4];
            out float v_eyeDistance;

            // gl lights per vertex, so the whole model is evaluated here rather than per fragment
            vec4 lit(vec3 eye, vec3 normal) {
                vec4 ambientMaterial = (u_colorMaterial & 1) != 0 ? in_color : u_materialAmbient;
                vec4 diffuseMaterial = (u_colorMaterial & 2) != 0 ? in_color : u_materialDiffuse;
                vec4 specularMaterial =
                        (u_colorMaterial & 4) != 0 ? in_color : u_materialSpecular;
                vec4 emissionMaterial =
                        (u_colorMaterial & 8) != 0 ? in_color : u_materialEmission;

                vec3 color = emissionMaterial.rgb
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
                                * u_lightSpecular[light].rgb * specularMaterial.rgb;
                    }
                }
                // gl takes the alpha from the diffuse material alone
                return vec4(color, diffuseMaterial.a);
            }

            void main() {
                // gl reads s and t, with r 0 and q 1, through each unit's texture matrix
                v_texCoord[0] = u_textureMatrix[0] * vec4(in_texCoord0, 0.0, 1.0);
                v_texCoord[1] = u_textureMatrix[1] * vec4(in_texCoord1, 0.0, 1.0);
                v_texCoord[2] = u_textureMatrix[2] * vec4(in_texCoord2, 0.0, 1.0);
                v_texCoord[3] = u_textureMatrix[3] * vec4(in_texCoord3, 0.0, 1.0);

                vec4 eye = u_modelView * vec4(in_position, 1.0);
                v_eyeDistance = length(eye.xyz);
                v_color = u_lightingEnabled
                        ? lit(eye.xyz, normalize(u_normalMatrix * in_normal))
                        : in_color;
                v_flatColor = v_color;

                gl_PointSize = u_pointSize;
                gl_Position = u_projection * eye;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            in vec4 v_color;
            flat in vec4 v_flatColor;
            in vec4 v_texCoord[4];
            in float v_eyeDistance;

            uniform sampler2D u_texture0;
            uniform sampler2D u_texture1;
            uniform sampler2D u_texture2;
            uniform sampler2D u_texture3;
            uniform bool u_textureEnabled[4];
            uniform int u_textureEnv[4];
            // per unit: rgb function and sources, alpha function and sources, rgb operands,
            // alpha operands
            uniform ivec4 u_combine[16];
            uniform vec4 u_textureEnvColor[4];
            uniform vec2 u_combineScale[4];
            uniform bool u_alphaTestEnabled;
            uniform float u_alphaTestThreshold;
            uniform bool u_flatShading;
            uniform bool u_fogEnabled;
            uniform int u_fogMode;
            uniform vec4 u_fogColor;
            uniform float u_fogDensity;
            uniform float u_fogStart;
            uniform float u_fogEnd;

            out vec4 out_color;

            // the four samples, each unit's taken once; a combine source may read any of them
            struct Samples {
                vec4 unit[4];
            };

            #ifdef COMBINE
            // GL_TEXTURE, GL_CONSTANT, GL_PRIMARY_COLOR, GL_PREVIOUS, then GL_TEXTURE0 onwards
            vec4 source(int code, int unit, Samples samples, vec4 previous, vec4 primary) {
                if (code == 0) {
                    return samples.unit[unit];
                }
                if (code == 1) {
                    return u_textureEnvColor[unit];
                }
                if (code == 2) {
                    return primary;
                }
                if (code == 3) {
                    return previous;
                }
                return samples.unit[code - 4];
            }

            vec3 operandRgb(vec4 value, int operand) {
                if (operand == 1) {
                    return 1.0 - value.rgb;
                }
                if (operand == 2) {
                    return vec3(value.a);
                }
                if (operand == 3) {
                    return vec3(1.0 - value.a);
                }
                return value.rgb;
            }

            float operandAlpha(vec4 value, int operand) {
                return operand == 3 ? 1.0 - value.a : value.a;
            }

            vec4 apply(int function, vec4 a, vec4 b, vec4 c) {
                if (function == 0) {
                    return a;
                }
                if (function == 2) {
                    return a + b;
                }
                if (function == 3) {
                    return a + b - 0.5;
                }
                if (function == 4) {
                    return a * c + b * (1.0 - c);
                }
                if (function == 5) {
                    return a - b;
                }
                return a * b;
            }

            vec4 combined(int unit, Samples samples, vec4 previous, vec4 primary) {
                ivec4 rgb = u_combine[unit * 4];
                ivec4 alpha = u_combine[unit * 4 + 1];
                ivec4 rgbOperand = u_combine[unit * 4 + 2];
                ivec4 alphaOperand = u_combine[unit * 4 + 3];
                vec4 first = source(rgb.y, unit, samples, previous, primary);
                vec4 second = source(rgb.z, unit, samples, previous, primary);
                vec4 third = source(rgb.w, unit, samples, previous, primary);
                vec4 color = apply(rgb.x, vec4(operandRgb(first, rgbOperand.x), 0.0),
                        vec4(operandRgb(second, rgbOperand.y), 0.0),
                        vec4(operandRgb(third, rgbOperand.z), 0.0));
                first = source(alpha.y, unit, samples, previous, primary);
                second = source(alpha.z, unit, samples, previous, primary);
                third = source(alpha.w, unit, samples, previous, primary);
                vec4 opacity = apply(alpha.x, vec4(operandAlpha(first, alphaOperand.x)),
                        vec4(operandAlpha(second, alphaOperand.y)),
                        vec4(operandAlpha(third, alphaOperand.z)));
                vec2 scale = u_combineScale[unit];
                return clamp(vec4(color.rgb * scale.x, opacity.a * scale.y), 0.0, 1.0);
            }
            #endif

            // GL_MODULATE, GL_REPLACE, GL_ADD, GL_COMBINE, GL_DECAL and GL_BLEND, the common one
            // first
            vec4 fold(int unit, Samples samples, vec4 previous, vec4 primary) {
                vec4 texel = samples.unit[unit];
                int mode = u_textureEnv[unit];
                if (mode == 0) {
                    return previous * texel;
                }
                if (mode == 1) {
                    return texel;
                }
                if (mode == 2) {
                    return vec4(previous.rgb + texel.rgb, previous.a * texel.a);
                }
                #ifdef COMBINE
                if (mode == 3) {
                    return combined(unit, samples, previous, primary);
                }
                #endif
                if (mode == 4) {
                    return vec4(mix(previous.rgb, texel.rgb, texel.a), previous.a);
                }
                return vec4(mix(previous.rgb, u_textureEnvColor[unit].rgb, texel.rgb),
                        previous.a * texel.a);
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
                vec4 primary = u_flatShading ? v_flatColor : v_color;
                Samples samples = Samples(vec4[4](vec4(1.0), vec4(1.0), vec4(1.0), vec4(1.0)));
                if (u_textureEnabled[0]) {
                    samples.unit[0] = textureProj(u_texture0, v_texCoord[0].xyw);
                }
                if (u_textureEnabled[1]) {
                    samples.unit[1] = textureProj(u_texture1, v_texCoord[1].xyw);
                }
                if (u_textureEnabled[2]) {
                    samples.unit[2] = textureProj(u_texture2, v_texCoord[2].xyw);
                }
                if (u_textureEnabled[3]) {
                    samples.unit[3] = textureProj(u_texture3, v_texCoord[3].xyw);
                }
                vec4 color = primary;
                if (u_textureEnabled[0]) {
                    color = fold(0, samples, color, primary);
                }
                if (u_textureEnabled[1]) {
                    color = fold(1, samples, color, primary);
                }
                if (u_textureEnabled[2]) {
                    color = fold(2, samples, color, primary);
                }
                if (u_textureEnabled[3]) {
                    color = fold(3, samples, color, primary);
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

    private static final String COMBINING_FRAGMENT_SHADER =
            FRAGMENT_SHADER.replace("#version 300 es\n", "#version 300 es\n#define COMBINE\n");

    private final GraphicsDevice device;
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

    private int normalMatrixFrom = NOTHING_WRITTEN;

    private final StagingArrays staging;

    private final float[] textureMatrices = new float[16 * VertexArrays.UNITS];
    private final Uniforms plain;
    private Uniforms combining;
    private Uniforms at;

    private Borrowed[] borrowed = new Borrowed[16];

    public FixedFunctionPipeline(GraphicsDevice device) {
        this.device = device;
        this.plain = new Uniforms(device, device.createProgram(VERTEX_SHADER, FRAGMENT_SHADER));
        this.at = plain;
        this.buffer = device.createBuffer();
        this.quadIndices = new QuadIndices(device);
        this.staging = new StagingArrays(device, VertexArrays.SLOTS);
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
            plain.forget();
            if (combining != null) {
                combining.forget();
            }
            beginLent(render, lent);
            return;
        }
        at = combines(render) ? combining() : plain;
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

    // the combine path slows every fragment it is compiled into, so it is a second build that a
    // draw takes only when one of its units combines
    private Uniforms combining() {
        if (combining == null) {
            combining = new Uniforms(device,
                    device.createProgram(VERTEX_SHADER, COMBINING_FRAGMENT_SHADER));
        }
        return combining;
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
        device.uniform1i(at.lightingEnabledUniform, lighting.isEnabled() ? 1 : 0);
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
        device.uniform1i(at.fogEnabledUniform, lighting.isFogEnabled() ? 1 : 0);
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
        device.deleteProgram(plain.program);
        if (combining != null) {
            device.deleteProgram(combining.program);
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
        private final int alphaTestEnabledUniform;
        private final int alphaTestThresholdUniform;
        private final int flatShadingUniform;

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
            alphaTestEnabledUniform = device.uniformLocation(program, "u_alphaTestEnabled");
            alphaTestThresholdUniform = device.uniformLocation(program, "u_alphaTestThreshold");
            flatShadingUniform = device.uniformLocation(program, "u_flatShading");

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

        void forget() {
            Arrays.fill(written, NOTHING_WRITTEN);
        }
    }
}
