package dev.flagon.lwjgl.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImmediateLayoutTest {

    @Test
    void everyMaskPacksItsAttributesInOrder() {
        for (int mask = 0; mask < ImmediateLayout.MASKS; mask++) {
            int at = 3;
            if (ImmediateLayout.has(mask, ImmediateLayout.COLOR)) {
                assertEquals(at, ImmediateLayout.colorOffset(mask), "colour of " + mask);
                at += 4;
            }
            for (int unit = 0; unit < VertexArrays.UNITS; unit++) {
                if (ImmediateLayout.has(mask, ImmediateLayout.texCoord(unit))) {
                    assertEquals(at, ImmediateLayout.texCoordOffset(mask, unit),
                            "unit " + unit + " of " + mask);
                    at += 2;
                }
            }
            if (ImmediateLayout.has(mask, ImmediateLayout.NORMAL)) {
                assertEquals(at, ImmediateLayout.normalOffset(mask), "normal of " + mask);
                at += 3;
            }
            assertEquals(at, ImmediateLayout.floats(mask), "floats of " + mask);
        }
    }
}
