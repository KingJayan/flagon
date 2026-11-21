package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.platform.Clock;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.DistanceModel;
import dev.flagon.platform.audio.SilentAudioDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AL10Test {

    private static final class TestClock implements Clock {
        long millis = 1_000L;

        @Override
        public long currentTimeMillis() {
            return millis;
        }

        @Override
        public long nanoTime() {
            return millis * 1_000_000L;
        }
    }

    private TestClock clock;
    private AudioDevice device;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        device = new SilentAudioDevice(clock);
        ALState.attach(device, null);
    }

    @AfterEach
    void tearDown() {
        ALState.detach();
    }

    private int buffer(int lengthInBytes) {
        int buffer = AL10.alGenBuffers();
        ByteBuffer pcm = BufferUtils.createByteBuffer(lengthInBytes);
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, pcm, 8_000);
        return buffer;
    }

    @Test
    void generatedNamesAreDistinctAndKnown() {
        int buffer = AL10.alGenBuffers();
        int source = AL10.alGenSources();

        assertNotEquals(buffer, source);
        assertTrue(AL10.alIsBuffer(buffer));
        assertTrue(AL10.alIsSource(source));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void namesAreGeneratedIntoABuffer() {
        IntBuffer names = BufferUtils.createIntBuffer(3);

        AL10.alGenSources(names);

        for (int at = 0; at < 3; at++) {
            assertTrue(AL10.alIsSource(names.get(at)), "source " + at);
        }
    }

    @Test
    void aBufferReportsTheFormatItWasGiven() {
        int buffer = buffer(16_000);

        assertEquals(8_000, AL10.alGetBufferi(buffer, AL10.AL_FREQUENCY));
        assertEquals(16, AL10.alGetBufferi(buffer, AL10.AL_BITS));
        assertEquals(1, AL10.alGetBufferi(buffer, AL10.AL_CHANNELS));
        assertEquals(16_000, AL10.alGetBufferi(buffer, AL10.AL_SIZE));
    }

    /** openal hands an error back one call later rather than throwing, and callers poll for it. */
    @Test
    void namingSomethingThatDoesNotExistLatchesAnError() {
        AL10.alSourcePlay(4_242);

        assertEquals(AL10.AL_INVALID_NAME, AL10.alGetError());
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void theFirstErrorIsTheOneKept() {
        AL10.alSourcePlay(4_242);
        AL10.alBufferData(1_111, AL10.AL_FORMAT_MONO8, BufferUtils.createByteBuffer(4), 8_000);

        assertEquals(AL10.AL_INVALID_NAME, AL10.alGetError());
    }

    @Test
    void anUnknownFormatIsAnInvalidEnum() {
        int buffer = AL10.alGenBuffers();

        AL10.alBufferData(buffer, 0x9999, BufferUtils.createByteBuffer(4), 8_000);

        assertEquals(AL10.AL_INVALID_ENUM, AL10.alGetError());
    }

    @Test
    void halfAFrameIsAnInvalidValue() {
        int buffer = AL10.alGenBuffers();

        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, BufferUtils.createByteBuffer(3), 8_000);

        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
    }

    @Test
    void aSourceRunsFromInitialToStopped() {
        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer(16_000));

        assertEquals(AL10.AL_INITIAL, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        AL10.alSourcePlay(source);
        assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        clock.millis += 2_000;
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void playingASourceWithNoBufferStopsIt() {
        int source = AL10.alGenSources();

        AL10.alSourcePlay(source);

        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
    }

    @Test
    void pausingAndStoppingReportThemselves() {
        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer(16_000));
        AL10.alSourcePlay(source);

        AL10.alSourcePause(source);
        assertEquals(AL10.AL_PAUSED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        AL10.alSourceStop(source);
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        AL10.alSourceRewind(source);
        assertEquals(AL10.AL_INITIAL, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
    }

    @Test
    void aWholeBufferOfSourcesPlaysAtOnce() {
        IntBuffer sources = BufferUtils.createIntBuffer(2);
        AL10.alGenSources(sources);
        int buffer = buffer(16_000);
        for (int at = 0; at < 2; at++) {
            AL10.alSourcei(sources.get(at), AL10.AL_BUFFER, buffer);
        }

        AL10.alSourcePlay(sources);

        for (int at = 0; at < 2; at++) {
            assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(sources.get(at), AL10.AL_SOURCE_STATE));
        }
    }

    @Test
    void sourcePropertiesComeBackAsTheyWereSet() {
        int source = AL10.alGenSources();

        AL10.alSourcef(source, AL10.AL_GAIN, 0.25f);
        AL10.alSourcef(source, AL10.AL_PITCH, 1.5f);
        AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_TRUE);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(source, AL10.AL_POSITION, 1.0f, 2.0f, 3.0f);

        assertEquals(0.25f, AL10.alGetSourcef(source, AL10.AL_GAIN));
        assertEquals(1.5f, AL10.alGetSourcef(source, AL10.AL_PITCH));
        assertEquals(AL10.AL_TRUE, AL10.alGetSourcei(source, AL10.AL_LOOPING));
        assertEquals(AL10.AL_TRUE, AL10.alGetSourcei(source, AL10.AL_SOURCE_RELATIVE));

        FloatBuffer position = BufferUtils.createFloatBuffer(3);
        AL10.alGetSource(source, AL10.AL_POSITION, position);
        assertEquals(1.0f, position.get(0));
        assertEquals(2.0f, position.get(1));
        assertEquals(3.0f, position.get(2));
    }

    @Test
    void theBufferFormSetsTheSameVectorTheScalarFormDoes() {
        int source = AL10.alGenSources();
        FloatBuffer position = BufferUtils.createFloatBuffer(3);
        position.put(new float[] {4.0f, 5.0f, 6.0f}).flip();

        AL10.alSource(source, AL10.AL_POSITION, position);

        FloatBuffer read = BufferUtils.createFloatBuffer(3);
        AL10.alGetSource(source, AL10.AL_POSITION, read);
        assertEquals(4.0f, read.get(0));
        assertEquals(5.0f, read.get(1));
        assertEquals(6.0f, read.get(2));
    }

    @Test
    void theBufferFormRefusesABufferWithNoVectorInIt() {
        int source = AL10.alGenSources();

        AL10.alSource(source, AL10.AL_POSITION, BufferUtils.createFloatBuffer(2));

        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
    }

    @Test
    void aNegativeGainIsAnInvalidValue() {
        int source = AL10.alGenSources();

        AL10.alSourcef(source, AL10.AL_GAIN, -1.0f);

        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
        assertEquals(1.0f, AL10.alGetSourcef(source, AL10.AL_GAIN));
    }

    /** a cone angle that quietly did nothing would be a sound at the wrong volume forever. */
    @Test
    void aParameterWithNoCounterpartIsRefusedByName() {
        int source = AL10.alGenSources();

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                () -> AL10.alSourcef(source, AL10.AL_CONE_OUTER_GAIN, 0.5f));

        assertTrue(thrown.getMessage().contains("alSourcef"), thrown.getMessage());
    }

    /** a stream is buffers handed over, played through, and handed back to be refilled. */
    @Test
    void aQueueIsPlayedThroughAndHandedBack() {
        int source = AL10.alGenSources();
        int first = buffer(16_000);
        int second = buffer(16_000);
        AL10.alSourceQueueBuffers(source, first);
        AL10.alSourceQueueBuffers(source, second);

        assertEquals(2, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));
        assertEquals(0, AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED));

        AL10.alSourcePlay(source);
        clock.millis += 1_100;

        assertEquals(1, AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED));
        assertEquals(first, AL10.alSourceUnqueueBuffers(source));
        assertEquals(1, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));
        assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        clock.millis += 1_100;
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void takingBackMoreThanHasPlayedIsAnInvalidValue() {
        int source = AL10.alGenSources();
        AL10.alSourceQueueBuffers(source, buffer(16_000));

        IntBuffer taken = BufferUtils.createIntBuffer(1);
        AL10.alSourceUnqueueBuffers(source, taken);

        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
    }

    @Test
    void aPlayingQueueRefusesASingleBuffer() {
        int source = AL10.alGenSources();
        AL10.alSourceQueueBuffers(source, buffer(16_000));
        AL10.alSourcePlay(source);

        AL10.alSourcei(source, AL10.AL_BUFFER, buffer(16_000));

        assertEquals(AL10.AL_INVALID_OPERATION, AL10.alGetError());
    }

    /** paulscode sets a stopped stream's buffer to rewind it */
    @Test
    void aStoppedQueueGivesWayToASingleBuffer() {
        int source = AL10.alGenSources();
        AL10.alSourceQueueBuffers(source, buffer(16_000));
        int single = buffer(16_000);

        AL10.alSourcei(source, AL10.AL_BUFFER, single);

        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
        assertEquals(0, AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED));
        assertEquals(single, AL10.alGetSourcei(source, AL10.AL_BUFFER));
    }

    @Test
    void listenerPropertiesComeBackAsTheyWereSet() {
        AL10.alListenerf(AL10.AL_GAIN, 0.5f);
        AL10.alListener3f(AL10.AL_POSITION, 7.0f, 8.0f, 9.0f);
        FloatBuffer orientation = BufferUtils.createFloatBuffer(6);
        orientation.put(new float[] {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f}).flip();
        AL10.alListener(AL10.AL_ORIENTATION, orientation);

        assertEquals(0.5f, AL10.alGetListenerf(AL10.AL_GAIN));

        FloatBuffer read = BufferUtils.createFloatBuffer(6);
        AL10.alGetListener(AL10.AL_POSITION, read);
        assertEquals(7.0f, read.get(0));
        assertEquals(9.0f, read.get(2));
        AL10.alGetListener(AL10.AL_ORIENTATION, read);
        assertEquals(1.0f, read.get(2));
    }

    @Test
    void anOrientationShorterThanSixFloatsIsAnInvalidValue() {
        AL10.alListener(AL10.AL_ORIENTATION, BufferUtils.createFloatBuffer(3));

        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
    }

    /** the device is told the model; what a source was told about distance never changes. */
    @Test
    void theDistanceModelReachesTheDeviceAndTheSourceKeepsItsNumbers() {
        int source = AL10.alGenSources();
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 2.0f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 4.0f);

        AL10.alDistanceModel(AL10.AL_NONE);
        assertEquals(DistanceModel.NONE, device.distanceModel());
        assertEquals(2.0f, device.rolloffFactor(source));
        assertEquals(4.0f, device.referenceDistance(source));
        assertEquals(2.0f, AL10.alGetSourcef(source, AL10.AL_ROLLOFF_FACTOR));
        assertEquals(AL10.AL_NONE, AL10.alGetInteger(AL10.AL_DISTANCE_MODEL));

        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);
        assertEquals(DistanceModel.INVERSE_CLAMPED, device.distanceModel());
    }

    @Test
    void everyDistanceModelOpenalNamesReachesTheDevice() {
        AL10.alDistanceModel(AL10.AL_LINEAR_DISTANCE);
        assertEquals(DistanceModel.LINEAR, device.distanceModel());
        assertEquals(AL10.AL_LINEAR_DISTANCE, AL10.alGetInteger(AL10.AL_DISTANCE_MODEL));

        AL10.alDistanceModel(AL10.AL_EXPONENT_DISTANCE_CLAMPED);
        assertEquals(DistanceModel.EXPONENT_CLAMPED, device.distanceModel());
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void aVelocityAlongTheLineShiftsTheSource() {
        int source = AL10.alGenSources();
        AL10.alSource3f(source, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
        AL10.alListener3f(AL10.AL_POSITION, 0.0f, 0.0f, 10.0f);

        assertEquals(1.0f, device.dopplerShift(source), 1.0e-6f);

        AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0f, 0.0f, 30.0f);
        assertTrue(device.dopplerShift(source) > 1.0f);

        AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0f, 0.0f, -30.0f);
        assertTrue(device.dopplerShift(source) < 1.0f);
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void theDopplerFactorAndSpeedOfSoundReachTheDevice() {
        AL10.alDopplerFactor(2.0f);
        assertEquals(2.0f, device.dopplerFactor());
        assertEquals(2.0f, AL10.alGetFloat(AL10.AL_DOPPLER_FACTOR));

        AL10.alSpeedOfSound(400.0f);
        assertEquals(400.0f, device.speedOfSound());
        assertEquals(400.0f, AL10.alGetFloat(AL10.AL_SPEED_OF_SOUND));

        // al 1.0 named the same speed the other way round
        AL10.alDopplerVelocity(320.0f);
        assertEquals(320.0f, device.speedOfSound());
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void aZeroFactorLeavesTheSourceUnshifted() {
        int source = AL10.alGenSources();
        AL10.alListener3f(AL10.AL_POSITION, 0.0f, 0.0f, 10.0f);
        AL10.alSource3f(source, AL10.AL_VELOCITY, 0.0f, 0.0f, 30.0f);

        AL10.alDopplerFactor(0.0f);
        assertEquals(1.0f, device.dopplerShift(source), 1.0e-6f);
    }

    @Test
    void aNegativeFactorOrSpeedIsAnInvalidValue() {
        AL10.alDopplerFactor(-1.0f);
        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
        assertEquals(1.0f, device.dopplerFactor());

        AL10.alSpeedOfSound(0.0f);
        assertEquals(AL10.AL_INVALID_VALUE, AL10.alGetError());
    }

    @Test
    void aNumberThatNamesNoDistanceModelIsAnInvalidEnum() {
        AL10.alDistanceModel(0xD00F);

        assertEquals(AL10.AL_INVALID_ENUM, AL10.alGetError());
    }

    @Test
    void deletingLeavesTheNameUnknown() {
        int source = AL10.alGenSources();
        int buffer = AL10.alGenBuffers();

        AL10.alDeleteSources(source);
        AL10.alDeleteBuffers(buffer);

        assertFalse(AL10.alIsSource(source));
        assertFalse(AL10.alIsBuffer(buffer));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void theRendererNamesTheDeviceUnderneath() {
        assertEquals("silent", AL10.alGetString(AL10.AL_RENDERER));
        assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
    }

    @Test
    void callingBeforeCreateIsAnIllegalState() {
        ALState.detach();

        assertThrows(IllegalStateException.class, AL10::alGenSources);
    }
}
