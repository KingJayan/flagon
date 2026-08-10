package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.GLState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLUTest {

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

    private static FloatBuffer floats(int size) {
        return BufferUtils.createFloatBuffer(size);
    }

    private static IntBuffer viewport(int x, int y, int width, int height) {
        IntBuffer buffer = BufferUtils.createIntBuffer(4);
        buffer.put(new int[] {x, y, width, height}).flip();
        return buffer;
    }

    private static float[] matrix(int name) {
        FloatBuffer read = floats(16);
        GL11.glGetFloat(name, read);
        float[] values = new float[16];
        read.get(values);
        return values;
    }

    @Test
    void perspectiveBuildsTheFrustumItsFieldOfViewDescribes() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(90.0f, 2.0f, 1.0f, 101.0f);

        float[] projection = matrix(GL11.GL_PROJECTION_MATRIX);
        // a 90 degree vertical view at z=1 is a unit high, and twice that wide
        assertEquals(0.5f, projection[0], 1.0e-4f);
        assertEquals(1.0f, projection[5], 1.0e-4f);
        assertEquals(-1.0f, projection[11], 1.0e-4f);
    }

    @Test
    void aPerspectiveWithNoDepthIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> GLU.gluPerspective(90.0f, 1.0f, 1.0f, 1.0f));
    }

    @Test
    void lookAtPutsTheEyeAtTheOriginLookingDownNegativeZ() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GLU.gluLookAt(0.0f, 0.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        float[] modelView = matrix(GL11.GL_MODELVIEW_MATRIX);
        // the point looked at lands ten units in front of the eye
        assertEquals(0.0f, modelView[12], 1.0e-4f);
        assertEquals(0.0f, modelView[13], 1.0e-4f);
        assertEquals(-10.0f, modelView[14], 1.0e-4f);
        assertEquals(1.0f, modelView[0], 1.0e-4f);
    }

    @Test
    void ortho2DIsGlOrthoWithTheNearAndFarGlUses() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluOrtho2D(0.0f, 640.0f, 360.0f, 0.0f);

        float[] projection = matrix(GL11.GL_PROJECTION_MATRIX);
        assertEquals(2.0f / 640.0f, projection[0], 1.0e-6f);
        assertEquals(-2.0f / 360.0f, projection[5], 1.0e-6f);
        assertEquals(-1.0f, projection[10], 1.0e-6f);
    }

    @Test
    void projectPutsTheCentreOfClipSpaceAtTheCentreOfTheViewport() {
        FloatBuffer modelView = identity();
        FloatBuffer projection = identity();
        FloatBuffer window = floats(3);

        assertTrue(GLU.gluProject(0.0f, 0.0f, 0.0f, modelView, projection,
                viewport(0, 0, 640, 360), window));

        assertEquals(320.0f, window.get(0), 1.0e-4f);
        assertEquals(180.0f, window.get(1), 1.0e-4f);
        assertEquals(0.5f, window.get(2), 1.0e-4f);
    }

    @Test
    void unProjectUndoesWhatProjectDid() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(60.0f, 16.0f / 9.0f, 1.0f, 100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GLU.gluLookAt(0.0f, 0.0f, 8.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        FloatBuffer modelView = read(GL11.GL_MODELVIEW_MATRIX);
        FloatBuffer projection = read(GL11.GL_PROJECTION_MATRIX);
        IntBuffer view = viewport(0, 0, 640, 360);
        FloatBuffer window = floats(3);
        FloatBuffer object = floats(3);

        assertTrue(GLU.gluProject(2.0f, -1.0f, 3.0f, modelView, projection, view, window));
        assertTrue(GLU.gluUnProject(window.get(0), window.get(1), window.get(2),
                modelView, projection, view, object));

        assertEquals(2.0f, object.get(0), 1.0e-3f);
        assertEquals(-1.0f, object.get(1), 1.0e-3f);
        assertEquals(3.0f, object.get(2), 1.0e-3f);
    }

    @Test
    void aPointBehindTheEyeHasNoWindowPosition() {
        FloatBuffer modelView = identity();
        FloatBuffer projection = floats(16);
        projection.put(new float[16]).flip();

        assertFalse(GLU.gluProject(1.0f, 1.0f, 1.0f, modelView, projection,
                viewport(0, 0, 640, 360), floats(3)));
    }

    @Test
    void aShortMatrixNamesItself() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> GLU.gluProject(0.0f, 0.0f, 0.0f, floats(4), identity(),
                        viewport(0, 0, 1, 1), floats(3)));
        assertTrue(failure.getMessage().contains("sixteen"), failure.getMessage());
    }

    @Test
    void thePickMatrixNarrowsTheProjectionOntoTheCursor() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPickMatrix(320.0f, 180.0f, 64.0f, 36.0f, viewport(0, 0, 640, 360));

        float[] projection = matrix(GL11.GL_PROJECTION_MATRIX);
        assertEquals(10.0f, projection[0], 1.0e-4f);
        assertEquals(10.0f, projection[5], 1.0e-4f);
        assertEquals(0.0f, projection[12], 1.0e-4f);
    }

    @Test
    void anEmptyPickBoxIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> GLU.gluPickMatrix(0.0f, 0.0f, 0.0f, 4.0f, viewport(0, 0, 640, 360)));
    }

    @Test
    void buildingMipmapsUploadsTheBaseLevelAndBuildsTheRest() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, GL11.glGenTextures());
        device.calls.clear();

        assertEquals(0, GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, 4, 2, 2,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba(2 * 2)));

        assertEquals(1, device.calls("textureImage").size());
        assertTrue(device.calls.contains("generateMipmap"), device.calls.toString());
    }

    @Test
    void anEmptyMipmapUploadIsRefusedByItsReturn() {
        assertEquals(100901, GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, 4, 0, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba(1)));
    }

    @Test
    void scalingAnImageSamplesItAcrossTheNewSize() {
        ByteBuffer source = BufferUtils.createByteBuffer(2 * 2 * 4);
        put(source, 0, 10);
        put(source, 1, 20);
        put(source, 2, 30);
        put(source, 3, 40);
        ByteBuffer target = BufferUtils.createByteBuffer(1 * 1 * 4);

        assertEquals(0, GLU.gluScaleImage(GL11.GL_RGBA, 2, 2, GL11.GL_UNSIGNED_BYTE, source,
                1, 1, GL11.GL_UNSIGNED_BYTE, target));

        assertEquals(10, target.get(0));
    }

    @Test
    void scalingRefusesAFormatItCannotRead() {
        assertEquals(100900, GLU.gluScaleImage(0x1907, 2, 2, GL11.GL_UNSIGNED_BYTE, rgba(4),
                1, 1, GL11.GL_UNSIGNED_BYTE, rgba(1)));
    }

    @Test
    void scalingRefusesABufferTooSmallToHold() {
        assertEquals(100902, GLU.gluScaleImage(GL11.GL_RGBA, 4, 4, GL11.GL_UNSIGNED_BYTE, rgba(2),
                1, 1, GL11.GL_UNSIGNED_BYTE, rgba(1)));
    }

    @Test
    void everyErrorAProgramCanSeeHasAName() {
        assertEquals("no error", GLU.gluErrorString(GL11.GL_NO_ERROR));
        assertEquals("invalid enum", GLU.gluErrorString(GL11.GL_INVALID_ENUM));
        assertEquals("out of memory", GLU.gluErrorString(GLU.GLU_OUT_OF_MEMORY));
        assertTrue(GLU.gluErrorString(0x1234).contains("unknown"));
    }

    private static void put(ByteBuffer buffer, int pixel, int value) {
        for (int channel = 0; channel < 4; channel++) {
            buffer.put(pixel * 4 + channel, (byte) value);
        }
    }

    private static ByteBuffer rgba(int pixels) {
        return BufferUtils.createByteBuffer(pixels * 4);
    }

    private static FloatBuffer identity() {
        FloatBuffer buffer = floats(16);
        buffer.put(new float[] {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
        }).flip();
        return buffer;
    }

    private static FloatBuffer read(int name) {
        FloatBuffer buffer = floats(16);
        GL11.glGetFloat(name, buffer);
        return buffer;
    }
}
