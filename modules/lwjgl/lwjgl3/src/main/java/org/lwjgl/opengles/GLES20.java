package org.lwjgl.opengles;

import dev.flagon.lwjgl.internal.Buffers;
import dev.flagon.platform.graphics.EsContext;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

/** gl es 2.0 on the webgl2 context */
public class GLES20 {

    public static final int GL_DEPTH_BUFFER_BIT = 0x100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x400;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_FALSE = 0x0;
    public static final int GL_TRUE = 0x1;
    public static final int GL_POINTS = 0x0;
    public static final int GL_LINES = 0x1;
    public static final int GL_LINE_LOOP = 0x2;
    public static final int GL_LINE_STRIP = 0x3;
    public static final int GL_TRIANGLES = 0x4;
    public static final int GL_TRIANGLE_STRIP = 0x5;
    public static final int GL_TRIANGLE_FAN = 0x6;
    public static final int GL_ZERO = 0x0;
    public static final int GL_ONE = 0x1;
    public static final int GL_SRC_COLOR = 0x300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x301;
    public static final int GL_SRC_ALPHA = 0x302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x303;
    public static final int GL_DST_ALPHA = 0x304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x305;
    public static final int GL_DST_COLOR = 0x306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x307;
    public static final int GL_SRC_ALPHA_SATURATE = 0x308;
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_BLEND_EQUATION = 0x8009;
    public static final int GL_BLEND_EQUATION_RGB = 0x8009;
    public static final int GL_BLEND_EQUATION_ALPHA = 0x883D;
    public static final int GL_FUNC_SUBTRACT = 0x800A;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
    public static final int GL_BLEND_DST_RGB = 0x80C8;
    public static final int GL_BLEND_SRC_RGB = 0x80C9;
    public static final int GL_BLEND_DST_ALPHA = 0x80CA;
    public static final int GL_BLEND_SRC_ALPHA = 0x80CB;
    public static final int GL_CONSTANT_COLOR = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_BLEND_COLOR = 0x8005;
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    public static final int GL_ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_BUFFER_SIZE = 0x8764;
    public static final int GL_BUFFER_USAGE = 0x8765;
    public static final int GL_CURRENT_VERTEX_ATTRIB = 0x8626;
    public static final int GL_FRONT = 0x404;
    public static final int GL_BACK = 0x405;
    public static final int GL_FRONT_AND_BACK = 0x408;
    public static final int GL_TEXTURE_2D = 0xDE1;
    public static final int GL_CULL_FACE = 0xB44;
    public static final int GL_BLEND = 0xBE2;
    public static final int GL_DITHER = 0xBD0;
    public static final int GL_STENCIL_TEST = 0xB90;
    public static final int GL_DEPTH_TEST = 0xB71;
    public static final int GL_SCISSOR_TEST = 0xC11;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
    public static final int GL_SAMPLE_COVERAGE = 0x80A0;
    public static final int GL_NO_ERROR = 0x0;
    public static final int GL_INVALID_ENUM = 0x500;
    public static final int GL_INVALID_VALUE = 0x501;
    public static final int GL_INVALID_OPERATION = 0x502;
    public static final int GL_OUT_OF_MEMORY = 0x505;
    public static final int GL_CW = 0x900;
    public static final int GL_CCW = 0x901;
    public static final int GL_LINE_WIDTH = 0xB21;
    public static final int GL_ALIASED_POINT_SIZE_RANGE = 0x846D;
    public static final int GL_ALIASED_LINE_WIDTH_RANGE = 0x846E;
    public static final int GL_CULL_FACE_MODE = 0xB45;
    public static final int GL_FRONT_FACE = 0xB46;
    public static final int GL_DEPTH_RANGE = 0xB70;
    public static final int GL_DEPTH_WRITEMASK = 0xB72;
    public static final int GL_DEPTH_CLEAR_VALUE = 0xB73;
    public static final int GL_DEPTH_FUNC = 0xB74;
    public static final int GL_STENCIL_CLEAR_VALUE = 0xB91;
    public static final int GL_STENCIL_FUNC = 0xB92;
    public static final int GL_STENCIL_FAIL = 0xB94;
    public static final int GL_STENCIL_PASS_DEPTH_FAIL = 0xB95;
    public static final int GL_STENCIL_PASS_DEPTH_PASS = 0xB96;
    public static final int GL_STENCIL_REF = 0xB97;
    public static final int GL_STENCIL_VALUE_MASK = 0xB93;
    public static final int GL_STENCIL_WRITEMASK = 0xB98;
    public static final int GL_STENCIL_BACK_FUNC = 0x8800;
    public static final int GL_STENCIL_BACK_FAIL = 0x8801;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_FAIL = 0x8802;
    public static final int GL_STENCIL_BACK_PASS_DEPTH_PASS = 0x8803;
    public static final int GL_STENCIL_BACK_REF = 0x8CA3;
    public static final int GL_STENCIL_BACK_VALUE_MASK = 0x8CA4;
    public static final int GL_STENCIL_BACK_WRITEMASK = 0x8CA5;
    public static final int GL_VIEWPORT = 0xBA2;
    public static final int GL_SCISSOR_BOX = 0xC10;
    public static final int GL_COLOR_CLEAR_VALUE = 0xC22;
    public static final int GL_COLOR_WRITEMASK = 0xC23;
    public static final int GL_UNPACK_ALIGNMENT = 0xCF5;
    public static final int GL_PACK_ALIGNMENT = 0xD05;
    public static final int GL_MAX_TEXTURE_SIZE = 0xD33;
    public static final int GL_MAX_VIEWPORT_DIMS = 0xD3A;
    public static final int GL_SUBPIXEL_BITS = 0xD50;
    public static final int GL_RED_BITS = 0xD52;
    public static final int GL_GREEN_BITS = 0xD53;
    public static final int GL_BLUE_BITS = 0xD54;
    public static final int GL_ALPHA_BITS = 0xD55;
    public static final int GL_DEPTH_BITS = 0xD56;
    public static final int GL_STENCIL_BITS = 0xD57;
    public static final int GL_POLYGON_OFFSET_UNITS = 0x2A00;
    public static final int GL_POLYGON_OFFSET_FACTOR = 0x8038;
    public static final int GL_TEXTURE_BINDING_2D = 0x8069;
    public static final int GL_SAMPLE_BUFFERS = 0x80A8;
    public static final int GL_SAMPLES = 0x80A9;
    public static final int GL_SAMPLE_COVERAGE_VALUE = 0x80AA;
    public static final int GL_SAMPLE_COVERAGE_INVERT = 0x80AB;
    public static final int GL_NUM_COMPRESSED_TEXTURE_FORMATS = 0x86A2;
    public static final int GL_COMPRESSED_TEXTURE_FORMATS = 0x86A3;
    public static final int GL_DONT_CARE = 0x1100;
    public static final int GL_FASTEST = 0x1101;
    public static final int GL_NICEST = 0x1102;
    public static final int GL_GENERATE_MIPMAP_HINT = 0x8192;
    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_FIXED = 0x140C;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB;
    public static final int GL_MAX_VARYING_VECTORS = 0x8DFC;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    public static final int GL_MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD;
    public static final int GL_SHADER_TYPE = 0x8B4F;
    public static final int GL_DELETE_STATUS = 0x8B80;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_VALIDATE_STATUS = 0x8B83;
    public static final int GL_ATTACHED_SHADERS = 0x8B85;
    public static final int GL_ACTIVE_UNIFORMS = 0x8B86;
    public static final int GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87;
    public static final int GL_ACTIVE_ATTRIBUTES = 0x8B89;
    public static final int GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    public static final int GL_CURRENT_PROGRAM = 0x8B8D;
    public static final int GL_NEVER = 0x200;
    public static final int GL_LESS = 0x201;
    public static final int GL_EQUAL = 0x202;
    public static final int GL_LEQUAL = 0x203;
    public static final int GL_GREATER = 0x204;
    public static final int GL_NOTEQUAL = 0x205;
    public static final int GL_GEQUAL = 0x206;
    public static final int GL_ALWAYS = 0x207;
    public static final int GL_KEEP = 0x1E00;
    public static final int GL_REPLACE = 0x1E01;
    public static final int GL_INCR = 0x1E02;
    public static final int GL_DECR = 0x1E03;
    public static final int GL_INVERT = 0x150A;
    public static final int GL_INCR_WRAP = 0x8507;
    public static final int GL_DECR_WRAP = 0x8508;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TEXTURE = 0x1702;
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    public static final int GL_TEXTURE_BINDING_CUBE_MAP = 0x8514;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A;
    public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C;
    public static final int GL_TEXTURE0 = 0x84C0;
    public static final int GL_TEXTURE1 = 0x84C1;
    public static final int GL_TEXTURE2 = 0x84C2;
    public static final int GL_TEXTURE3 = 0x84C3;
    public static final int GL_TEXTURE4 = 0x84C4;
    public static final int GL_TEXTURE5 = 0x84C5;
    public static final int GL_TEXTURE6 = 0x84C6;
    public static final int GL_TEXTURE7 = 0x84C7;
    public static final int GL_TEXTURE8 = 0x84C8;
    public static final int GL_TEXTURE9 = 0x84C9;
    public static final int GL_TEXTURE10 = 0x84CA;
    public static final int GL_TEXTURE11 = 0x84CB;
    public static final int GL_TEXTURE12 = 0x84CC;
    public static final int GL_TEXTURE13 = 0x84CD;
    public static final int GL_TEXTURE14 = 0x84CE;
    public static final int GL_TEXTURE15 = 0x84CF;
    public static final int GL_TEXTURE16 = 0x84D0;
    public static final int GL_TEXTURE17 = 0x84D1;
    public static final int GL_TEXTURE18 = 0x84D2;
    public static final int GL_TEXTURE19 = 0x84D3;
    public static final int GL_TEXTURE20 = 0x84D4;
    public static final int GL_TEXTURE21 = 0x84D5;
    public static final int GL_TEXTURE22 = 0x84D6;
    public static final int GL_TEXTURE23 = 0x84D7;
    public static final int GL_TEXTURE24 = 0x84D8;
    public static final int GL_TEXTURE25 = 0x84D9;
    public static final int GL_TEXTURE26 = 0x84DA;
    public static final int GL_TEXTURE27 = 0x84DB;
    public static final int GL_TEXTURE28 = 0x84DC;
    public static final int GL_TEXTURE29 = 0x84DD;
    public static final int GL_TEXTURE30 = 0x84DE;
    public static final int GL_TEXTURE31 = 0x84DF;
    public static final int GL_ACTIVE_TEXTURE = 0x84E0;
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_FLOAT_VEC2 = 0x8B50;
    public static final int GL_FLOAT_VEC3 = 0x8B51;
    public static final int GL_FLOAT_VEC4 = 0x8B52;
    public static final int GL_INT_VEC2 = 0x8B53;
    public static final int GL_INT_VEC3 = 0x8B54;
    public static final int GL_INT_VEC4 = 0x8B55;
    public static final int GL_BOOL = 0x8B56;
    public static final int GL_BOOL_VEC2 = 0x8B57;
    public static final int GL_BOOL_VEC3 = 0x8B58;
    public static final int GL_BOOL_VEC4 = 0x8B59;
    public static final int GL_FLOAT_MAT2 = 0x8B5A;
    public static final int GL_FLOAT_MAT3 = 0x8B5B;
    public static final int GL_FLOAT_MAT4 = 0x8B5C;
    public static final int GL_SAMPLER_2D = 0x8B5E;
    public static final int GL_SAMPLER_CUBE = 0x8B60;
    public static final int GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622;
    public static final int GL_VERTEX_ATTRIB_ARRAY_SIZE = 0x8623;
    public static final int GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624;
    public static final int GL_VERTEX_ATTRIB_ARRAY_TYPE = 0x8625;
    public static final int GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 0x886A;
    public static final int GL_VERTEX_ATTRIB_ARRAY_POINTER = 0x8645;
    public static final int GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F;
    public static final int GL_IMPLEMENTATION_COLOR_READ_TYPE = 0x8B9A;
    public static final int GL_IMPLEMENTATION_COLOR_READ_FORMAT = 0x8B9B;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_INFO_LOG_LENGTH = 0x8B84;
    public static final int GL_SHADER_SOURCE_LENGTH = 0x8B88;
    public static final int GL_SHADER_COMPILER = 0x8DFA;
    public static final int GL_SHADER_BINARY_FORMATS = 0x8DF8;
    public static final int GL_NUM_SHADER_BINARY_FORMATS = 0x8DF9;
    public static final int GL_LOW_FLOAT = 0x8DF0;
    public static final int GL_MEDIUM_FLOAT = 0x8DF1;
    public static final int GL_HIGH_FLOAT = 0x8DF2;
    public static final int GL_LOW_INT = 0x8DF3;
    public static final int GL_MEDIUM_INT = 0x8DF4;
    public static final int GL_HIGH_INT = 0x8DF5;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_RENDERBUFFER = 0x8D41;
    public static final int GL_RGBA4 = 0x8056;
    public static final int GL_RGB5_A1 = 0x8057;
    public static final int GL_RGB565 = 0x8D62;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_STENCIL_INDEX8 = 0x8D48;
    public static final int GL_RENDERBUFFER_WIDTH = 0x8D42;
    public static final int GL_RENDERBUFFER_HEIGHT = 0x8D43;
    public static final int GL_RENDERBUFFER_INTERNAL_FORMAT = 0x8D44;
    public static final int GL_RENDERBUFFER_RED_SIZE = 0x8D50;
    public static final int GL_RENDERBUFFER_GREEN_SIZE = 0x8D51;
    public static final int GL_RENDERBUFFER_BLUE_SIZE = 0x8D52;
    public static final int GL_RENDERBUFFER_ALPHA_SIZE = 0x8D53;
    public static final int GL_RENDERBUFFER_DEPTH_SIZE = 0x8D54;
    public static final int GL_RENDERBUFFER_STENCIL_SIZE = 0x8D55;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 0x8CD0;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 0x8CD1;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL = 0x8CD2;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8CD3;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_NONE = 0x0;
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    public static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    public static final int GL_RENDERBUFFER_BINDING = 0x8CA7;
    public static final int GL_MAX_RENDERBUFFER_SIZE = 0x84E8;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION = 0x506;

