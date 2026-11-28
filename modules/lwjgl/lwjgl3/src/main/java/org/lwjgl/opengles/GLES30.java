package org.lwjgl.opengles;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

/** gl es 3.0 on the webgl2 context */
public class GLES30 extends GLES20 {

    public static final int GL_READ_BUFFER = 0xC02;
    public static final int GL_UNPACK_ROW_LENGTH = 0xCF2;
    public static final int GL_UNPACK_SKIP_ROWS = 0xCF3;
    public static final int GL_UNPACK_SKIP_PIXELS = 0xCF4;
    public static final int GL_PACK_ROW_LENGTH = 0xD02;
    public static final int GL_PACK_SKIP_ROWS = 0xD03;
    public static final int GL_PACK_SKIP_PIXELS = 0xD04;
    public static final int GL_COLOR = 0x1800;
    public static final int GL_DEPTH = 0x1801;
    public static final int GL_STENCIL = 0x1802;
    public static final int GL_RED = 0x1903;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGB10_A2 = 0x8059;
    public static final int GL_TEXTURE_BINDING_3D = 0x806A;
    public static final int GL_UNPACK_SKIP_IMAGES = 0x806D;
    public static final int GL_UNPACK_IMAGE_HEIGHT = 0x806E;
    public static final int GL_TEXTURE_3D = 0x806F;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_MAX_3D_TEXTURE_SIZE = 0x8073;
    public static final int GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368;
    public static final int GL_MAX_ELEMENTS_VERTICES = 0x80E8;
    public static final int GL_MAX_ELEMENTS_INDICES = 0x80E9;
    public static final int GL_TEXTURE_MIN_LOD = 0x813A;
    public static final int GL_TEXTURE_MAX_LOD = 0x813B;
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_MIN = 0x8007;
    public static final int GL_MAX = 0x8008;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_MAX_TEXTURE_LOD_BIAS = 0x84FD;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
    public static final int GL_CURRENT_QUERY = 0x8865;
    public static final int GL_QUERY_RESULT = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;
    public static final int GL_BUFFER_MAPPED = 0x88BC;
    public static final int GL_BUFFER_MAP_POINTER = 0x88BD;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_STREAM_COPY = 0x88E2;
    public static final int GL_STATIC_READ = 0x88E5;
    public static final int GL_STATIC_COPY = 0x88E6;
    public static final int GL_DYNAMIC_READ = 0x88E9;
    public static final int GL_DYNAMIC_COPY = 0x88EA;
    public static final int GL_MAX_DRAW_BUFFERS = 0x8824;
    public static final int GL_DRAW_BUFFER0 = 0x8825;
    public static final int GL_DRAW_BUFFER1 = 0x8826;
    public static final int GL_DRAW_BUFFER2 = 0x8827;
    public static final int GL_DRAW_BUFFER3 = 0x8828;
    public static final int GL_DRAW_BUFFER4 = 0x8829;
    public static final int GL_DRAW_BUFFER5 = 0x882A;
    public static final int GL_DRAW_BUFFER6 = 0x882B;
    public static final int GL_DRAW_BUFFER7 = 0x882C;
    public static final int GL_DRAW_BUFFER8 = 0x882D;
    public static final int GL_DRAW_BUFFER9 = 0x882E;
    public static final int GL_DRAW_BUFFER10 = 0x882F;
    public static final int GL_DRAW_BUFFER11 = 0x8830;
    public static final int GL_DRAW_BUFFER12 = 0x8831;
    public static final int GL_DRAW_BUFFER13 = 0x8832;
    public static final int GL_DRAW_BUFFER14 = 0x8833;
    public static final int GL_DRAW_BUFFER15 = 0x8834;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    public static final int GL_SAMPLER_3D = 0x8B5F;
    public static final int GL_SAMPLER_2D_SHADOW = 0x8B62;
    public static final int GL_FRAGMENT_SHADER_DERIVATIVE_HINT = 0x8B8B;
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    public static final int GL_PIXEL_PACK_BUFFER_BINDING = 0x88ED;
    public static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    public static final int GL_FLOAT_MAT2x3 = 0x8B65;
    public static final int GL_FLOAT_MAT2x4 = 0x8B66;
    public static final int GL_FLOAT_MAT3x2 = 0x8B67;
    public static final int GL_FLOAT_MAT3x4 = 0x8B68;
    public static final int GL_FLOAT_MAT4x2 = 0x8B69;
    public static final int GL_FLOAT_MAT4x3 = 0x8B6A;
    public static final int GL_SRGB = 0x8C40;
    public static final int GL_SRGB8 = 0x8C41;
    public static final int GL_SRGB8_ALPHA8 = 0x8C43;
    public static final int GL_COMPARE_REF_TO_TEXTURE = 0x884E;
    public static final int GL_MAJOR_VERSION = 0x821B;
    public static final int GL_MINOR_VERSION = 0x821C;
    public static final int GL_NUM_EXTENSIONS = 0x821D;
    public static final int GL_RGBA32F = 0x8814;
    public static final int GL_RGB32F = 0x8815;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_RGB16F = 0x881B;
    public static final int GL_VERTEX_ATTRIB_ARRAY_INTEGER = 0x88FD;
    public static final int GL_MAX_ARRAY_TEXTURE_LAYERS = 0x88FF;
    public static final int GL_MIN_PROGRAM_TEXEL_OFFSET = 0x8904;
    public static final int GL_MAX_PROGRAM_TEXEL_OFFSET = 0x8905;
    public static final int GL_MAX_VARYING_COMPONENTS = 0x8B4B;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_TEXTURE_BINDING_2D_ARRAY = 0x8C1D;
    public static final int GL_R11F_G11F_B10F = 0x8C3A;
    public static final int GL_UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B;
    public static final int GL_RGB9_E5 = 0x8C3D;
    public static final int GL_UNSIGNED_INT_5_9_9_9_REV = 0x8C3E;
    public static final int GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH = 0x8C76;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER_MODE = 0x8C7F;
    public static final int GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS = 0x8C80;
    public static final int GL_TRANSFORM_FEEDBACK_VARYINGS = 0x8C83;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER_START = 0x8C84;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER_SIZE = 0x8C85;
    public static final int GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
    public static final int GL_RASTERIZER_DISCARD = 0x8C89;
    public static final int GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 0x8C8A;
    public static final int GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS = 0x8C8B;
    public static final int GL_INTERLEAVED_ATTRIBS = 0x8C8C;
    public static final int GL_SEPARATE_ATTRIBS = 0x8C8D;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER_BINDING = 0x8C8F;
    public static final int GL_RGBA32UI = 0x8D70;
    public static final int GL_RGB32UI = 0x8D71;
    public static final int GL_RGBA16UI = 0x8D76;
    public static final int GL_RGB16UI = 0x8D77;
    public static final int GL_RGBA8UI = 0x8D7C;
    public static final int GL_RGB8UI = 0x8D7D;
    public static final int GL_RGBA32I = 0x8D82;
    public static final int GL_RGB32I = 0x8D83;
    public static final int GL_RGBA16I = 0x8D88;
    public static final int GL_RGB16I = 0x8D89;
    public static final int GL_RGBA8I = 0x8D8E;
    public static final int GL_RGB8I = 0x8D8F;
    public static final int GL_RED_INTEGER = 0x8D94;
    public static final int GL_RGB_INTEGER = 0x8D98;
    public static final int GL_RGBA_INTEGER = 0x8D99;
    public static final int GL_SAMPLER_2D_ARRAY = 0x8DC1;
    public static final int GL_SAMPLER_2D_ARRAY_SHADOW = 0x8DC4;
    public static final int GL_SAMPLER_CUBE_SHADOW = 0x8DC5;
    public static final int GL_UNSIGNED_INT_VEC2 = 0x8DC6;
    public static final int GL_UNSIGNED_INT_VEC3 = 0x8DC7;
    public static final int GL_UNSIGNED_INT_VEC4 = 0x8DC8;
    public static final int GL_INT_SAMPLER_2D = 0x8DCA;
    public static final int GL_INT_SAMPLER_3D = 0x8DCB;
    public static final int GL_INT_SAMPLER_CUBE = 0x8DCC;
    public static final int GL_INT_SAMPLER_2D_ARRAY = 0x8DCF;
    public static final int GL_UNSIGNED_INT_SAMPLER_2D = 0x8DD2;
    public static final int GL_UNSIGNED_INT_SAMPLER_3D = 0x8DD3;
    public static final int GL_UNSIGNED_INT_SAMPLER_CUBE = 0x8DD4;
    public static final int GL_UNSIGNED_INT_SAMPLER_2D_ARRAY = 0x8DD7;
    public static final int GL_BUFFER_ACCESS_FLAGS = 0x911F;
    public static final int GL_BUFFER_MAP_LENGTH = 0x9120;
    public static final int GL_BUFFER_MAP_OFFSET = 0x9121;
    public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
    public static final int GL_DEPTH32F_STENCIL8 = 0x8CAD;
    public static final int GL_FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING = 0x8210;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE = 0x8211;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE = 0x8212;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = 0x8213;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = 0x8214;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = 0x8215;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = 0x8216;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = 0x8217;
    public static final int GL_FRAMEBUFFER_DEFAULT = 0x8218;
    public static final int GL_FRAMEBUFFER_UNDEFINED = 0x8219;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    public static final int GL_DEPTH_STENCIL = 0x84F9;
    public static final int GL_UNSIGNED_INT_24_8 = 0x84FA;
    public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
    public static final int GL_UNSIGNED_NORMALIZED = 0x8C17;
    public static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    public static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    public static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    public static final int GL_RENDERBUFFER_SAMPLES = 0x8CAB;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER = 0x8CD4;
    public static final int GL_MAX_COLOR_ATTACHMENTS = 0x8CDF;
    public static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
    public static final int GL_COLOR_ATTACHMENT2 = 0x8CE2;
    public static final int GL_COLOR_ATTACHMENT3 = 0x8CE3;
    public static final int GL_COLOR_ATTACHMENT4 = 0x8CE4;
    public static final int GL_COLOR_ATTACHMENT5 = 0x8CE5;
    public static final int GL_COLOR_ATTACHMENT6 = 0x8CE6;
    public static final int GL_COLOR_ATTACHMENT7 = 0x8CE7;
    public static final int GL_COLOR_ATTACHMENT8 = 0x8CE8;
    public static final int GL_COLOR_ATTACHMENT9 = 0x8CE9;
    public static final int GL_COLOR_ATTACHMENT10 = 0x8CEA;
    public static final int GL_COLOR_ATTACHMENT11 = 0x8CEB;
    public static final int GL_COLOR_ATTACHMENT12 = 0x8CEC;
    public static final int GL_COLOR_ATTACHMENT13 = 0x8CED;
    public static final int GL_COLOR_ATTACHMENT14 = 0x8CEE;
    public static final int GL_COLOR_ATTACHMENT15 = 0x8CEF;
    public static final int GL_COLOR_ATTACHMENT16 = 0x8CF0;
    public static final int GL_COLOR_ATTACHMENT17 = 0x8CF1;
    public static final int GL_COLOR_ATTACHMENT18 = 0x8CF2;
    public static final int GL_COLOR_ATTACHMENT19 = 0x8CF3;
    public static final int GL_COLOR_ATTACHMENT20 = 0x8CF4;
    public static final int GL_COLOR_ATTACHMENT21 = 0x8CF5;
    public static final int GL_COLOR_ATTACHMENT22 = 0x8CF6;
    public static final int GL_COLOR_ATTACHMENT23 = 0x8CF7;
    public static final int GL_COLOR_ATTACHMENT24 = 0x8CF8;
    public static final int GL_COLOR_ATTACHMENT25 = 0x8CF9;
    public static final int GL_COLOR_ATTACHMENT26 = 0x8CFA;
    public static final int GL_COLOR_ATTACHMENT27 = 0x8CFB;
    public static final int GL_COLOR_ATTACHMENT28 = 0x8CFC;
    public static final int GL_COLOR_ATTACHMENT29 = 0x8CFD;
    public static final int GL_COLOR_ATTACHMENT30 = 0x8CFE;
    public static final int GL_COLOR_ATTACHMENT31 = 0x8CFF;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56;
    public static final int GL_MAX_SAMPLES = 0x8D57;
    public static final int GL_HALF_FLOAT = 0x140B;
    public static final int GL_MAP_READ_BIT = 0x1;
    public static final int GL_MAP_WRITE_BIT = 0x2;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x4;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x8;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x10;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x20;
    public static final int GL_RG = 0x8227;
    public static final int GL_RG_INTEGER = 0x8228;
    public static final int GL_R8 = 0x8229;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_R16F = 0x822D;
    public static final int GL_R32F = 0x822E;
    public static final int GL_RG16F = 0x822F;
    public static final int GL_RG32F = 0x8230;
    public static final int GL_R8I = 0x8231;
    public static final int GL_R8UI = 0x8232;
    public static final int GL_R16I = 0x8233;
    public static final int GL_R16UI = 0x8234;
    public static final int GL_R32I = 0x8235;
    public static final int GL_R32UI = 0x8236;
    public static final int GL_RG8I = 0x8237;
    public static final int GL_RG8UI = 0x8238;
    public static final int GL_RG16I = 0x8239;
    public static final int GL_RG16UI = 0x823A;
    public static final int GL_RG32I = 0x823B;
    public static final int GL_RG32UI = 0x823C;
    public static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    public static final int GL_R8_SNORM = 0x8F94;
    public static final int GL_RG8_SNORM = 0x8F95;
    public static final int GL_RGB8_SNORM = 0x8F96;
    public static final int GL_RGBA8_SNORM = 0x8F97;
    public static final int GL_SIGNED_NORMALIZED = 0x8F9C;
    public static final int GL_PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_COPY_READ_BUFFER_BINDING = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER_BINDING = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_UNIFORM_BUFFER_BINDING = 0x8A28;
    public static final int GL_UNIFORM_BUFFER_START = 0x8A29;
    public static final int GL_UNIFORM_BUFFER_SIZE = 0x8A2A;
    public static final int GL_MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B;
    public static final int GL_MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D;
    public static final int GL_MAX_COMBINED_UNIFORM_BLOCKS = 0x8A2E;
    public static final int GL_MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F;
    public static final int GL_MAX_UNIFORM_BLOCK_SIZE = 0x8A30;
    public static final int GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 0x8A31;
    public static final int GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 0x8A33;
    public static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;
    public static final int GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 0x8A35;
    public static final int GL_ACTIVE_UNIFORM_BLOCKS = 0x8A36;
    public static final int GL_UNIFORM_TYPE = 0x8A37;
    public static final int GL_UNIFORM_SIZE = 0x8A38;
    public static final int GL_UNIFORM_NAME_LENGTH = 0x8A39;
    public static final int GL_UNIFORM_BLOCK_INDEX = 0x8A3A;
    public static final int GL_UNIFORM_OFFSET = 0x8A3B;
    public static final int GL_UNIFORM_ARRAY_STRIDE = 0x8A3C;
    public static final int GL_UNIFORM_MATRIX_STRIDE = 0x8A3D;
    public static final int GL_UNIFORM_IS_ROW_MAJOR = 0x8A3E;
    public static final int GL_UNIFORM_BLOCK_BINDING = 0x8A3F;
    public static final int GL_UNIFORM_BLOCK_DATA_SIZE = 0x8A40;
    public static final int GL_UNIFORM_BLOCK_NAME_LENGTH = 0x8A41;
    public static final int GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS = 0x8A42;
    public static final int GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 0x8A43;
    public static final int GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 0x8A44;
    public static final int GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 0x8A46;
    public static final int GL_INVALID_INDEX = -1;
    public static final int GL_MAX_VERTEX_OUTPUT_COMPONENTS = 0x9122;
    public static final int GL_MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125;
    public static final long GL_TIMEOUT_IGNORED = -1L;
    public static final int GL_MAX_SERVER_WAIT_TIMEOUT = 0x9111;
    public static final int GL_OBJECT_TYPE = 0x9112;
    public static final int GL_SYNC_CONDITION = 0x9113;
    public static final int GL_SYNC_STATUS = 0x9114;
    public static final int GL_SYNC_FLAGS = 0x9115;
    public static final int GL_SYNC_FENCE = 0x9116;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_UNSIGNALED = 0x9118;
    public static final int GL_SIGNALED = 0x9119;
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;
    public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x1;
    public static final int GL_VERTEX_ATTRIB_ARRAY_DIVISOR = 0x88FE;
    public static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;
    public static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
    public static final int GL_SAMPLER_BINDING = 0x8919;
    public static final int GL_RGB10_A2UI = 0x906F;
    public static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    public static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    public static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    public static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    public static final int GL_GREEN = 0x1904;
    public static final int GL_BLUE = 0x1905;
    public static final int GL_INT_2_10_10_10_REV = 0x8D9F;
    public static final int GL_TRANSFORM_FEEDBACK = 0x8E22;
    public static final int GL_TRANSFORM_FEEDBACK_PAUSED = 0x8E23;
    public static final int GL_TRANSFORM_FEEDBACK_ACTIVE = 0x8E24;
    public static final int GL_TRANSFORM_FEEDBACK_BINDING = 0x8E25;
    public static final int GL_PROGRAM_BINARY_RETRIEVABLE_HINT = 0x8257;
    public static final int GL_PROGRAM_BINARY_LENGTH = 0x8741;
    public static final int GL_NUM_PROGRAM_BINARY_FORMATS = 0x87FE;
    public static final int GL_PROGRAM_BINARY_FORMATS = 0x87FF;
    public static final int GL_COMPRESSED_R11_EAC = 0x9270;
    public static final int GL_COMPRESSED_SIGNED_R11_EAC = 0x9271;
    public static final int GL_COMPRESSED_RG11_EAC = 0x9272;
    public static final int GL_COMPRESSED_SIGNED_RG11_EAC = 0x9273;
    public static final int GL_COMPRESSED_RGB8_ETC2 = 0x9274;
    public static final int GL_COMPRESSED_SRGB8_ETC2 = 0x9275;
    public static final int GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9276;
    public static final int GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277;
    public static final int GL_COMPRESSED_RGBA8_ETC2_EAC = 0x9278;
    public static final int GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC = 0x9279;
    public static final int GL_TEXTURE_IMMUTABLE_FORMAT = 0x912F;
    public static final int GL_MAX_ELEMENT_INDEX = 0x8D6B;
    public static final int GL_NUM_SAMPLE_COUNTS = 0x9380;
    public static final int GL_TEXTURE_IMMUTABLE_LEVELS = 0x82DF;

