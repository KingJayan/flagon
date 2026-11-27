package dev.flagon.lwjgl;

import dev.flagon.platform.Platforms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** openal as an lwjgl 3 program calls it: handles in place of objects, and capabilities */
class OpenAL3Test {

    private long device;
    private long context;

    @BeforeEach
    void setUp() {
        Platforms.install(new FakeDisplayPlatform());
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        ALCCapabilities deviceCapabilities = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        ALCapabilities capabilities = AL.createCapabilities(deviceCapabilities);
        assertTrue(deviceCapabilities.OpenALC11);
        assertTrue(capabilities.OpenAL11);
        assertFalse(capabilities.AL_EXT_FLOAT32);
    }

    @AfterEach
    void tearDown() {
        ALC10.alcMakeContextCurrent(0L);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
        Platforms.reset();
    }

    @Test
    void theHandlesNameWhatWasOpened() {
        assertNotEquals(0L, device);
        assertEquals(device, ALC10.alcGetContextsDevice(context));
        assertEquals("silent", ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER));
        assertEquals("silent", ALC10.alcGetString(0L, ALC11.ALC_ALL_DEVICES_SPECIFIER));
        assertEquals(ALC10.ALC_NO_ERROR, ALC10.alcGetError(device));
    }

    @Test
    void aDestroyedContextIsNoLongerItsHandle() {
        long other = ALC10.alcCreateContext(device, new int[]{0});
        ALC10.alcDestroyContext(other);

        assertFalse(ALC10.alcMakeContextCurrent(other));
        assertEquals(ALC10.ALC_INVALID_CONTEXT, ALC10.alcGetError(device));
    }

    @Test
    void capabilitiesNeedACurrentContext() {
        ALC10.alcMakeContextCurrent(0L);

        assertThrows(IllegalStateException.class,
                () -> AL.createCapabilities(ALC.getCapabilities()));
    }

    @Test
    void shortSamplesAreBufferedAsTheirBytes() {
        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, ShortBuffer.wrap(new short[8]), 44_100);

        assertEquals(16, AL10.alGetBufferi(buffer, AL10.AL_SIZE));
        assertEquals(2, AL10.alGetBufferi(buffer, AL10.AL_CHANNELS));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void theArrayAndBufferFormsReachTheSameSource() {
        int[] sources = new int[2];
        AL10.alGenSources(sources);
        AL10.alSourcefv(sources[0], AL10.AL_POSITION, new float[]{1, 2, 3});
        AL11.alSource3i(sources[1], AL10.AL_POSITION, 4, 5, 6);
        AL10.alSourcefv(sources[0], AL10.AL_GAIN, FloatBuffer.wrap(new float[]{0.5f}));

        float[] x = new float[1];
        float[] y = new float[1];
        float[] z = new float[1];
        AL10.alGetSource3f(sources[0], AL10.AL_POSITION, x, y, z);
        int[] position = new int[3];
        AL10.alGetSourceiv(sources[1], AL10.AL_POSITION, position);

        assertArrayEquals(new float[]{1, 2, 3}, new float[]{x[0], y[0], z[0]});
        assertArrayEquals(new int[]{4, 5, 6}, position);
        assertEquals(0.5f, AL10.alGetSourcef(sources[0], AL10.AL_GAIN));
    }

    @Test
    void aSourcePlaysFromAnArrayOfNames() {
        int source = AL10.alGenSources();
        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, ByteBuffer.allocateDirect(8), 8_000);
        AL10.alSourceQueueBuffers(source, new int[]{buffer});
        AL10.alSourcePlayv(new int[]{source});

        assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        assertEquals(1, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));
    }

    @Test
    void globalStateReadsInEveryNumberType() {
        AL10.alDopplerFactor(2.0f);

        assertEquals(2.0, AL10.alGetDouble(AL10.AL_DOPPLER_FACTOR));
        assertEquals(2, AL10.alGetInteger(AL10.AL_DOPPLER_FACTOR));
        assertTrue(AL10.alGetBoolean(AL10.AL_DOPPLER_FACTOR));
        assertEquals(AL10.AL_INVERSE_DISTANCE_CLAMPED, AL10.alGetInteger(AL10.AL_DISTANCE_MODEL));
    }

    @Test
    void enumNamesLookUpTheirValues() {
        assertEquals(AL10.AL_FORMAT_STEREO16, AL10.alGetEnumValue("AL_FORMAT_STEREO16"));
        assertEquals(ALC10.ALC_FREQUENCY, ALC10.alcGetEnumValue(device, "ALC_FREQUENCY"));
        ByteBuffer name = ByteBuffer.allocateDirect(16).put("AL_GAIN\0".getBytes()).flip();
        assertEquals(AL10.AL_GAIN, AL10.alGetEnumValue(name));
        assertEquals(0, AL10.alGetEnumValue("AL_EXT_NOT_HERE"));
    }

    @Test
    void thereIsNoCaptureDevice() {
        assertEquals(0L, ALC11.alcCaptureOpenDevice((CharSequence) null, 44_100,
                AL10.AL_FORMAT_MONO16, 1024));
        assertEquals(ALC10.ALC_INVALID_VALUE, ALC10.alcGetError(0L));
    }
}
