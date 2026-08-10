package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.FixedFunctionPipeline;
import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightingTest {

    private RecordingGraphicsDevice device;

    @BeforeEach
    void setUp() {
        device = new RecordingGraphicsDevice();
        GLState.attach(device);
        device.calls.clear();
    }

    @AfterEach
    void tearDown() {
        GLState.detach();
    }

    private static FloatBuffer floats(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    private void drawATriangle() {
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0.0f, 0.0f, 0.0f);
        GL11.glVertex3f(1.0f, 0.0f, 0.0f);
        GL11.glVertex3f(0.0f, 1.0f, 0.0f);
        GL11.glEnd();
    }

    @Test
    void everyVertexCarriesTheNormalItWasGiven() {
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(1.0f, 2.0f, 3.0f);
        GL11.glEnd();

        int stride = FixedFunctionPipeline.VERTEX_FLOATS;
        assertArrayEquals(new float[] {0.0f, 1.0f, 0.0f},
                new float[] {device.lastBufferData[stride - 3], device.lastBufferData[stride - 2],
                        device.lastBufferData[stride - 1]},
                1.0e-6f);
    }

    @Test
    void theNormalStartsPointingDownTheZAxisAsGlSaysItDoes() {
        drawATriangle();

        int stride = FixedFunctionPipeline.VERTEX_FLOATS;
        assertEquals(1.0f, device.lastBufferData[stride - 1], 1.0e-6f);
    }

    @Test
    void lightingIsOffUntilItIsEnabled() {
        assertFalse(GL11.glIsEnabled(GL11.GL_LIGHTING));
        GL11.glEnable(GL11.GL_LIGHTING);
        assertTrue(GL11.glIsEnabled(GL11.GL_LIGHTING));
    }

    @Test
    void theShaderOnlyHearsAboutLightsWhileLightingIsOn() {
        drawATriangle();
        assertNull(device.uniformFloats("u_lightPosition[0]"));

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        drawATriangle();

        assertEquals(1, device.uniformInts("u_lightEnabled[0]")[0]);
        assertEquals(0, device.uniformInts("u_lightEnabled[0]")[1]);
    }

    @Test
    void aLightPositionIsFixedInEyeSpaceWhenItIsSet() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(10.0f, 0.0f, 0.0f);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, floats(1.0f, 2.0f, 3.0f, 1.0f));

        // moving the modelview afterwards must not drag the light along with it
        GL11.glLoadIdentity();
        drawATriangle();

        float[] positions = device.uniformFloats("u_lightPosition[0]");
        assertArrayEquals(new float[] {11.0f, 2.0f, 3.0f, 1.0f},
                new float[] {positions[0], positions[1], positions[2], positions[3]}, 1.0e-6f);
    }

    @Test
    void aDirectionalLightKeepsItsZeroW() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, floats(0.0f, 0.0f, 1.0f, 0.0f));
        drawATriangle();

        assertEquals(0.0f, device.uniformFloats("u_lightPosition[0]")[3], 1.0e-6f);
    }

    @Test
    void lightColoursReachTheShaderInTheirOwnUniforms() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT1);
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, floats(0.25f, 0.5f, 0.75f, 1.0f));
        drawATriangle();

        float[] diffuse = device.uniformFloats("u_lightDiffuse[0]");
        assertArrayEquals(new float[] {0.25f, 0.5f, 0.75f, 1.0f},
                new float[] {diffuse[4], diffuse[5], diffuse[6], diffuse[7]}, 1.0e-6f);
    }

    @Test
    void attenuationTakesItsThreeFactorsInOrder() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_CONSTANT_ATTENUATION, 0.5f);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_LINEAR_ATTENUATION, 0.25f);
        GL11.glLightf(GL11.GL_LIGHT0, GL11.GL_QUADRATIC_ATTENUATION, 0.125f);
        drawATriangle();

        float[] attenuation = device.uniformFloats("u_lightAttenuation[0]");
        assertArrayEquals(new float[] {0.5f, 0.25f, 0.125f},
                new float[] {attenuation[0], attenuation[1], attenuation[2]}, 1.0e-6f);
    }

    @Test
    void aLightOutsideTheEightNamesItself() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GL11.glLightf(GL11.GL_LIGHT7 + 1, GL11.GL_CONSTANT_ATTENUATION, 1.0f));
        assertTrue(failure.getMessage().contains("GL_LIGHT0"), failure.getMessage());
    }

    @Test
    void materialColoursReachTheShader() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE,
                floats(0.1f, 0.2f, 0.3f, 1.0f));
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 32.0f);
        drawATriangle();

        assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f, 1.0f},
                device.uniformFloats("u_materialDiffuse"), 1.0e-6f);
        assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f, 1.0f},
                device.uniformFloats("u_materialAmbient"), 1.0e-6f);
        assertArrayEquals(new float[] {32.0f}, device.uniformFloats("u_shininess"), 1.0e-6f);
    }

    @Test
    void aOneSidedMaterialSaysWhyItCannotBeHonoured() {
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, floats(1.0f, 1.0f, 1.0f, 1.0f)));
        assertTrue(failure.getMessage().contains("GL_FRONT_AND_BACK"), failure.getMessage());
    }

    @Test
    void colorMaterialTellsTheShaderToTakeTheVertexColour() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        drawATriangle();

        assertArrayEquals(new int[] {1}, device.uniformInts("u_colorMaterial"));
    }

    @Test
    void flatShadingSaysWhyItIsNotHere() {
        GL11.glShadeModel(GL11.GL_SMOOTH);
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> GL11.glShadeModel(GL11.GL_FLAT));
        assertTrue(failure.getMessage().contains("provoking"), failure.getMessage());
    }

    @Test
    void fogReachesTheShaderWithItsCurveAndItsColour() {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, 10.0f);
        GL11.glFogf(GL11.GL_FOG_END, 40.0f);
        GL11.glFog(GL11.GL_FOG_COLOR, floats(0.5f, 0.6f, 0.7f, 1.0f));
        drawATriangle();

        assertArrayEquals(new int[] {0}, device.uniformInts("u_fogMode"));
        assertArrayEquals(new float[] {10.0f}, device.uniformFloats("u_fogStart"), 1.0e-6f);
        assertArrayEquals(new float[] {40.0f}, device.uniformFloats("u_fogEnd"), 1.0e-6f);
        assertArrayEquals(new float[] {0.5f, 0.6f, 0.7f, 1.0f},
                device.uniformFloats("u_fogColor"), 1.0e-6f);
    }

    @Test
    void exponentialFogCarriesItsDensity() {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP2);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.125f);
        drawATriangle();

        assertArrayEquals(new int[] {2}, device.uniformInts("u_fogMode"));
        assertArrayEquals(new float[] {0.125f}, device.uniformFloats("u_fogDensity"), 1.0e-6f);
    }

    @Test
    void anUnknownFogCurveNamesTheThreeThatWork() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GL11.glFogi(GL11.GL_FOG_MODE, 0x1234));
        assertTrue(failure.getMessage().contains("GL_EXP2"), failure.getMessage());
    }

    @Test
    void aNormalArrayFeedsTheShaderTheSameSlot() {
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glNormalPointer(0, floats(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f));
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, floats(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f));
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);

        assertTrue(device.calls.contains("enableAttribute 6"), device.calls.toString());
    }

    @Test
    void theAttributeStackCarriesLightingAndTheCurrentNormal() {
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glNormal3f(0.0f, 1.0f, 0.0f);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glNormal3f(1.0f, 0.0f, 0.0f);
        GL11.glPopAttrib();

        assertTrue(GL11.glIsEnabled(GL11.GL_LIGHTING));
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3f(0.0f, 0.0f, 0.0f);
        GL11.glEnd();
        int stride = FixedFunctionPipeline.VERTEX_FLOATS;
        assertEquals(1.0f, device.lastBufferData[stride - 2], 1.0e-6f);
    }

    @Test
    void aDisplayListReplaysTheLightingCallsInsideIt() {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, floats(0.5f, 0.5f, 0.5f, 1.0f));
        GL11.glEndList();

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        drawATriangle();
        assertEquals(1.0f, device.uniformFloats("u_lightDiffuse[0]")[0], 1.0e-6f);

        GL11.glCallList(list);
        drawATriangle();
        assertEquals(0.5f, device.uniformFloats("u_lightDiffuse[0]")[0], 1.0e-6f);
    }
}