    protected GLES30() {
    }

    public static void glReadBuffer(int src) {
        es().readBuffer(src);
    }

    public static void glDrawRangeElements(int mode, int start, int end, int count, int type,
                                           long indices) {
        if (Es.bound(GL_ELEMENT_ARRAY_BUFFER_BINDING)) {
            Es.drawing().drawRangeElements(mode, start, end, count, type, indices);
        } else {
            Es.drawElements(mode, type,
                    MemoryUtil.memByteBuffer(indices, count * Es.typeSize(type)), start, end, -1);
        }
    }

    public static void glDrawRangeElements(int mode, int start, int end, int type,
                                           ByteBuffer indices) {
        Es.drawElements(mode, type, indices, start, end, -1);
    }

    public static void glDrawRangeElements(int mode, int start, int end, ByteBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_BYTE, indices, start, end, -1);
    }

    public static void glDrawRangeElements(int mode, int start, int end, ShortBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_SHORT, indices, start, end, -1);
    }

    public static void glDrawRangeElements(int mode, int start, int end, IntBuffer indices) {
        Es.drawElements(mode, GL_UNSIGNED_INT, indices, start, end, -1);
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    ByteBuffer pixels) {
        es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                type, Es.bytes(pixels));
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    long pixels) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                    type, pixels);
        } else {
            es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                    type, pixels == 0 ? null : Es.bytes(MemoryUtil.memByteBuffer(pixels,
                            Es.pixelBytes(format, type, width, height, depth))));
        }
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    ShortBuffer pixels) {
        es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                type, Es.bytes(pixels));
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    IntBuffer pixels) {
        es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                type, Es.bytes(pixels));
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    FloatBuffer pixels) {
        es().texImage3D(target, level, internalformat, width, height, depth, border, format,
                type, Es.bytes(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                       int zoffset, int width, int height, int depth,
                                       int format, int type, ByteBuffer pixels) {
        es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                format, type, Es.bytes(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                       int zoffset, int width, int height, int depth,
                                       int format, int type, long pixels) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                    format, type, pixels);
        } else {
            es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                    format, type, Es.bytes(MemoryUtil.memByteBuffer(pixels,
                            Es.pixelBytes(format, type, width, height, depth))));
        }
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                       int zoffset, int width, int height, int depth,
                                       int format, int type, ShortBuffer pixels) {
        es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                format, type, Es.bytes(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                       int zoffset, int width, int height, int depth,
                                       int format, int type, IntBuffer pixels) {
        es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                format, type, Es.bytes(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                       int zoffset, int width, int height, int depth,
                                       int format, int type, FloatBuffer pixels) {
        es().texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth,
                format, type, Es.bytes(pixels));
    }

    public static void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset,
                                           int zoffset, int x, int y, int width, int height) {
        es().copyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    public static void glCompressedTexImage3D(int target, int level, int internalformat,
                                              int width, int height, int depth, int border,
                                              int imageSize, long data) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().compressedTexImage3D(target, level, internalformat, width, height, depth,
                    border, imageSize, data);
        } else {
            es().compressedTexImage3D(target, level, internalformat, width, height, depth,
                    border, Es.bytes(MemoryUtil.memByteBuffer(data, imageSize)));
        }
    }

    public static void glCompressedTexImage3D(int target, int level, int internalformat,
                                              int width, int height, int depth, int border,
                                              ByteBuffer data) {
        es().compressedTexImage3D(target, level, internalformat, width, height, depth, border,
                Es.bytes(data));
    }

    public static void glCompressedTexSubImage3D(int target, int level, int xoffset,
                                                 int yoffset, int zoffset, int width,
                                                 int height, int depth, int format,
                                                 int imageSize, long data) {
        if (Es.bound(Es.PIXEL_UNPACK_BUFFER_BINDING)) {
            es().compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width,
                    height, depth, format, imageSize, data);
        } else {
            es().compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width,
                    height, depth, format, Es.bytes(MemoryUtil.memByteBuffer(data, imageSize)));
        }
    }

    public static void glCompressedTexSubImage3D(int target, int level, int xoffset,
                                                 int yoffset, int zoffset, int width,
                                                 int height, int depth, int format,
                                                 ByteBuffer data) {
        es().compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height,
                depth, format, Es.bytes(data));
    }

    public static void glGenQueries(IntBuffer ids) {
        Es.names(ids, es()::createQuery);
    }

    public static int glGenQueries() {
        return es().createQuery();
    }

    public static void glDeleteQueries(IntBuffer ids) {
        Es.delete(ids, es()::deleteQuery);
    }

    public static void glDeleteQueries(int id) {
        es().deleteQuery(id);
    }

    public static boolean glIsQuery(int id) {
        return es().isQuery(id);
    }

    public static void glBeginQuery(int target, int id) {
        es().beginQuery(target, id);
    }

    public static void glEndQuery(int target) {
        es().endQuery(target);
    }

    public static void glGetQueryiv(int target, int pname, IntBuffer params) {
        params.put(params.position(), es().getQuery(target, pname));
    }

    public static int glGetQueryi(int target, int pname) {
        return es().getQuery(target, pname);
    }

    public static void glGetQueryObjectuiv(int id, int pname, IntBuffer params) {
        params.put(params.position(), glGetQueryObjectui(id, pname));
    }

    /** a result waits for the page to answer, as gl blocks until it is available */
    public static int glGetQueryObjectui(int id, int pname) {
        return (int) Es.queryResult(id, pname);
    }

    public static boolean glUnmapBuffer(int target) {
        return Es.unmap(target);
    }

    public static void glGetBufferPointerv(int target, int pname, PointerBuffer params) {
        params.put(params.position(), glGetBufferPointer(target, pname));
    }

    public static long glGetBufferPointer(int target, int pname) {
        ByteBuffer mapped = Es.mapped(target);
        return mapped == null ? 0 : MemoryUtil.memAddress(mapped);
    }

    public static void glDrawBuffers(IntBuffer bufs) {
        es().drawBuffers(Es.ints(bufs));
    }

    public static void glDrawBuffers(int buf) {
        es().drawBuffers(new int[] {buf});
    }

    public static void glUniformMatrix2x3fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 2, 3, transpose, Es.floats(value));
    }

    public static void glUniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 3, 2, transpose, Es.floats(value));
    }

    public static void glUniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 2, 4, transpose, Es.floats(value));
    }

    public static void glUniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 4, 2, transpose, Es.floats(value));
    }

    public static void glUniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 3, 4, transpose, Es.floats(value));
    }

    public static void glUniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {
        es().uniformMatrixfv(location, 4, 3, transpose, Es.floats(value));
    }

    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
                                         int dstY0, int dstX1, int dstY1, int mask,
                                         int filter) {
        es().blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask,
                filter);
    }

    public static void glRenderbufferStorageMultisample(int target, int samples,
                                                        int internalformat, int width,
                                                        int height) {
        es().renderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    public static void glFramebufferTextureLayer(int target, int attachment, int texture,
                                                 int level, int layer) {
        es().framebufferTextureLayer(target, attachment, texture, level, layer);
    }

    /** webgl cannot map a buffer, so this hands out a copy and writes it back at unmap */
    public static ByteBuffer glMapBufferRange(int target, long offset, long length, int access) {
        return Es.map(target, offset, length, access);
    }

    public static ByteBuffer glMapBufferRange(int target, long offset, long length, int access,
                                              ByteBuffer old_buffer) {
        return Es.map(target, offset, length, access);
    }

    public static void glFlushMappedBufferRange(int target, long offset, long length) {
        Es.flushMapped(target, offset, length);
    }

    public static void glBindVertexArray(int array) {
        es().bindVertexArray(array);
    }

    public static void glDeleteVertexArrays(IntBuffer arrays) {
        Es.delete(arrays, es()::deleteVertexArray);
    }

    public static void glDeleteVertexArrays(int array) {
        es().deleteVertexArray(array);
    }

    public static void glGenVertexArrays(IntBuffer arrays) {
        Es.names(arrays, es()::createVertexArray);
    }

    public static int glGenVertexArrays() {
        return es().createVertexArray();
    }

    public static boolean glIsVertexArray(int array) {
        return es().isVertexArray(array);
    }

    public static void glGetIntegeri_v(int target, int index, IntBuffer data) {
        data.put(data.position(), glGetIntegeri(target, index));
    }

    public static int glGetIntegeri(int target, int index) {
        return (int) es().getIndexedParameter(target, index);
    }

    public static void glBeginTransformFeedback(int primitiveMode) {
        es().beginTransformFeedback(primitiveMode);
    }

    public static void glEndTransformFeedback() {
        es().endTransformFeedback();
    }

    public static void glBindBufferRange(int target, int index, int buffer, long offset,
                                         long size) {
        es().bindBufferRange(target, index, buffer, offset, size);
    }

    public static void glBindBufferBase(int target, int index, int buffer) {
        es().bindBufferBase(target, index, buffer);
    }

    public static void glTransformFeedbackVaryings(int program, PointerBuffer varyings,
                                                   int bufferMode) {
        es().transformFeedbackVaryings(program, Es.texts(varyings), bufferMode);
    }

    public static void glTransformFeedbackVaryings(int program, CharSequence varying,
                                                   int bufferMode) {
        es().transformFeedbackVaryings(program, new String[] {varying.toString()}, bufferMode);
    }

    public static void glTransformFeedbackVaryings(int program, CharSequence[] varyings,
                                                   int bufferMode) {
        String[] names = new String[varyings.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = varyings[i].toString();
        }
        es().transformFeedbackVaryings(program, names, bufferMode);
    }

    public static void glGetTransformFeedbackVarying(int program, int index, IntBuffer length,
                                                     IntBuffer size, IntBuffer type,
                                                     ByteBuffer name) {
        describe(es().getTransformFeedbackVarying(program, index), length, size, type, name);
    }

    public static String glGetTransformFeedbackVarying(int program, int index, int bufSize,
                                                       IntBuffer size, IntBuffer type) {
        return Es.clip(describe(es().getTransformFeedbackVarying(program, index), size, type),
                bufSize);
    }

    public static String glGetTransformFeedbackVarying(int program, int index, IntBuffer size,
                                                       IntBuffer type) {
        return describe(es().getTransformFeedbackVarying(program, index), size, type);
    }

    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              ByteBuffer pointer) {
        Es.pointAt(index, pointer, size, type, false, stride, true);
    }

    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              long pointer) {
        Es.pointAt(index);
        es().vertexAttribIPointer(index, size, type, stride, pointer);
    }

    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              ShortBuffer pointer) {
        Es.pointAt(index, pointer, size, type, false, stride, true);
    }

    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              IntBuffer pointer) {
        Es.pointAt(index, pointer, size, type, false, stride, true);
    }

    public static void glGetVertexAttribIiv(int index, int pname, IntBuffer params) {
        Es.put(params, es().getVertexAttrib(index, pname));
    }

    public static int glGetVertexAttribIi(int index, int pname) {
        return (int) (long) es().getVertexAttrib(index, pname)[0];
    }

    public static void glGetVertexAttribIuiv(int index, int pname, IntBuffer params) {
        Es.put(params, es().getVertexAttrib(index, pname));
    }

    public static int glGetVertexAttribIui(int index, int pname) {
        return (int) (long) es().getVertexAttrib(index, pname)[0];
    }

    public static void glVertexAttribI4i(int index, int x, int y, int z, int w) {
        es().vertexAttribI4i(index, x, y, z, w);
    }

    public static void glVertexAttribI4ui(int index, int x, int y, int z, int w) {
        es().vertexAttribI4ui(index, x, y, z, w);
    }

    public static void glVertexAttribI4iv(int index, IntBuffer v) {
        int at = v.position();
        es().vertexAttribI4i(index, v.get(at), v.get(at + 1), v.get(at + 2), v.get(at + 3));
    }

    public static void glVertexAttribI4uiv(int index, IntBuffer v) {
        int at = v.position();
        es().vertexAttribI4ui(index, v.get(at), v.get(at + 1), v.get(at + 2), v.get(at + 3));
    }

    public static void glGetUniformuiv(int program, int location, IntBuffer params) {
        Es.put(params, es().getUniform(program, location));
    }

    public static int glGetUniformui(int program, int location) {
        return (int) (long) es().getUniform(program, location)[0];
    }

    public static int glGetFragDataLocation(int program, ByteBuffer name) {
        return es().getFragDataLocation(program, Es.text(name));
    }

    public static int glGetFragDataLocation(int program, CharSequence name) {
        return es().getFragDataLocation(program, name.toString());
    }

    public static void glUniform1ui(int location, int v0) {
        es().uniform1ui(location, v0);
    }

    public static void glUniform2ui(int location, int v0, int v1) {
        es().uniform2ui(location, v0, v1);
    }

    public static void glUniform3ui(int location, int v0, int v1, int v2) {
        es().uniform3ui(location, v0, v1, v2);
    }

    public static void glUniform4ui(int location, int v0, int v1, int v2, int v3) {
        es().uniform4ui(location, v0, v1, v2, v3);
    }

    public static void glUniform1uiv(int location, IntBuffer value) {
        es().uniformuiv(location, 1, Es.ints(value));
    }

    public static void glUniform2uiv(int location, IntBuffer value) {
        es().uniformuiv(location, 2, Es.ints(value));
    }

    public static void glUniform3uiv(int location, IntBuffer value) {
        es().uniformuiv(location, 3, Es.ints(value));
    }

    public static void glUniform4uiv(int location, IntBuffer value) {
        es().uniformuiv(location, 4, Es.ints(value));
    }

    public static void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        es().clearBufferiv(buffer, drawbuffer, Es.ints(value));
    }

    public static void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {
        es().clearBufferuiv(buffer, drawbuffer, Es.ints(value));
    }

    public static void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        es().clearBufferfv(buffer, drawbuffer, Es.floats(value));
    }

    public static void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        es().clearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    public static String glGetStringi(int name, int index) {
        String[] extensions = es().extensions();
        if (name != GL_EXTENSIONS || index < 0 || index >= extensions.length) {
            Es.fail(name != GL_EXTENSIONS ? GL_INVALID_ENUM : GL_INVALID_VALUE);
            return null;
        }
        return extensions[index];
    }

    public static void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset,
                                           long writeOffset, long size) {
        es().copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    public static void glGetUniformIndices(int program, PointerBuffer uniformNames,
                                           IntBuffer uniformIndices) {
        Es.put(uniformIndices, es().getUniformIndices(program, Es.texts(uniformNames)));
    }

    public static void glGetActiveUniformsiv(int program, IntBuffer uniformIndices, int pname,
                                             IntBuffer params) {
        Es.put(params, es().getActiveUniforms(program, Es.ints(uniformIndices), pname));
    }

    public static int glGetUniformBlockIndex(int program, ByteBuffer uniformBlockName) {
        return es().getUniformBlockIndex(program, Es.text(uniformBlockName));
    }

    public static int glGetUniformBlockIndex(int program, CharSequence uniformBlockName) {
        return es().getUniformBlockIndex(program, uniformBlockName.toString());
    }

    public static void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname,
                                                 IntBuffer params) {
        Es.put(params, es().getActiveUniformBlockParameter(program, uniformBlockIndex, pname));
    }

    public static int glGetActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        return es().getActiveUniformBlockParameter(program, uniformBlockIndex, pname)[0];
    }

    public static void glGetActiveUniformBlockName(int program, int uniformBlockIndex,
                                                   IntBuffer length,
                                                   ByteBuffer uniformBlockName) {
        Es.write(es().getActiveUniformBlockName(program, uniformBlockIndex), length,
                uniformBlockName);
    }

    public static String glGetActiveUniformBlockName(int program, int uniformBlockIndex,
                                                     int bufSize) {
        return Es.clip(es().getActiveUniformBlockName(program, uniformBlockIndex), bufSize);
    }

    public static String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return es().getActiveUniformBlockName(program, uniformBlockIndex);
    }

    public static void glUniformBlockBinding(int program, int uniformBlockIndex,
                                             int uniformBlockBinding) {
        es().uniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    public static void glDrawArraysInstanced(int mode, int first, int count,
                                             int instancecount) {
        Es.drawing().drawArraysInstanced(mode, first, count, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, int count, int type, long indices,
                                               int instancecount) {
        if (Es.bound(GL_ELEMENT_ARRAY_BUFFER_BINDING)) {
            Es.drawing().drawElementsInstanced(mode, count, type, indices, instancecount);
        } else {
            Es.drawElements(mode, type, MemoryUtil.memByteBuffer(indices,
                    count * Es.typeSize(type)), -1, -1, instancecount);
        }
    }

    public static void glDrawElementsInstanced(int mode, int type, ByteBuffer indices,
                                               int instancecount) {
        Es.drawElements(mode, type, indices, -1, -1, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, ByteBuffer indices, int instancecount) {
        Es.drawElements(mode, GL_UNSIGNED_BYTE, indices, -1, -1, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, ShortBuffer indices,
                                               int instancecount) {
        Es.drawElements(mode, GL_UNSIGNED_SHORT, indices, -1, -1, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, IntBuffer indices, int instancecount) {
        Es.drawElements(mode, GL_UNSIGNED_INT, indices, -1, -1, instancecount);
    }

    public static long glFenceSync(int condition, int flags) {
        return es().fenceSync(condition, flags);
    }

    public static boolean glIsSync(long sync) {
        return es().isSync((int) sync);
    }

    public static void glDeleteSync(long sync) {
        es().deleteSync((int) sync);
    }

    /** waits up to the timeout, giving the page its turn between checks */
    public static int glClientWaitSync(long sync, int flags, long timeout) {
        return Es.clientWaitSync((int) sync, flags, timeout);
    }

    public static void glWaitSync(long sync, int flags, long timeout) {
        es().waitSync((int) sync, flags, timeout);
    }

    public static void glGetInteger64v(int pname, LongBuffer data) {
        double[] values = es().getParameter(pname);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, (long) values[i]);
        }
    }

    public static long glGetInteger64(int pname) {
        return (long) es().getParameter(pname)[0];
    }

    public static void glGetSynciv(long sync, int pname, IntBuffer length, IntBuffer values) {
        values.put(values.position(), es().getSyncParameter((int) sync, pname));
        Es.putIfPresent(length, 1);
    }

    public static int glGetSynci(long sync, int pname, IntBuffer length) {
        Es.putIfPresent(length, 1);
        return es().getSyncParameter((int) sync, pname);
    }

    public static void glGetInteger64i_v(int target, int index, LongBuffer data) {
        Es.put(data, es().getIndexedParameter(target, index));
    }

    public static long glGetInteger64i(int target, int index) {
        return es().getIndexedParameter(target, index);
    }

    public static void glGetBufferParameteri64v(int target, int pname, LongBuffer params) {
        Es.put(params, bufferParameter(target, pname));
    }

    public static long glGetBufferParameteri64(int target, int pname) {
        return bufferParameter(target, pname);
    }

    public static void glGenSamplers(IntBuffer samplers) {
        Es.names(samplers, es()::createSampler);
    }

    public static int glGenSamplers() {
        return es().createSampler();
    }

    public static void glDeleteSamplers(IntBuffer samplers) {
        Es.delete(samplers, es()::deleteSampler);
    }

    public static void glDeleteSamplers(int sampler) {
        es().deleteSampler(sampler);
    }

    public static boolean glIsSampler(int sampler) {
        return es().isSampler(sampler);
    }

    public static void glBindSampler(int unit, int sampler) {
        es().bindSampler(unit, sampler);
    }

    public static void glSamplerParameteri(int sampler, int pname, int param) {
        es().samplerParameteri(sampler, pname, param);
    }

    public static void glSamplerParameteriv(int sampler, int pname, IntBuffer param) {
        es().samplerParameteri(sampler, pname, param.get(param.position()));
    }

    public static void glSamplerParameterf(int sampler, int pname, float param) {
        es().samplerParameterf(sampler, pname, param);
    }

    public static void glSamplerParameterfv(int sampler, int pname, FloatBuffer param) {
        es().samplerParameterf(sampler, pname, param.get(param.position()));
    }

    public static void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params) {
        params.put(params.position(), glGetSamplerParameteri(sampler, pname));
    }

    public static int glGetSamplerParameteri(int sampler, int pname) {
        return (int) es().getSamplerParameter(sampler, pname);
    }

    public static void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params) {
        params.put(params.position(), glGetSamplerParameterf(sampler, pname));
    }

    public static float glGetSamplerParameterf(int sampler, int pname) {
        return (float) es().getSamplerParameter(sampler, pname);
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        es().vertexAttribDivisor(index, divisor);
    }

    public static void glBindTransformFeedback(int target, int id) {
        es().bindTransformFeedback(target, id);
    }

    public static void glDeleteTransformFeedbacks(IntBuffer ids) {
        Es.delete(ids, es()::deleteTransformFeedback);
    }

    public static void glDeleteTransformFeedbacks(int id) {
        es().deleteTransformFeedback(id);
    }

    public static void glGenTransformFeedbacks(IntBuffer ids) {
        Es.names(ids, es()::createTransformFeedback);
    }

    public static int glGenTransformFeedbacks() {
        return es().createTransformFeedback();
    }

    public static boolean glIsTransformFeedback(int id) {
        return es().isTransformFeedback(id);
    }

    public static void glPauseTransformFeedback() {
        es().pauseTransformFeedback();
    }

    public static void glResumeTransformFeedback() {
        es().resumeTransformFeedback();
    }

    /** webgl keeps no program binaries, so a program has none to give */
    public static void glGetProgramBinary(int program, IntBuffer length, IntBuffer binaryFormat,
                                          ByteBuffer binary) {
        Es.fail(GL_INVALID_OPERATION);
        Es.putIfPresent(length, 0);
    }

    public static void glProgramBinary(int program, int binaryFormat, ByteBuffer binary) {
        Es.fail(GL_INVALID_ENUM);
    }

    public static void glProgramParameteri(int program, int pname, int value) {
        if (pname != GL_PROGRAM_BINARY_RETRIEVABLE_HINT) {
            Es.fail(GL_INVALID_ENUM);
        }
    }

    public static void glInvalidateFramebuffer(int target, IntBuffer attachments) {
        es().invalidateFramebuffer(target, Es.ints(attachments));
    }

    public static void glInvalidateFramebuffer(int target, int attachment) {
        es().invalidateFramebuffer(target, new int[] {attachment});
    }

    public static void glInvalidateSubFramebuffer(int target, IntBuffer attachments, int x,
                                                  int y, int width, int height) {
        es().invalidateSubFramebuffer(target, Es.ints(attachments), x, y, width, height);
    }

    public static void glInvalidateSubFramebuffer(int target, int attachment, int x, int y,
                                                  int width, int height) {
        es().invalidateSubFramebuffer(target, new int[] {attachment}, x, y, width, height);
    }

    public static void glTexStorage2D(int target, int levels, int internalformat, int width,
                                      int height) {
        es().texStorage2D(target, levels, internalformat, width, height);
    }

    public static void glTexStorage3D(int target, int levels, int internalformat, int width,
                                      int height, int depth) {
        es().texStorage3D(target, levels, internalformat, width, height, depth);
    }

    public static void glGetInternalformativ(int target, int internalformat, int pname,
                                             IntBuffer params) {
        Es.put(params, es().getInternalformatParameter(target, internalformat, pname));
    }

    public static int glGetInternalformati(int target, int internalformat, int pname) {
        int[] values = es().getInternalformatParameter(target, internalformat, pname);
        return values.length == 0 ? 0 : values[0];
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    short[] pixels) {
        glTexImage3D(target, level, internalformat, width, height, depth, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    int[] pixels) {
        glTexImage3D(target, level, internalformat, width, height, depth, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexImage3D(int target, int level, int internalformat, int width,
                                    int height, int depth, int border, int format, int type,
                                    float[] pixels) {
        glTexImage3D(target, level, internalformat, width, height, depth, border, format, type,
                Es.wrap(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                       int width, int height, int depth, int format, int type,
                                       short[] pixels) {
        glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format,
                type, Es.wrap(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                       int width, int height, int depth, int format, int type,
                                       int[] pixels) {
        glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format,
                type, Es.wrap(pixels));
    }

    public static void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                       int width, int height, int depth, int format, int type,
                                       float[] pixels) {
        glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format,
                type, Es.wrap(pixels));
    }

    public static void glGenQueries(int[] ids) {
        glGenQueries(Es.wrap(ids));
    }

    public static void glDeleteQueries(int[] ids) {
        glDeleteQueries(Es.wrap(ids));
    }

    public static void glGetQueryiv(int target, int pname, int[] params) {
        glGetQueryiv(target, pname, Es.wrap(params));
    }

    public static void glGetQueryObjectuiv(int id, int pname, int[] params) {
        glGetQueryObjectuiv(id, pname, Es.wrap(params));
    }

    public static void glDrawBuffers(int[] bufs) {
        glDrawBuffers(Es.wrap(bufs));
    }

    public static void glUniformMatrix2x3fv(int location, boolean transpose, float[] value) {
        glUniformMatrix2x3fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix3x2fv(int location, boolean transpose, float[] value) {
        glUniformMatrix3x2fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix2x4fv(int location, boolean transpose, float[] value) {
        glUniformMatrix2x4fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix4x2fv(int location, boolean transpose, float[] value) {
        glUniformMatrix4x2fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix3x4fv(int location, boolean transpose, float[] value) {
        glUniformMatrix3x4fv(location, transpose, Es.wrap(value));
    }

    public static void glUniformMatrix4x3fv(int location, boolean transpose, float[] value) {
        glUniformMatrix4x3fv(location, transpose, Es.wrap(value));
    }

    public static void glDeleteVertexArrays(int[] arrays) {
        glDeleteVertexArrays(Es.wrap(arrays));
    }

    public static void glGenVertexArrays(int[] arrays) {
        glGenVertexArrays(Es.wrap(arrays));
    }

    public static void glGetIntegeri_v(int target, int index, int[] data) {
        glGetIntegeri_v(target, index, Es.wrap(data));
    }

    public static void glGetTransformFeedbackVarying(int program, int index, int[] length,
                                                     int[] size, int[] type, ByteBuffer name) {
        glGetTransformFeedbackVarying(program, index, Es.wrap(length), Es.wrap(size), Es.wrap(type),
                name);
    }

    public static void glGetVertexAttribIiv(int index, int pname, int[] params) {
        glGetVertexAttribIiv(index, pname, Es.wrap(params));
    }

    public static void glGetVertexAttribIuiv(int index, int pname, int[] params) {
        glGetVertexAttribIuiv(index, pname, Es.wrap(params));
    }

    public static void glVertexAttribI4iv(int index, int[] v) {
        glVertexAttribI4iv(index, Es.wrap(v));
    }

    public static void glVertexAttribI4uiv(int index, int[] v) {
        glVertexAttribI4uiv(index, Es.wrap(v));
    }

    public static void glGetUniformuiv(int program, int location, int[] params) {
        glGetUniformuiv(program, location, Es.wrap(params));
    }

    public static void glUniform1uiv(int location, int[] value) {
        glUniform1uiv(location, Es.wrap(value));
    }

    public static void glUniform2uiv(int location, int[] value) {
        glUniform2uiv(location, Es.wrap(value));
    }

    public static void glUniform3uiv(int location, int[] value) {
        glUniform3uiv(location, Es.wrap(value));
    }

    public static void glUniform4uiv(int location, int[] value) {
        glUniform4uiv(location, Es.wrap(value));
    }

    public static void glClearBufferiv(int buffer, int drawbuffer, int[] value) {
        glClearBufferiv(buffer, drawbuffer, Es.wrap(value));
    }

    public static void glClearBufferuiv(int buffer, int drawbuffer, int[] value) {
        glClearBufferuiv(buffer, drawbuffer, Es.wrap(value));
    }

    public static void glClearBufferfv(int buffer, int drawbuffer, float[] value) {
        glClearBufferfv(buffer, drawbuffer, Es.wrap(value));
    }

    public static void glGetUniformIndices(int program, PointerBuffer uniformNames,
                                           int[] uniformIndices) {
        glGetUniformIndices(program, uniformNames, Es.wrap(uniformIndices));
    }

    public static void glGetActiveUniformsiv(int program, int[] uniformIndices, int pname,
                                             int[] params) {
        glGetActiveUniformsiv(program, Es.wrap(uniformIndices), pname, Es.wrap(params));
    }

    public static void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname,
                                                 int[] params) {
        glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, Es.wrap(params));
    }

    public static void glGetActiveUniformBlockName(int program, int uniformBlockIndex, int[] length,
                                                   ByteBuffer uniformBlockName) {
        glGetActiveUniformBlockName(program, uniformBlockIndex, Es.wrap(length), uniformBlockName);
    }

    public static void glGetInteger64v(int pname, long[] data) {
        glGetInteger64v(pname, Es.wrap(data));
    }

    public static void glGetSynciv(long sync, int pname, int[] length, int[] values) {
        glGetSynciv(sync, pname, Es.wrap(length), Es.wrap(values));
    }

    public static void glGetInteger64i_v(int target, int index, long[] data) {
        glGetInteger64i_v(target, index, Es.wrap(data));
    }

    public static void glGetBufferParameteri64v(int target, int pname, long[] params) {
        glGetBufferParameteri64v(target, pname, Es.wrap(params));
    }

    public static void glGenSamplers(int[] samplers) {
        glGenSamplers(Es.wrap(samplers));
    }

    public static void glDeleteSamplers(int[] samplers) {
        glDeleteSamplers(Es.wrap(samplers));
    }

    public static void glSamplerParameteriv(int sampler, int pname, int[] param) {
        glSamplerParameteriv(sampler, pname, Es.wrap(param));
    }

    public static void glSamplerParameterfv(int sampler, int pname, float[] param) {
        glSamplerParameterfv(sampler, pname, Es.wrap(param));
    }

    public static void glGetSamplerParameteriv(int sampler, int pname, int[] params) {
        glGetSamplerParameteriv(sampler, pname, Es.wrap(params));
    }

    public static void glGetSamplerParameterfv(int sampler, int pname, float[] params) {
        glGetSamplerParameterfv(sampler, pname, Es.wrap(params));
    }

    public static void glDeleteTransformFeedbacks(int[] ids) {
        glDeleteTransformFeedbacks(Es.wrap(ids));
    }

    public static void glGenTransformFeedbacks(int[] ids) {
        glGenTransformFeedbacks(Es.wrap(ids));
    }

    public static void glGetProgramBinary(int program, int[] length, int[] binaryFormat,
                                          ByteBuffer binary) {
        glGetProgramBinary(program, Es.wrap(length), Es.wrap(binaryFormat), binary);
    }

    public static void glInvalidateFramebuffer(int target, int[] attachments) {
        glInvalidateFramebuffer(target, Es.wrap(attachments));
    }

    public static void glInvalidateSubFramebuffer(int target, int[] attachments, int x, int y,
                                                  int width, int height) {
        glInvalidateSubFramebuffer(target, Es.wrap(attachments), x, y, width, height);
    }

    public static void glGetInternalformativ(int target, int internalformat, int pname,
                                             int[] params) {
        glGetInternalformativ(target, internalformat, pname, Es.wrap(params));
    }
}
