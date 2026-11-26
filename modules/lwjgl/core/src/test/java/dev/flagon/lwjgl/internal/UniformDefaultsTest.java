package dev.flagon.lwjgl.internal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniformDefaultsTest {

    private static final String SOURCE = """
            uniform highp float Resolution = 4.0;
            uniform vec3 Gray = vec3(1.0);
            uniform ivec2 Steps = ivec2(2, 3);
            uniform bool On = true;
            uniform vec2 Size = OutSize * 2.0;
            uniform sampler2D DiffuseSampler;
            """;

    @Test
    void readsEachConstantStartingValue() {
        List<UniformDefaults.Default> found = UniformDefaults.of(SOURCE);

        assertEquals(List.of("Resolution", "Gray", "Steps", "On"),
                found.stream().map(UniformDefaults.Default::name).toList());
        assertArrayEquals(new float[] {4.0f}, found.get(0).values());
        assertArrayEquals(new float[] {1.0f, 1.0f, 1.0f}, found.get(1).values());
        assertTrue(found.get(2).integer());
        assertArrayEquals(new float[] {2.0f, 3.0f}, found.get(2).values());
        assertArrayEquals(new float[] {1.0f}, found.get(3).values());
    }

    @Test
    void dropsOnlyTheInitializersItCanSet() {
        String stripped = UniformDefaults.strip(SOURCE, SOURCE);

        assertTrue(stripped.contains("uniform highp float Resolution;\n"), stripped);
        assertTrue(stripped.contains("uniform ivec2 Steps;\n"), stripped);
        assertTrue(stripped.contains("uniform vec2 Size = OutSize * 2.0;"), stripped);
        assertFalse(stripped.contains("vec3(1.0)"), stripped);
    }
}