    protected GLES20() {
    }

    static EsContext es() {
        return Es.context();
    }

    static String describe(EsContext.Variable variable, IntBuffer size, IntBuffer type) {
        if (variable == null) {
            return "";
        }
        size.put(size.position(), variable.size());
        type.put(type.position(), variable.type());
        return variable.name();
    }

    static void describe(EsContext.Variable variable, IntBuffer length, IntBuffer size,
                         IntBuffer type, ByteBuffer name) {
        Es.write(describe(variable, size, type), length, name);
    }

    public static void glActiveTexture(int texture) {
        es().activeTexture(texture);
    }

    public static void glAttachShader(int program, int shader) {
        es().attachShader(program, shader);
    }

    public static void glBindAttribLocation(int program, int index, ByteBuffer name) {
        es().bindAttribLocation(program, index, Es.text(name));
    }

    public static void glBindAttribLocation(int program, int index, CharSequence name) {
        es().bindAttribLocation(program, index, name.toString());
    }

    public static void glBindBuffer(int target, int buffer) {
        es().bindBuffer(target, buffer);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        es().bindFramebuffer(target, framebuffer);
    }

    public static void glBindRenderbuffer(int target, int renderbuffer) {
        es().bindRenderbuffer(target, renderbuffer);
    }

    public static void glBindTexture(int target, int texture) {
        es().bindTexture(target, texture);
    }

