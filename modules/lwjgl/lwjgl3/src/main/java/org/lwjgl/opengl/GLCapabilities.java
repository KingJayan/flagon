package org.lwjgl.opengl;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** what a context reports it can do, as glfw callers read it after glfwMakeContextCurrent. */
public final class GLCapabilities {

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
    public final boolean GL_ARB_instanced_arrays;
    public final boolean GL_ARB_vertex_array_object;

    private final Set<String> extensions;

    GLCapabilities(Set<String> extensions) {
        this.extensions = Collections.unmodifiableSet(new TreeSet<>(extensions));
        this.OpenGL11 = true;
        this.OpenGL12 = true;
        this.OpenGL13 = true;
        this.OpenGL14 = true;
        this.OpenGL15 = true;
        this.OpenGL20 = true;
        this.OpenGL21 = true;
        this.OpenGL30 = true;
        this.OpenGL31 = true;
        this.OpenGL32 = true;
        // 3.3 arrives whole here: the divisor is the only part of it the layer below needs
        this.OpenGL33 = true;
        this.GL_ARB_instanced_arrays = true;
        this.GL_ARB_vertex_array_object = true;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public boolean hasExtension(String name) {
        return extensions.contains(name);
    }
}
