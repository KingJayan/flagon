package dev.flagon.lwjgl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import dev.flagon.lwjgl.internal.GLState;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * lwjgl 3 splits each version into the core-profile half and the compatibility half, and modern
 * code imports the core half by name. what is on which side is the contract these tests hold.
 */
class CoreProfileClassesTest {

    /** anything gl 3.2 core removed; none of it may appear on a GLxxC class */
    private static final Set<String> COMPATIBILITY_ONLY = Set.of(
            "glBegin", "glEnd", "glVertex2f", "glVertex3f", "glColor3f", "glColor4f",
            "glNormal3f", "glTexCoord2f", "glMultiTexCoord1f", "glMultiTexCoord2f",
            "glMultiTexCoord3f", "glNewList", "glEndList", "glCallList", "glGenLists",
            "glDeleteLists", "glIsList", "glMatrixMode", "glLoadIdentity", "glLoadMatrix",
            "glLoadMatrixf", "glMultMatrix", "glMultMatrixf", "glPushMatrix", "glPopMatrix",
            "glTranslatef", "glRotatef", "glScalef", "glOrtho", "glFrustum", "glPushAttrib",
            "glPopAttrib", "glLight", "glLightf", "glLighti", "glLightfv", "glLightModel",
            "glLightModelfv", "glMaterial", "glMaterialf", "glMateriali", "glMaterialfv",
            "glColorMaterial", "glShadeModel", "glFog", "glFogf", "glFogi", "glFogfv",
            "glAlphaFunc", "glTexEnvf", "glTexEnvi", "glVertexPointer", "glColorPointer",
            "glNormalPointer", "glTexCoordPointer", "glEnableClientState",
            "glDisableClientState", "glClientActiveTexture", "glTranslated", "glScaled",
            "glTexEnv", "glTexGen", "glTexGeni", "glLogicOp");

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

    @Test
    void everyVersionExtendsItsCoreProfileHalf() {
        assertTrue(GL11C.class.isAssignableFrom(GL11.class));
        assertTrue(GL12C.class.isAssignableFrom(GL12.class));
        assertTrue(GL13C.class.isAssignableFrom(GL13.class));
        assertTrue(GL14C.class.isAssignableFrom(GL14.class));
        assertTrue(GL15C.class.isAssignableFrom(GL15.class));
        assertTrue(GL20C.class.isAssignableFrom(GL20.class));
    }

    @Test
    void noneOfTheFixedFunctionPipelineLeaksOntoACoreProfileClass() {
        List<String> leaked = new ArrayList<>();
        for (Class<?> core : List.of(GL11C.class, GL12C.class, GL13C.class, GL14C.class,
                GL15C.class, GL20C.class)) {
            for (Method method : core.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())
                        && COMPATIBILITY_ONLY.contains(method.getName())) {
                    leaked.add(core.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertEquals(List.of(), leaked, "these belong on the compatibility class");
    }

    /** the compatibility class still answers for the core half, as it did before the split */
    @Test
    void theCompatibilityClassStillCarriesTheCoreEntryPoints() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);

        assertEquals(List.of("clear true false false", "clear true false false"),
                device.calls("clear"));
        assertEquals(GL11.GL_TRIANGLES, GL11C.GL_TRIANGLES);
        assertEquals(GL15.GL_ARRAY_BUFFER, GL15C.GL_ARRAY_BUFFER);
        assertEquals(GL20.GL_VERTEX_SHADER, GL20C.GL_VERTEX_SHADER);
    }

    @Test
    void aCoreProfileCallGoesToTheSameDeviceTheCompatibilityCallWould() {
        int texture = GL11C.glGenTextures();
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE1);

        assertTrue(device.calls.contains("bindTexture 0 " + texture), device.calls.toString());
        assertTrue(device.calls.contains("activeTexture 1"), device.calls.toString());
    }
}
