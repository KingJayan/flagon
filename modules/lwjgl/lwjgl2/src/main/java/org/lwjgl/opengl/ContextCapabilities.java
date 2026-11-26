package org.lwjgl.opengl;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ContextCapabilities {

    public final boolean OpenGL11;
    public final boolean OpenGL12;
    public final boolean OpenGL13;
    public final boolean OpenGL14;
    public final boolean OpenGL15;
    public final boolean OpenGL20;
    public final boolean OpenGL21;
    public final boolean OpenGL30;
    public final boolean OpenGL31;
    public final boolean OpenGL32;
    public final boolean OpenGL33;

    public final boolean GL_ARB_multitexture;
    public final boolean GL_ARB_vertex_buffer_object;
    public final boolean GL_ARB_shader_objects;
    public final boolean GL_ARB_vertex_shader;
    public final boolean GL_ARB_fragment_shader;
    public final boolean GL_ARB_texture_non_power_of_two;
    public final boolean GL_EXT_blend_func_separate;
    public final boolean GL_EXT_blend_subtract;
    public final boolean GL_EXT_blend_color;
    public final boolean GL_EXT_framebuffer_object;
    public final boolean GL_ARB_framebuffer_object;
    public final boolean GL_ARB_texture_env_combine;
    public final boolean GL_ARB_arrays_of_arrays;
    public final boolean GL_NV_fog_distance;

    public final boolean GL_EXT_texture_filter_anisotropic;
    public final boolean GL_EXT_texture_compression_s3tc;
    public final boolean GL_ARB_texture_float;
    public final boolean GL_ARB_occlusion_query;

    public final boolean GL_ARB_base_instance;
    public final boolean GL_ARB_blend_func_extended;
    public final boolean GL_ARB_clear_buffer_object;
    public final boolean GL_ARB_color_buffer_float;
    public final boolean GL_ARB_compatibility;
    public final boolean GL_ARB_compressed_texture_pixel_storage;
    public final boolean GL_ARB_compute_shader;
    public final boolean GL_ARB_copy_buffer;
    public final boolean GL_ARB_copy_image;
    public final boolean GL_ARB_depth_buffer_float;
    public final boolean GL_ARB_depth_clamp;
    public final boolean GL_ARB_depth_texture;
    public final boolean GL_ARB_draw_buffers;
    public final boolean GL_ARB_draw_buffers_blend;
    public final boolean GL_ARB_draw_elements_base_vertex;
    public final boolean GL_ARB_draw_indirect;
    public final boolean GL_ARB_draw_instanced;
    public final boolean GL_ARB_explicit_attrib_location;
    public final boolean GL_ARB_explicit_uniform_location;
    public final boolean GL_ARB_fragment_layer_viewport;
    public final boolean GL_ARB_fragment_program;
    public final boolean GL_ARB_fragment_program_shadow;
    public final boolean GL_ARB_framebuffer_sRGB;
    public final boolean GL_ARB_geometry_shader4;
    public final boolean GL_ARB_gpu_shader5;
    public final boolean GL_ARB_half_float_pixel;
    public final boolean GL_ARB_half_float_vertex;
    public final boolean GL_ARB_instanced_arrays;
    public final boolean GL_ARB_map_buffer_alignment;
    public final boolean GL_ARB_map_buffer_range;
    public final boolean GL_ARB_multisample;
    public final boolean GL_ARB_occlusion_query2;
    public final boolean GL_ARB_pixel_buffer_object;
    public final boolean GL_ARB_seamless_cube_map;
    public final boolean GL_ARB_shader_stencil_export;
    public final boolean GL_ARB_shader_texture_lod;
    public final boolean GL_ARB_shadow;
    public final boolean GL_ARB_shadow_ambient;
    public final boolean GL_ARB_stencil_texturing;
    public final boolean GL_ARB_sync;
    public final boolean GL_ARB_tessellation_shader;
    public final boolean GL_ARB_texture_border_clamp;
    public final boolean GL_ARB_texture_buffer_object;
    public final boolean GL_ARB_texture_cube_map;
    public final boolean GL_ARB_texture_cube_map_array;
    public final boolean GL_ARB_uniform_buffer_object;
    public final boolean GL_ARB_vertex_blend;
    public final boolean GL_ARB_vertex_program;
    public final boolean GL_EXT_bindable_uniform;
    public final boolean GL_EXT_blend_equation_separate;
    public final boolean GL_EXT_blend_minmax;
    public final boolean GL_EXT_draw_instanced;
    public final boolean GL_EXT_framebuffer_multisample;
    public final boolean GL_EXT_framebuffer_sRGB;
    public final boolean GL_EXT_geometry_shader4;
    public final boolean GL_EXT_gpu_program_parameters;
    public final boolean GL_EXT_gpu_shader4;
    public final boolean GL_EXT_multi_draw_arrays;
    public final boolean GL_EXT_packed_depth_stencil;
    public final boolean GL_EXT_paletted_texture;
    public final boolean GL_EXT_rescale_normal;
    public final boolean GL_EXT_separate_shader_objects;
    public final boolean GL_EXT_shader_image_load_store;
    public final boolean GL_EXT_shadow_funcs;
    public final boolean GL_EXT_shared_texture_palette;
    public final boolean GL_EXT_stencil_clear_tag;
    public final boolean GL_EXT_stencil_two_side;
    public final boolean GL_EXT_stencil_wrap;
    public final boolean GL_EXT_texture_3d;
    public final boolean GL_EXT_texture_array;
    public final boolean GL_EXT_texture_buffer_object;
    public final boolean GL_EXT_texture_integer;
    public final boolean GL_EXT_texture_lod_bias;
    public final boolean GL_EXT_texture_sRGB;
    public final boolean GL_EXT_vertex_shader;
    public final boolean GL_EXT_vertex_weighting;

    private final Set<String> extensions;
    private final String reported;

    ContextCapabilities(String extensionString) {
        this.reported = extensionString == null ? "" : extensionString;
        this.extensions = split(reported);

        OpenGL11 = true;
        OpenGL12 = true;
        OpenGL13 = true;
        OpenGL14 = true;
        OpenGL15 = true;
        OpenGL20 = true;
        OpenGL21 = false;
        OpenGL30 = false;
        OpenGL31 = false;
        OpenGL32 = false;
        OpenGL33 = false;

        // webgl2 carries these in core, so the entry points here are always backed
        GL_ARB_multitexture = true;
        GL_ARB_vertex_buffer_object = true;
        GL_ARB_shader_objects = true;
        GL_ARB_vertex_shader = true;
        GL_ARB_fragment_shader = true;
        GL_ARB_texture_non_power_of_two = true;
        GL_EXT_blend_func_separate = true;
        GL_EXT_blend_subtract = true;
        GL_EXT_blend_color = true;
        GL_EXT_framebuffer_object = true;
        GL_ARB_framebuffer_object = true;

        // the compatibility shader combines with GL_TEXTURE_ENV_MODE, and fog is a linear distance
        GL_ARB_texture_env_combine = false;
        GL_ARB_arrays_of_arrays = false;
        GL_NV_fog_distance = false;

        GL_EXT_texture_filter_anisotropic = hasAny("EXT_texture_filter_anisotropic");
        GL_EXT_texture_compression_s3tc =
                hasAny("WEBGL_compressed_texture_s3tc", "EXT_texture_compression_s3tc");
        GL_ARB_texture_float = hasAny("EXT_color_buffer_float", "OES_texture_float");
        GL_ARB_occlusion_query = false;

        // the rest are answered from the host's own list, which is the only honest answer
        GL_ARB_base_instance = hasAny("GL_ARB_base_instance");
        GL_ARB_blend_func_extended = hasAny("GL_ARB_blend_func_extended");
        GL_ARB_clear_buffer_object = hasAny("GL_ARB_clear_buffer_object");
        GL_ARB_color_buffer_float = hasAny("GL_ARB_color_buffer_float");
        GL_ARB_compatibility = hasAny("GL_ARB_compatibility");
        GL_ARB_compressed_texture_pixel_storage = hasAny("GL_ARB_compressed_texture_pixel_storage");
        GL_ARB_compute_shader = hasAny("GL_ARB_compute_shader");
        GL_ARB_copy_buffer = hasAny("GL_ARB_copy_buffer");
        GL_ARB_copy_image = hasAny("GL_ARB_copy_image");
        GL_ARB_depth_buffer_float = hasAny("GL_ARB_depth_buffer_float");
        GL_ARB_depth_clamp = hasAny("GL_ARB_depth_clamp");
        GL_ARB_depth_texture = hasAny("GL_ARB_depth_texture");
        GL_ARB_draw_buffers = hasAny("GL_ARB_draw_buffers");
        GL_ARB_draw_buffers_blend = hasAny("GL_ARB_draw_buffers_blend");
        GL_ARB_draw_elements_base_vertex = hasAny("GL_ARB_draw_elements_base_vertex");
        GL_ARB_draw_indirect = hasAny("GL_ARB_draw_indirect");
        GL_ARB_draw_instanced = hasAny("GL_ARB_draw_instanced");
        GL_ARB_explicit_attrib_location = hasAny("GL_ARB_explicit_attrib_location");
        GL_ARB_explicit_uniform_location = hasAny("GL_ARB_explicit_uniform_location");
        GL_ARB_fragment_layer_viewport = hasAny("GL_ARB_fragment_layer_viewport");
        GL_ARB_fragment_program = hasAny("GL_ARB_fragment_program");
        GL_ARB_fragment_program_shadow = hasAny("GL_ARB_fragment_program_shadow");
        GL_ARB_framebuffer_sRGB = hasAny("GL_ARB_framebuffer_sRGB");
        GL_ARB_geometry_shader4 = hasAny("GL_ARB_geometry_shader4");
        GL_ARB_gpu_shader5 = hasAny("GL_ARB_gpu_shader5");
        GL_ARB_half_float_pixel = hasAny("GL_ARB_half_float_pixel");
        GL_ARB_half_float_vertex = hasAny("GL_ARB_half_float_vertex");
        GL_ARB_instanced_arrays = hasAny("GL_ARB_instanced_arrays");
        GL_ARB_map_buffer_alignment = hasAny("GL_ARB_map_buffer_alignment");
        GL_ARB_map_buffer_range = hasAny("GL_ARB_map_buffer_range");
        GL_ARB_multisample = hasAny("GL_ARB_multisample");
        GL_ARB_occlusion_query2 = hasAny("GL_ARB_occlusion_query2");
        GL_ARB_pixel_buffer_object = hasAny("GL_ARB_pixel_buffer_object");
        GL_ARB_seamless_cube_map = hasAny("GL_ARB_seamless_cube_map");
        GL_ARB_shader_stencil_export = hasAny("GL_ARB_shader_stencil_export");
        GL_ARB_shader_texture_lod = hasAny("GL_ARB_shader_texture_lod");
        GL_ARB_shadow = hasAny("GL_ARB_shadow");
        GL_ARB_shadow_ambient = hasAny("GL_ARB_shadow_ambient");
        GL_ARB_stencil_texturing = hasAny("GL_ARB_stencil_texturing");
        GL_ARB_sync = hasAny("GL_ARB_sync");
        GL_ARB_tessellation_shader = hasAny("GL_ARB_tessellation_shader");
        GL_ARB_texture_border_clamp = hasAny("GL_ARB_texture_border_clamp");
        GL_ARB_texture_buffer_object = hasAny("GL_ARB_texture_buffer_object");
        GL_ARB_texture_cube_map = hasAny("GL_ARB_texture_cube_map");
        GL_ARB_texture_cube_map_array = hasAny("GL_ARB_texture_cube_map_array");
        GL_ARB_uniform_buffer_object = hasAny("GL_ARB_uniform_buffer_object");
        GL_ARB_vertex_blend = hasAny("GL_ARB_vertex_blend");
        GL_ARB_vertex_program = hasAny("GL_ARB_vertex_program");
        GL_EXT_bindable_uniform = hasAny("GL_EXT_bindable_uniform");
        GL_EXT_blend_equation_separate = hasAny("GL_EXT_blend_equation_separate");
        GL_EXT_blend_minmax = hasAny("GL_EXT_blend_minmax");
        GL_EXT_draw_instanced = hasAny("GL_EXT_draw_instanced");
        GL_EXT_framebuffer_multisample = hasAny("GL_EXT_framebuffer_multisample");
        GL_EXT_framebuffer_sRGB = hasAny("GL_EXT_framebuffer_sRGB");
        GL_EXT_geometry_shader4 = hasAny("GL_EXT_geometry_shader4");
        GL_EXT_gpu_program_parameters = hasAny("GL_EXT_gpu_program_parameters");
        GL_EXT_gpu_shader4 = hasAny("GL_EXT_gpu_shader4");
        GL_EXT_multi_draw_arrays = hasAny("GL_EXT_multi_draw_arrays");
        GL_EXT_packed_depth_stencil = hasAny("GL_EXT_packed_depth_stencil");
        GL_EXT_paletted_texture = hasAny("GL_EXT_paletted_texture");
        GL_EXT_rescale_normal = hasAny("GL_EXT_rescale_normal");
        GL_EXT_separate_shader_objects = hasAny("GL_EXT_separate_shader_objects");
        GL_EXT_shader_image_load_store = hasAny("GL_EXT_shader_image_load_store");
        GL_EXT_shadow_funcs = hasAny("GL_EXT_shadow_funcs");
        GL_EXT_shared_texture_palette = hasAny("GL_EXT_shared_texture_palette");
        GL_EXT_stencil_clear_tag = hasAny("GL_EXT_stencil_clear_tag");
        GL_EXT_stencil_two_side = hasAny("GL_EXT_stencil_two_side");
        GL_EXT_stencil_wrap = hasAny("GL_EXT_stencil_wrap");
        GL_EXT_texture_3d = hasAny("GL_EXT_texture_3d");
        GL_EXT_texture_array = hasAny("GL_EXT_texture_array");
        GL_EXT_texture_buffer_object = hasAny("GL_EXT_texture_buffer_object");
        GL_EXT_texture_integer = hasAny("GL_EXT_texture_integer");
        GL_EXT_texture_lod_bias = hasAny("GL_EXT_texture_lod_bias");
        GL_EXT_texture_sRGB = hasAny("GL_EXT_texture_sRGB");
        GL_EXT_vertex_shader = hasAny("GL_EXT_vertex_shader");
        GL_EXT_vertex_weighting = hasAny("GL_EXT_vertex_weighting");
    }

    /** @return whether the host listed the extension, with or without a {@code GL_} prefix */
    public boolean isSupported(String extension) {
        return hasAny(extension);
    }

    /**
     * @return the host's extension list, the same string {@code glGetString(GL_EXTENSIONS)} gives
     */
    public String getExtensions() {
        return reported;
    }

    private boolean hasAny(String... names) {
        for (String name : names) {
            if (extensions.contains(canonical(name))) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> split(String reported) {
        Set<String> names = new HashSet<>();
        int at = 0;
        while (at < reported.length()) {
            while (at < reported.length() && reported.charAt(at) <= ' ') {
                at++;
            }
            int start = at;
            while (at < reported.length() && reported.charAt(at) > ' ') {
                at++;
            }
            if (at > start) {
                names.add(canonical(reported.substring(start, at)));
            }
        }
        return names;
    }

    /** one spelling for GL_EXT_foo, EXT_foo and EXT_FOO alike */
    private static String canonical(String name) {
        String bare = name.startsWith("GL_") ? name.substring(3) : name;
        return bare.toUpperCase(Locale.ROOT);
    }
}
