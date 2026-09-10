package org.lwjgl.opengl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * what this context can do, in the shape lwjgl applications read it.
 *
 * <p>the version flags say which of the {@code GL##} classes here are backed by real entry points;
 * the extension flags are answered from the host's own extension list, so an application that asks
 * before it uses something gets the truth rather than a promise.
 */
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

    public final boolean GL_EXT_texture_filter_anisotropic;
    public final boolean GL_EXT_texture_compression_s3tc;
    public final boolean GL_ARB_texture_float;
    public final boolean GL_ARB_occlusion_query;

    private final Set<String> extensions;
    private final String reported;

    ContextCapabilities(String extensionString) {
        this.reported = extensionString == null ? "" : extensionString;
        this.extensions = new HashSet<>(Arrays.asList(reported.split("\\s+")));
        this.extensions.remove("");

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

        GL_EXT_texture_filter_anisotropic = hasAny("EXT_texture_filter_anisotropic");
        GL_EXT_texture_compression_s3tc =
                hasAny("WEBGL_compressed_texture_s3tc", "EXT_texture_compression_s3tc");
        GL_ARB_texture_float = hasAny("EXT_color_buffer_float", "OES_texture_float");
        GL_ARB_occlusion_query = false;
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
            String bare = name.startsWith("GL_") ? name.substring(3) : name;
            if (extensions.contains(name) || extensions.contains(bare)
                    || extensions.contains("GL_" + bare)
                    || extensions.contains(bare.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
