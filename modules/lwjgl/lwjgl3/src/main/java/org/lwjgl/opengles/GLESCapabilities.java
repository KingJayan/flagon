package org.lwjgl.opengles;

import java.util.Set;

/** what the context reports: gl es 3.0 and the extensions webgl2 enables. */
public final class GLESCapabilities {

    public final boolean GLES20;
    public final boolean GLES30;
    public final boolean GLES31;
    public final boolean GLES32;
    public final boolean GL_EXT_color_buffer_float;
    public final boolean GL_EXT_color_buffer_half_float;
    public final boolean GL_EXT_float_blend;
    public final boolean GL_EXT_texture_compression_bptc;
    public final boolean GL_EXT_texture_compression_rgtc;
    public final boolean GL_EXT_texture_compression_s3tc;
    public final boolean GL_EXT_texture_filter_anisotropic;
    public final boolean GL_KHR_texture_compression_astc_ldr;
    public final boolean GL_OES_texture_float_linear;

    GLESCapabilities(Set<String> extensions) {
        GLES20 = true;
        GLES30 = true;
        GLES31 = false;
        GLES32 = false;
        GL_EXT_color_buffer_float = extensions.contains("EXT_color_buffer_float");
        GL_EXT_color_buffer_half_float = extensions.contains("EXT_color_buffer_half_float");
        GL_EXT_float_blend = extensions.contains("EXT_float_blend");
        GL_EXT_texture_compression_bptc = extensions.contains("EXT_texture_compression_bptc");
        GL_EXT_texture_compression_rgtc = extensions.contains("EXT_texture_compression_rgtc");
        GL_EXT_texture_compression_s3tc = extensions.contains("WEBGL_compressed_texture_s3tc");
        GL_EXT_texture_filter_anisotropic =
                extensions.contains("EXT_texture_filter_anisotropic");
        GL_KHR_texture_compression_astc_ldr =
                extensions.contains("WEBGL_compressed_texture_astc");
        GL_OES_texture_float_linear = extensions.contains("OES_texture_float_linear");
    }
}
