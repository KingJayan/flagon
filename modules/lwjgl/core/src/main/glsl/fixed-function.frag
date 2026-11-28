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
    #ifdef MULTITEXTURE
    if (u_textureEnabled[1]) {
        samples.unit[1] = textureProj(u_texture1, v_texCoord[1].xyw);
    }
    if (u_textureEnabled[2]) {
        samples.unit[2] = textureProj(u_texture2, v_texCoord[2].xyw);
    }
    if (u_textureEnabled[3]) {
        samples.unit[3] = textureProj(u_texture3, v_texCoord[3].xyw);
    }
    #endif
    vec4 color = primary;
    if (u_textureEnabled[0]) {
        color = fold(0, samples, color, primary);
    }
    #ifdef MULTITEXTURE
    if (u_textureEnabled[1]) {
        color = fold(1, samples, color, primary);
    }
    if (u_textureEnabled[2]) {
        color = fold(2, samples, color, primary);
    }
    if (u_textureEnabled[3]) {
        color = fold(3, samples, color, primary);
    }
    #endif
    // gl's alpha test discards, it does not blend; discard is the faithful mapping
    if (u_alphaTestEnabled && color.a <= u_alphaTestThreshold) {
        discard;
    }
    #ifdef FOG
    // fog reaches the color but never the alpha
    float blend = clamp(fogFactor(v_eyeDistance), 0.0, 1.0);
    color = vec4(mix(u_fogColor.rgb, color.rgb, blend), color.a);
    #endif
    out_color = color;
}
