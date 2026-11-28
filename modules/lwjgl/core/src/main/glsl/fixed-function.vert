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

#ifdef GENERATE
// each unit's s, t, r and q mode: 0 given, 1 object linear, 2 eye linear, 3 sphere map
uniform ivec4 u_texGen[4];
uniform vec4 u_objectPlane[16];
uniform vec4 u_eyePlane[16];
#endif

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

// gl reads s and t, with r 0 and q 1, through each unit's texture matrix
vec4 coordinate(int unit, vec2 given, vec4 eye, vec3 normal) {
    vec4 coordinate = vec4(given, 0.0, 1.0);
    #ifdef GENERATE
    ivec4 modes = u_texGen[unit];
    vec3 reflected = reflect(normalize(eye.xyz), normal);
    float sphere = 2.0 * length(reflected + vec3(0.0, 0.0, 1.0));
    for (int at = 0; at < 4; at++) {
        if (modes[at] == 1) {
            coordinate[at] = dot(u_objectPlane[unit * 4 + at], vec4(in_position, 1.0));
        } else if (modes[at] == 2) {
            coordinate[at] = dot(u_eyePlane[unit * 4 + at], eye);
        } else if (modes[at] == 3) {
            coordinate[at] = reflected[at] / max(sphere, 0.0001) + 0.5;
        }
    }
    #endif
    return u_textureMatrix[unit] * coordinate;
}

void main() {
    vec4 eye = u_modelView * vec4(in_position, 1.0);
    vec3 normal = normalize(u_normalMatrix * in_normal);
    v_texCoord[0] = coordinate(0, in_texCoord0, eye, normal);
    v_texCoord[1] = coordinate(1, in_texCoord1, eye, normal);
    v_texCoord[2] = coordinate(2, in_texCoord2, eye, normal);
    v_texCoord[3] = coordinate(3, in_texCoord3, eye, normal);

    v_eyeDistance = length(eye.xyz);
    #ifdef LIGHTING
    v_color = lit(eye.xyz, normal);
    #else
    v_color = in_color;
    #endif
    v_flatColor = v_color;

    gl_PointSize = u_pointSize;
    gl_Position = u_projection * eye;
}