    public static void glBlendColor(float red, float green, float blue, float alpha) {
        es().blendColor(red, green, blue, alpha);
    }

    public static void glBlendEquation(int mode) {
        es().blendEquation(mode);
    }

    public static void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        es().blendEquationSeparate(modeRGB, modeAlpha);
    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        es().blendFunc(sfactor, dfactor);
    }

    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha,
                                           int dfactorAlpha) {
        es().blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    public static void glBufferData(int target, long size, int usage) {
        es().bufferData(target, size, usage);
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        es().bufferData(target, Es.bytes(data), usage);
    }

    public static void glBufferData(int target, ShortBuffer data, int usage) {
        es().bufferData(target, Es.bytes(data), usage);
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        es().bufferData(target, Es.bytes(data), usage);
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        es().bufferData(target, Es.bytes(data), usage);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer data) {
        es().bufferSubData(target, offset, Es.bytes(data));
    }

    public static void glBufferSubData(int target, long offset, ShortBuffer data) {
        es().bufferSubData(target, offset, Es.bytes(data));
    }

    public static void glBufferSubData(int target, long offset, IntBuffer data) {
        es().bufferSubData(target, offset, Es.bytes(data));
    }

    public static void glBufferSubData(int target, long offset, FloatBuffer data) {
        es().bufferSubData(target, offset, Es.bytes(data));
    }

    public static int glCheckFramebufferStatus(int target) {
        return es().checkFramebufferStatus(target);
    }

    public static void glClear(int mask) {
        es().clear(mask);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        es().clearColor(red, green, blue, alpha);
    }

    public static void glClearDepthf(float d) {
        es().clearDepth(d);
    }

    public static void glClearStencil(int s) {
        es().clearStencil(s);
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        es().colorMask(red, green, blue, alpha);
    }

    public static void glCompileShader(int shader) {
        es().compileShader(shader);
    }

    public static void glCompressedTexImage2D(int target, int level, int internalformat,
                                              int width, int height, int border, int imageSize,
                                              long data) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().compressedTexImage2D(target, level, internalformat, width, height, border,
                    imageSize, data);
        } else {
            es().compressedTexImage2D(target, level, internalformat, width, height, border,
                    Es.bytes(MemoryUtil.memByteBuffer(data, imageSize)));
        }
    }

    public static void glCompressedTexImage2D(int target, int level, int internalformat,
                                              int width, int height, int border,
                                              ByteBuffer data) {
        es().compressedTexImage2D(target, level, internalformat, width, height, border,
                Es.bytes(data));
    }

    public static void glCompressedTexSubImage2D(int target, int level, int xoffset,
                                                 int yoffset, int width, int height, int format,
                                                 int imageSize, long data) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format,
                    imageSize, data);
        } else {
            es().compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format,
                    Es.bytes(MemoryUtil.memByteBuffer(data, imageSize)));
        }
    }

    public static void glCompressedTexSubImage2D(int target, int level, int xoffset,
                                                 int yoffset, int width, int height, int format,
                                                 ByteBuffer data) {
        es().compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format,
                Es.bytes(data));
    }

    public static void glCopyTexImage2D(int target, int level, int internalformat, int x, int y,
                                        int width, int height, int border) {
        es().copyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    public static void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                           int x, int y, int width, int height) {
        es().copyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public static int glCreateProgram() {
        return es().createProgram();
    }

    public static int glCreateShader(int type) {
        return es().createShader(type);
    }

    public static void glCullFace(int mode) {
        es().cullFace(mode);
    }

    public static void glDeleteBuffers(IntBuffer buffers) {
        Es.delete(buffers, es()::deleteBuffer);
    }

    public static void glDeleteBuffers(int buffer) {
        es().deleteBuffer(buffer);
    }

    public static void glDeleteFramebuffers(IntBuffer framebuffers) {
        Es.delete(framebuffers, es()::deleteFramebuffer);
    }

    public static void glDeleteFramebuffers(int framebuffer) {
        es().deleteFramebuffer(framebuffer);
    }

    public static void glDeleteProgram(int program) {
        es().deleteProgram(program);
    }

    public static void glDeleteRenderbuffers(IntBuffer renderbuffers) {
        Es.delete(renderbuffers, es()::deleteRenderbuffer);
    }

    public static void glDeleteRenderbuffers(int renderbuffer) {
        es().deleteRenderbuffer(renderbuffer);
    }

    public static void glDeleteShader(int shader) {
        es().deleteShader(shader);
    }

    public static void glDeleteTextures(IntBuffer textures) {
        Es.delete(textures, es()::deleteTexture);
    }

    public static void glDeleteTextures(int texture) {
        es().deleteTexture(texture);
    }

    public static void glDepthFunc(int func) {
        es().depthFunc(func);
    }

    public static void glDepthMask(boolean flag) {
        es().depthMask(flag);
    }

    public static void glDepthRangef(float n, float f) {
        es().depthRange(n, f);
    }

    public static void glDetachShader(int program, int shader) {
        es().detachShader(program, shader);
    }

    public static void glDisable(int cap) {
        es().disable(cap);
    }

    public static void glDisableVertexAttribArray(int index) {
        es().disableVertexAttribArray(index);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        Es.drawing().drawArrays(mode, first, count);
    }

    public static void glDrawElements(int mode, int count, int type, long indices) {
        if (Es.bound(GL_ELEMENT_ARRAY_BUFFER_BINDING)) {
            Es.drawing().drawElements(mode, count, type, indices);
        } else {
            Es.drawElements(mode, type,
                    MemoryUtil.memByteBuffer(indices, count * Es.typeSize(type)), -1, -1, -1);
        }
    }

    public static void glDrawElements(int mode, int type, ByteBuffer indices) {
        Es.drawElements(mode, type, indices, -1, -1, -1);
    }

    public static void glDrawElements(int mode, ByteBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_BYTE, indices, -1, -1, -1);
    }

    public static void glDrawElements(int mode, ShortBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_SHORT, indices, -1, -1, -1);
    }

    public static void glDrawElements(int mode, IntBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_INT, indices, -1, -1, -1);
    }

    public static void glEnable(int cap) {
        es().enable(cap);
    }

    public static void glEnableVertexAttribArray(int index) {
        es().enableVertexAttribArray(index);
    }

    public static void glFinish() {
        es().finish();
    }

    public static void glFlush() {
        es().flush();
    }

    public static void glFramebufferRenderbuffer(int target, int attachment,
                                                 int renderbuffertarget, int renderbuffer) {
        es().framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget,
                                              int texture, int level) {
        es().framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static void glFrontFace(int mode) {
        es().frontFace(mode);
    }

    public static void glGenBuffers(IntBuffer buffers) {
        Es.names(buffers, es()::createBuffer);
    }

    public static int glGenBuffers() {
        return es().createBuffer();
    }

    public static void glGenerateMipmap(int target) {
        es().generateMipmap(target);
    }

    public static void glGenFramebuffers(IntBuffer framebuffers) {
        Es.names(framebuffers, es()::createFramebuffer);
    }

    public static int glGenFramebuffers() {
        return es().createFramebuffer();
    }

    public static void glGenRenderbuffers(IntBuffer renderbuffers) {
        Es.names(renderbuffers, es()::createRenderbuffer);
    }

    public static int glGenRenderbuffers() {
        return es().createRenderbuffer();
    }

    public static void glGenTextures(IntBuffer textures) {
        Es.names(textures, es()::createTexture);
    }

    public static int glGenTextures() {
        return es().createTexture();
    }

    public static void glGetActiveAttrib(int program, int index, IntBuffer length,
                                         IntBuffer size, IntBuffer type, ByteBuffer name) {
        describe(es().getActiveAttrib(program, index), length, size, type, name);
    }

    public static String glGetActiveAttrib(int program, int index, int bufSize, IntBuffer size,
                                           IntBuffer type) {
        return Es.clip(describe(es().getActiveAttrib(program, index), size, type), bufSize);
    }

    public static String glGetActiveAttrib(int program, int index, IntBuffer size,
                                           IntBuffer type) {
        return describe(es().getActiveAttrib(program, index), size, type);
    }

    public static void glGetActiveUniform(int program, int index, IntBuffer length,
                                          IntBuffer size, IntBuffer type, ByteBuffer name) {
        describe(es().getActiveUniform(program, index), length, size, type, name);
    }

    public static String glGetActiveUniform(int program, int index, int bufSize, IntBuffer size,
                                            IntBuffer type) {
        return Es.clip(describe(es().getActiveUniform(program, index), size, type), bufSize);
    }

    public static String glGetActiveUniform(int program, int index, IntBuffer size,
                                            IntBuffer type) {
        return describe(es().getActiveUniform(program, index), size, type);
    }

    public static void glGetAttachedShaders(int program, IntBuffer count, IntBuffer shaders) {
        int[] attached = es().getAttachedShaders(program);
        Es.put(shaders, attached);
        Es.putIfPresent(count, Math.min(attached.length, shaders.remaining()));
    }

    public static int glGetAttribLocation(int program, ByteBuffer name) {
        return es().getAttribLocation(program, Es.text(name));
    }

    public static int glGetAttribLocation(int program, CharSequence name) {
        return es().getAttribLocation(program, name.toString());
    }

    public static void glGetBooleanv(int pname, ByteBuffer data) {
        Es.put(data, es().getParameter(pname));
    }

    public static boolean glGetBoolean(int pname) {
        return es().getParameter(pname)[0] != 0;
    }

    public static void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        params.put(params.position(), glGetBufferParameteri(target, pname));
    }

    public static int glGetBufferParameteri(int target, int pname) {
        return (int) bufferParameter(target, pname);
    }

    static long bufferParameter(int target, int pname) {
        long mapped = Es.mappedParameter(target, pname);
        return mapped >= 0 ? mapped : es().getBufferParameter(target, pname);
    }

    public static int glGetError() {
        return Es.error();
    }

    public static void glGetFloatv(int pname, FloatBuffer data) {
        Es.put(data, es().getParameter(pname));
    }

    public static float glGetFloat(int pname) {
        return (float) es().getParameter(pname)[0];
    }

    public static void glGetFramebufferAttachmentParameteriv(int target, int attachment,
                                                             int pname, IntBuffer params) {
        params.put(params.position(),
                es().getFramebufferAttachmentParameter(target, attachment, pname));
    }

    public static int glGetFramebufferAttachmentParameteri(int target, int attachment,
                                                           int pname) {
        return es().getFramebufferAttachmentParameter(target, attachment, pname);
    }

    public static void glGetIntegerv(int pname, IntBuffer data) {
        Es.put(data, es().getParameter(pname));
    }

    public static int glGetInteger(int pname) {
        return (int) (long) es().getParameter(pname)[0];
    }

    public static void glGetProgramiv(int program, int pname, IntBuffer params) {
        params.put(params.position(), es().getProgramParameter(program, pname));
    }

    public static int glGetProgrami(int program, int pname) {
        return es().getProgramParameter(program, pname);
    }

    public static void glGetProgramInfoLog(int program, IntBuffer length, ByteBuffer infoLog) {
        Es.write(es().getProgramInfoLog(program), length, infoLog);
    }

    public static String glGetProgramInfoLog(int program, int bufSize) {
        return Es.clip(es().getProgramInfoLog(program), bufSize);
    }

    public static String glGetProgramInfoLog(int program) {
        return es().getProgramInfoLog(program);
    }

    public static void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params) {
        params.put(params.position(), es().getRenderbufferParameter(target, pname));
    }

    public static int glGetRenderbufferParameteri(int target, int pname) {
        return es().getRenderbufferParameter(target, pname);
    }

    public static void glGetShaderiv(int shader, int pname, IntBuffer params) {
        params.put(params.position(), es().getShaderParameter(shader, pname));
    }

    public static int glGetShaderi(int shader, int pname) {
        return es().getShaderParameter(shader, pname);
    }

    public static void glGetShaderInfoLog(int shader, IntBuffer length, ByteBuffer infoLog) {
        Es.write(es().getShaderInfoLog(shader), length, infoLog);
    }

    public static String glGetShaderInfoLog(int shader, int bufSize) {
        return Es.clip(es().getShaderInfoLog(shader), bufSize);
    }

    public static String glGetShaderInfoLog(int shader) {
        return es().getShaderInfoLog(shader);
    }

    public static void glGetShaderPrecisionFormat(int shadertype, int precisiontype,
                                                  IntBuffer range, IntBuffer precision) {
        int[] format = es().getShaderPrecisionFormat(shadertype, precisiontype);
        range.put(range.position(), format[0]);
        range.put(range.position() + 1, format[1]);
        precision.put(precision.position(), format[2]);
    }

    public static void glGetShaderSource(int shader, IntBuffer length, ByteBuffer source) {
        Es.write(es().getShaderSource(shader), length, source);
    }

    public static String glGetShaderSource(int shader, int bufSize) {
        return Es.clip(es().getShaderSource(shader), bufSize);
    }

    public static String glGetShaderSource(int shader) {
        return es().getShaderSource(shader);
    }

    public static String glGetString(int name) {
        return es().getString(name);
    }

    public static void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        params.put(params.position(), glGetTexParameterf(target, pname));
    }

    public static float glGetTexParameterf(int target, int pname) {
        return (float) es().getTexParameter(target, pname);
    }

    public static void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        params.put(params.position(), glGetTexParameteri(target, pname));
    }

    public static int glGetTexParameteri(int target, int pname) {
        return (int) es().getTexParameter(target, pname);
    }

    public static void glGetUniformfv(int program, int location, FloatBuffer params) {
        Es.put(params, es().getUniform(program, location));
    }

    public static float glGetUniformf(int program, int location) {
        return (float) es().getUniform(program, location)[0];
    }

    public static void glGetUniformiv(int program, int location, IntBuffer params) {
        Es.put(params, es().getUniform(program, location));
    }

    public static int glGetUniformi(int program, int location) {
        return (int) (long) es().getUniform(program, location)[0];
    }

    public static int glGetUniformLocation(int program, ByteBuffer name) {
        return es().getUniformLocation(program, Es.text(name));
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return es().getUniformLocation(program, name.toString());
    }

    public static void glGetVertexAttribfv(int index, int pname, FloatBuffer params) {
        Es.put(params, es().getVertexAttrib(index, pname));
    }

    public static void glGetVertexAttribiv(int index, int pname, IntBuffer params) {
        Es.put(params, es().getVertexAttrib(index, pname));
    }

    public static void glGetVertexAttribPointerv(int index, int pname, PointerBuffer pointer) {
        pointer.put(pointer.position(), glGetVertexAttribPointer(index, pname));
    }

    public static long glGetVertexAttribPointer(int index, int pname) {
        Buffer client = Es.clientPointer(index);
        return client != null ? MemoryUtil.memAddress(client)
                : es().getVertexAttribOffset(index, pname);
    }

    public static void glHint(int target, int mode) {
        es().hint(target, mode);
    }

    public static boolean glIsBuffer(int buffer) {
        return es().isBuffer(buffer);
    }

    public static boolean glIsEnabled(int cap) {
        return es().isEnabled(cap);
    }

    public static boolean glIsFramebuffer(int framebuffer) {
        return es().isFramebuffer(framebuffer);
    }

    public static boolean glIsProgram(int program) {
        return es().isProgram(program);
    }

    public static boolean glIsRenderbuffer(int renderbuffer) {
        return es().isRenderbuffer(renderbuffer);
    }

    public static boolean glIsShader(int shader) {
        return es().isShader(shader);
    }

    public static boolean glIsTexture(int texture) {
        return es().isTexture(texture);
    }

    public static void glLineWidth(float width) {
        es().lineWidth(width);
    }

    public static void glLinkProgram(int program) {
        es().linkProgram(program);
    }

    public static void glPixelStorei(int pname, int param) {
        es().pixelStorei(pname, param);
    }

    public static void glPolygonOffset(float factor, float units) {
        es().polygonOffset(factor, units);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    ByteBuffer pixels) {
        readPixels(x, y, width, height, format, type, pixels);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    long pixels) {
        if (Es.bound(Es.PIXEL_PACK_BUFFER_BINDING)) {
            es().readPixels(x, y, width, height, format, type, pixels);
        } else {
            readPixels(x, y, width, height, format, type, MemoryUtil.memByteBuffer(pixels,
                    Es.pixelBytes(format, type, width, height, 1)));
        }
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    ShortBuffer pixels) {
        readPixels(x, y, width, height, format, type, pixels);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    IntBuffer pixels) {
        readPixels(x, y, width, height, format, type, pixels);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    FloatBuffer pixels) {
        readPixels(x, y, width, height, format, type, pixels);
    }

    private static void readPixels(int x, int y, int width, int height, int format, int type,
                                   Buffer pixels) {
        byte[] read = new byte[pixels.remaining() * Buffers.elementSize(pixels)];
        es().readPixels(x, y, width, height, format, type, read);
        Es.write(pixels, read);
    }

    public static void glReleaseShaderCompiler() {
    }

    public static void glRenderbufferStorage(int target, int internalformat, int width,
                                             int height) {
        es().renderbufferStorage(target, internalformat, width, height);
    }

    public static void glSampleCoverage(float value, boolean invert) {
        es().sampleCoverage(value, invert);
    }

    public static void glScissor(int x, int y, int width, int height) {
        es().scissor(x, y, width, height);
    }

    /** webgl compiles from source only, so it lists no binary formats */
    public static void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        Es.fail(GL_INVALID_ENUM);
    }

    public static void glShaderSource(int shader, PointerBuffer string, IntBuffer length) {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < string.remaining(); i++) {
            source.append(Es.text(string, i, length));
        }
        es().shaderSource(shader, source.toString());
    }

    public static void glShaderSource(int shader, CharSequence... string) {
        es().shaderSource(shader, String.join("", string));
    }

    public static void glShaderSource(int shader, CharSequence string) {
        es().shaderSource(shader, string.toString());
    }

    public static void glStencilFunc(int func, int ref, int mask) {
        es().stencilFuncSeparate(GL_FRONT_AND_BACK, func, ref, mask);
    }

    public static void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        es().stencilFuncSeparate(face, func, ref, mask);
    }

    public static void glStencilMask(int mask) {
        es().stencilMaskSeparate(GL_FRONT_AND_BACK, mask);
    }

    public static void glStencilMaskSeparate(int face, int mask) {
        es().stencilMaskSeparate(face, mask);
    }

    public static void glStencilOp(int fail, int zfail, int zpass) {
        es().stencilOpSeparate(GL_FRONT_AND_BACK, fail, zfail, zpass);
    }

    public static void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        es().stencilOpSeparate(face, sfail, dpfail, dppass);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type,
                                    ByteBuffer pixels) {
        es().texImage2D(target, level, internalformat, width, height, border, format, type,
                Es.bytes(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type, long pixels) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().texImage2D(target, level, internalformat, width, height, border, format, type,
                    pixels);
        } else {
            es().texImage2D(target, level, internalformat, width, height, border, format, type,
                    pixels == 0 ? null : Es.bytes(MemoryUtil.memByteBuffer(pixels,
                            Es.pixelBytes(format, type, width, height, 1))));
        }
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type,
                                    ShortBuffer pixels) {
        es().texImage2D(target, level, internalformat, width, height, border, format, type,
                Es.bytes(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type,
                                    IntBuffer pixels) {
        es().texImage2D(target, level, internalformat, width, height, border, format, type,
                Es.bytes(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type,
                                    FloatBuffer pixels) {
        es().texImage2D(target, level, internalformat, width, height, border, format, type,
                Es.bytes(pixels));
    }

    public static void glTexParameterf(int target, int pname, float param) {
        es().texParameterf(target, pname, param);
    }

    public static void glTexParameterfv(int target, int pname, FloatBuffer params) {
        es().texParameterf(target, pname, params.get(params.position()));
    }

    public static void glTexParameteri(int target, int pname, int param) {
        es().texParameteri(target, pname, param);
    }

    public static void glTexParameteriv(int target, int pname, IntBuffer params) {
        es().texParameteri(target, pname, params.get(params.position()));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                       int width, int height, int format, int type,
                                       ByteBuffer pixels) {
        es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.bytes(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                       int width, int height, int format, int type,
                                       long pixels) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                    pixels);
        } else {
            es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                    Es.bytes(MemoryUtil.memByteBuffer(pixels,
                            Es.pixelBytes(format, type, width, height, 1))));
        }
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                       int width, int height, int format, int type,
                                       ShortBuffer pixels) {
        es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.bytes(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                       int width, int height, int format, int type,
                                       IntBuffer pixels) {
        es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.bytes(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                       int width, int height, int format, int type,
                                       FloatBuffer pixels) {
        es().texSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.bytes(pixels));
    }

    public static void glUniform1f(int location, float v0) {
        es().uniform1f(location, v0);
    }

    public static void glUniform1fv(int location, FloatBuffer value) {
        es().uniformfv(location, 1, Es.floats(value));
    }

    public static void glUniform1i(int location, int v0) {
        es().uniform1i(location, v0);
    }

    public static void glUniform1iv(int location, IntBuffer value) {
        es().uniformiv(location, 1, Es.ints(value));
    }

    public static void glUniform2f(int location, float v0, float v1) {
        es().uniform2f(location, v0, v1);
    }

    public static void glUniform2fv(int location, FloatBuffer value) {
        es().uniformfv(location, 2, Es.floats(value));
    }

    public static void glUniform2i(int location, int v0, int v1) {
        es().uniform2i(location, v0, v1);
    }

    public static void glUniform2iv(int location, IntBuffer value) {
        es().uniformiv(location, 2, Es.ints(value));
    }

    public static void glUniform3f(int location, float v0, float v1, float v2) {
        es().uniform3f(location, v0, v1, v2);
    }

    public static void glUniform3fv(int location, FloatBuffer value) {
        es().uniformfv(location, 3, Es.floats(value));
    }

    public static void glUniform3i(int location, int v0, int v1, int v2) {
        es().uniform3i(location, v0, v1, v2);
    }

    public static void glUniform3iv(int location, IntBuffer value) {
        es().uniformiv(location, 3, Es.ints(value));
    }

    public static void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        es().uniform4f(location, v0, v1, v2, v3);
    }

    public static void glUniform4fv(int location, FloatBuffer value) {
        es().uniformfv(location, 4, Es.floats(value));
    }

    public static void glUniform4i(int location, int v0, int v1, int v2, int v3) {
        es().uniform4i(location, v0, v1, v2, v3);
    }

    public static void glUniform4iv(int location, IntBuffer value) {
        es().uniformiv(location, 4, Es.ints(value));
    }

    public static void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 2, 2, transpose, Es.floats(value));
    }

    public static void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 3, 3, transpose, Es.floats(value));
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 4, 4, transpose, Es.floats(value));
    }

    public static void glUseProgram(int program) {
        es().useProgram(program);
    }

    public static void glValidateProgram(int program) {
        es().validateProgram(program);
    }

    public static void glVertexAttrib1f(int index, float x) {
        es().vertexAttrib4f(index, x, 0, 0, 1);
    }

    public static void glVertexAttrib1fv(int index, FloatBuffer v) {
        es().vertexAttrib4f(index, v.get(v.position()), 0, 0, 1);
    }

    public static void glVertexAttrib2f(int index, float x, float y) {
        es().vertexAttrib4f(index, x, y, 0, 1);
    }

    public static void glVertexAttrib2fv(int index, FloatBuffer v) {
        int at = v.position();
        es().vertexAttrib4f(index, v.get(at), v.get(at + 1), 0, 1);
    }

    public static void glVertexAttrib3f(int index, float x, float y, float z) {
        es().vertexAttrib4f(index, x, y, z, 1);
    }

    public static void glVertexAttrib3fv(int index, FloatBuffer v) {
        int at = v.position();
        es().vertexAttrib4f(index, v.get(at), v.get(at + 1), v.get(at + 2), 1);
    }

    public static void glVertexAttrib4f(int index, float x, float y, float z, float w) {
        es().vertexAttrib4f(index, x, y, z, w);
    }

    public static void glVertexAttrib4fv(int index, FloatBuffer v) {
        int at = v.position();
        es().vertexAttrib4f(index, v.get(at), v.get(at + 1), v.get(at + 2), v.get(at + 3));
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, ByteBuffer pointer) {
        Es.pointAt(index, pointer, size, type, normalized, stride, false);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, long pointer) {
        Es.pointAt(index);
        es().vertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, ShortBuffer pointer) {
        Es.pointAt(index, pointer, size, type, normalized, stride, false);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, IntBuffer pointer) {
        Es.pointAt(index, pointer, size, type, normalized, stride, false);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized,
                                             int stride, FloatBuffer pointer) {
        Es.pointAt(index, pointer, size, type, normalized, stride, false);
    }

    public static void glViewport(int x, int y, int width, int height) {
        es().viewport(x, y, width, height);
    }

    public static void glBufferData(int target, short[] data, int usage) {
        glBufferData(target, Es.wrap(data), usage);
    }

    public static void glBufferData(int target, int[] data, int usage) {
        glBufferData(target, Es.wrap(data), usage);
    }

    public static void glBufferData(int target, float[] data, int usage) {
        glBufferData(target, Es.wrap(data), usage);
    }

    public static void glBufferSubData(int target, long offset, short[] data) {
        glBufferSubData(target, offset, Es.wrap(data));
    }

    public static void glBufferSubData(int target, long offset, int[] data) {
        glBufferSubData(target, offset, Es.wrap(data));
    }

    public static void glBufferSubData(int target, long offset, float[] data) {
        glBufferSubData(target, offset, Es.wrap(data));
    }

    public static void glDeleteBuffers(int[] buffers) {
        glDeleteBuffers(Es.wrap(buffers));
    }

    public static void glDeleteFramebuffers(int[] framebuffers) {
        glDeleteFramebuffers(Es.wrap(framebuffers));
    }

    public static void glDeleteRenderbuffers(int[] renderbuffers) {
        glDeleteRenderbuffers(Es.wrap(renderbuffers));
    }

    public static void glDeleteTextures(int[] textures) {
        glDeleteTextures(Es.wrap(textures));
    }

    public static void glGenBuffers(int[] buffers) {
        glGenBuffers(Es.wrap(buffers));
    }

    public static void glGenFramebuffers(int[] framebuffers) {
        glGenFramebuffers(Es.wrap(framebuffers));
    }

    public static void glGenRenderbuffers(int[] renderbuffers) {
        glGenRenderbuffers(Es.wrap(renderbuffers));
    }

    public static void glGenTextures(int[] textures) {
        glGenTextures(Es.wrap(textures));
    }

    public static void glGetActiveAttrib(int program, int index, int[] length, int[] size,
                                         int[] type, ByteBuffer name) {
        glGetActiveAttrib(program, index, Es.wrap(length), Es.wrap(size), Es.wrap(type), name);
    }

    public static void glGetActiveUniform(int program, int index, int[] length, int[] size,
                                          int[] type, ByteBuffer name) {
        glGetActiveUniform(program, index, Es.wrap(length), Es.wrap(size), Es.wrap(type), name);
    }

    public static void glGetAttachedShaders(int program, int[] count, int[] shaders) {
        glGetAttachedShaders(program, Es.wrap(count), Es.wrap(shaders));
    }

    public static void glGetBufferParameteriv(int target, int pname, int[] params) {
        glGetBufferParameteriv(target, pname, Es.wrap(params));
    }

    public static void glGetFloatv(int pname, float[] data) {
        glGetFloatv(pname, Es.wrap(data));
    }

    public static void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname,
                                                             int[] params) {
        glGetFramebufferAttachmentParameteriv(target, attachment, pname, Es.wrap(params));
    }

    public static void glGetIntegerv(int pname, int[] data) {
        glGetIntegerv(pname, Es.wrap(data));
    }

    public static void glGetProgramiv(int program, int pname, int[] params) {
        glGetProgramiv(program, pname, Es.wrap(params));
    }

    public static void glGetProgramInfoLog(int program, int[] length, ByteBuffer infoLog) {
        glGetProgramInfoLog(program, Es.wrap(length), infoLog);
    }

    public static void glGetRenderbufferParameteriv(int target, int pname, int[] params) {
        glGetRenderbufferParameteriv(target, pname, Es.wrap(params));
    }

    public static void glGetShaderiv(int shader, int pname, int[] params) {
        glGetShaderiv(shader, pname, Es.wrap(params));
    }

    public static void glGetShaderInfoLog(int shader, int[] length, ByteBuffer infoLog) {
        glGetShaderInfoLog(shader, Es.wrap(length), infoLog);
    }

    public static void glGetShaderPrecisionFormat(int shadertype, int precisiontype, int[] range,
                                                  int[] precision) {
        glGetShaderPrecisionFormat(shadertype, precisiontype, Es.wrap(range), Es.wrap(precision));
    }

    public static void glGetShaderSource(int shader, int[] length, ByteBuffer source) {
        glGetShaderSource(shader, Es.wrap(length), source);
    }

    public static void glGetTexParameterfv(int target, int pname, float[] params) {
        glGetTexParameterfv(target, pname, Es.wrap(params));
    }

    public static void glGetTexParameteriv(int target, int pname, int[] params) {
        glGetTexParameteriv(target, pname, Es.wrap(params));
    }

    public static void glGetUniformfv(int program, int location, float[] params) {
        glGetUniformfv(program, location, Es.wrap(params));
    }

    public static void glGetUniformiv(int program, int location, int[] params) {
        glGetUniformiv(program, location, Es.wrap(params));
    }

    public static void glGetVertexAttribfv(int index, int pname, float[] params) {
        glGetVertexAttribfv(index, pname, Es.wrap(params));
    }

    public static void glGetVertexAttribiv(int index, int pname, int[] params) {
        glGetVertexAttribiv(index, pname, Es.wrap(params));
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    short[] pixels) {
        glReadPixels(x, y, width, height, format, type, Es.wrap(pixels));
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    int[] pixels) {
        glReadPixels(x, y, width, height, format, type, Es.wrap(pixels));
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type,
                                    float[] pixels) {
        glReadPixels(x, y, width, height, format, type, Es.wrap(pixels));
    }

    public static void glShaderBinary(int[] shaders, int binaryformat, ByteBuffer binary) {
        glShaderBinary(Es.wrap(shaders), binaryformat, binary);
    }

    public static void glShaderSource(int shader, PointerBuffer string, int[] length) {
        glShaderSource(shader, string, Es.wrap(length));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type, short[] pixels) {
        glTexImage2D(target, level, internalformat, width, height, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type, int[] pixels) {
        glTexImage2D(target, level, internalformat, width, height, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width,
                                    int height, int border, int format, int type, float[] pixels) {
        glTexImage2D(target, level, internalformat, width, height, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexParameterfv(int target, int pname, float[] params) {
        glTexParameterfv(target, pname, Es.wrap(params));
    }

    public static void glTexParameteriv(int target, int pname, int[] params) {
        glTexParameteriv(target, pname, Es.wrap(params));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
                                       int height, int format, int type, short[] pixels) {
        glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.wrap(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
                                       int height, int format, int type, int[] pixels) {
        glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.wrap(pixels));
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
                                       int height, int format, int type, float[] pixels) {
        glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                Es.wrap(pixels));
    }

    public static void glUniform1fv(int location, float[] value) {
        glUniform1fv(location, Es.wrap(value));
    }

    public static void glUniform1iv(int location, int[] value) {
        glUniform1iv(location, Es.wrap(value));
    }

    public static void glUniform2fv(int location, float[] value) {
        glUniform2fv(location, Es.wrap(value));
    }

    public static void glUniform2iv(int location, int[] value) {
        glUniform2iv(location, Es.wrap(value));
    }

    public static void glUniform3fv(int location, float[] value) {
        glUniform3fv(location, Es.wrap(value));
    }

    public static void glUniform3iv(int location, int[] value) {
        glUniform3iv(location, Es.wrap(value));
    }

    public static void glUniform4fv(int location, float[] value) {
        glUniform4fv(location, Es.wrap(value));
    }

    public static void glUniform4iv(int location, int[] value) {
        glUniform4iv(location, Es.wrap(value));
    }

    public static void glUniformMatrix2fv(int location, boolean transpose, float[] value) {
        glUniformMatrix2fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix3fv(int location, boolean transpose, float[] value) {
        glUniformMatrix3fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, float[] value) {
        glUniformMatrix4fv(location, transpose, Es.wrap(value));
    }

    public static void glVertexAttrib1fv(int index, float[] v) {
        glVertexAttrib1fv(index, Es.wrap(v));
    }

    public static void glVertexAttrib2fv(int index, float[] v) {
        glVertexAttrib2fv(index, Es.wrap(v));
    }

    public static void glVertexAttrib3fv(int index, float[] v) {
        glVertexAttrib3fv(index, Es.wrap(v));
    }

    public static void glVertexAttrib4fv(int index, float[] v) {
        glVertexAttrib4fv(index, Es.wrap(v));
    }
}
