package dev.flagon.lwjgl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * the ARB and EXT classes lwjgl 2 code reaches for when it checks a capability first.
 */
class LegacyClassesTest {

    private static final String[] CLASSES = {
            "org.lwjgl.opengl.ARBFramebufferObject",
            "org.lwjgl.opengl.ARBMultitexture",
            "org.lwjgl.opengl.ARBShaderObjects",
            "org.lwjgl.opengl.ARBVertexBufferObject",
            "org.lwjgl.opengl.ARBVertexShader",
            "org.lwjgl.opengl.EXTBlendFuncSeparate",
            "org.lwjgl.opengl.EXTFramebufferObject",
            "org.lwjgl.util.glu.Project",
            "org.lwjgl.util.vector.Matrix4f",
            "org.lwjgl.util.vector.Vector3f",
            "org.lwjgl.util.vector.Vector4f",
    };

    @Test
    void everyLegacyNameIsOnTheClasspath() {
        for (String name : CLASSES) {
            assertDoesNotThrow(() -> Class.forName(name), name + " is not there");
        }
    }
}
