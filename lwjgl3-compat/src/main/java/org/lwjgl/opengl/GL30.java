package org.lwjgl.opengl;

import dev.flagon.lwjgl.internal.GLEnums;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsInfo;

import java.nio.IntBuffer;

/** vertex array objects, the indexed string queries and the integer attribute pointers. */
public final class GL30 {

    public static final int GL_MAJOR_VERSION = 0x821B;
    public static final int GL_MINOR_VERSION = 0x821C;
    public static final int GL_NUM_EXTENSIONS = 0x821D;
    public static final int GL_CONTEXT_FLAGS = 0x821E;
    public static final int GL_CONTEXT_CORE_PROFILE_BIT = 0x00000001;
    public static final int GL_CONTEXT_COMPATIBILITY_PROFILE_BIT = 0x00000002;

    public static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    public static final int GL_EXTENSIONS = 0x1F03;

    public static final int GL_RGBA32F = 0x8814;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_R8 = 0x8229;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_HALF_FLOAT = 0x140B;

    private GL30() {
    }

    public static int glGenVertexArrays() {
        return GLState.get().createVertexArray();
    }

    public static void glGenVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            arrays.put(i, glGenVertexArrays());
        }
    }

    public static void glBindVertexArray(int array) {
        GLState.get().bindVertexArray(array);
    }

    public static void glDeleteVertexArrays(int array) {
        GLState.get().deleteVertexArray(array);
    }

    public static void glDeleteVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            glDeleteVertexArrays(arrays.get(i));
        }
    }

    public static boolean glIsVertexArray(int array) {
        return array != 0 && GLState.get().boundVertexArray() == array;
    }

    /** the core profile replaced the one space-separated GL_EXTENSIONS string with this. */
    public static String glGetStringi(int name, int index) {
        if (name != GL_EXTENSIONS) {
            throw new UnsupportedOperationException(
                    "glGetStringi emulates GL_EXTENSIONS only, got 0x" + Integer.toHexString(name));
        }
        String[] extensions = extensions();
        if (index < 0 || index >= extensions.length) {
            throw new IndexOutOfBoundsException(
                    "extension " + index + " of " + extensions.length);
        }
        return extensions[index];
    }

    public static int glGetInteger(int name) {
        return switch (name) {
            case GL_MAJOR_VERSION -> 3;
            case GL_MINOR_VERSION -> 2;
            case GL_NUM_EXTENSIONS -> extensions().length;
            case GL_CONTEXT_FLAGS -> 0;
            case GL_VERTEX_ARRAY_BINDING -> GLState.get().boundVertexArray();
            default -> throw new UnsupportedOperationException(
                    "GL30.glGetInteger does not emulate 0x" + Integer.toHexString(name));
        };
    }

    public static int glGetIntegeri(int name, int index) {
        return glGetInteger(name);
    }

    /**
     * webgl2 writes fragment outputs by their declared location, and the page cannot rename one
     * after linking. location 0 is the colour buffer every draw here targets, so binding it is
     * what the caller asked for and anything else would silently write nowhere.
     */
    public static void glBindFragDataLocation(int program, int colorNumber, CharSequence name) {
        if (colorNumber != 0) {
            throw new UnsupportedOperationException(
                    "glBindFragDataLocation can only bind colour 0; webgl2 draws to one colour "
                            + "buffer here, and the shader declares its own location");
        }
    }

    public static void glVertexAttribIPointer(int index, int size, int type, int stride,
                                              long offset) {
        if (offset < 0 || offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("glVertexAttribIPointer: offset out of range: " + offset);
        }
        GLState state = GLState.get();
        int buffer = state.boundBuffer(BufferTarget.ARRAY);
        if (buffer == GraphicsDevice.NO_BUFFER) {
            throw new UnsupportedOperationException(
                    "glVertexAttribIPointer was given an offset with no GL_ARRAY_BUFFER bound; the "
                            + "core profile has no client-side arrays");
        }
        AttributeType component = GLEnums.integerAttributeType(type, "glVertexAttribIPointer");
        state.attributes().slot(index)
                .pointAtIntegers(buffer, (int) offset, size, component, stride);
    }

    private static String[] extensions() {
        String reported = GLState.get().device().information(GraphicsInfo.EXTENSIONS);
        if (reported == null || reported.isBlank()) {
            return new String[0];
        }
        return reported.trim().split("\\s+");
    }
}
