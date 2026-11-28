package dev.flagon.platform.audio;

import dev.flagon.platform.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentAudioDeviceTest {

    /** a clock the test moves by hand, so playback can be watched running out. */
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
    private SilentAudioDevice device;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        device = new SilentAudioDevice(clock);
    }

    /** one second of 16-bit mono at 8 kHz. */
    private int oneSecondBuffer() {
        int buffer = device.createBuffer();
        device.bufferData(buffer, AudioFormat.MONO_16, new byte[16_000], 16_000, 8_000);
        return buffer;
    }

    @Test
    void namesAreHandedOutOnceEach() {
        int first = device.createBuffer();
        int second = device.createSource();

        assertTrue(device.isBuffer(first));
        assertTrue(device.isSource(second));
        assertFalse(device.isBuffer(second));
        assertFalse(device.isSource(first));
    }

    @Test
    void aBufferReportsWhatItWasGiven() {
        int buffer = oneSecondBuffer();

        assertEquals(AudioFormat.MONO_16, device.bufferFormat(buffer));
        assertEquals(8_000, device.bufferSampleRate(buffer));
        assertEquals(16_000, device.bufferSizeInBytes(buffer));
    }

    @Test
    void aSourceStartsBeforeItHasPlayed() {
        int source = device.createSource();

        assertEquals(SourceState.INITIAL, device.sourceState(source));
    }

    @Test
    void playingWithNoBufferStopsTheSource() {
        int source = device.createSource();

        device.play(source);

        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    /** the point of timing a silent device: code that waits for a sound to end has to be let go. */
    @Test
    void aSourceStopsWhenItsBufferRunsOut() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.play(source);

        clock.millis += 500;
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        clock.millis += 600;
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void aLoopingSourceNeverRunsOut() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.setLooping(source, true);
        device.play(source);

        clock.millis += 10_000;

        assertEquals(SourceState.PLAYING, device.sourceState(source));
    }

    @Test
    void pausingKeepsThePositionAndPlayingResumesFromIt() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.play(source);

        clock.millis += 900;
        device.pause(source);
        assertEquals(SourceState.PAUSED, device.sourceState(source));

        clock.millis += 10_000;
        device.play(source);
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        clock.millis += 200;
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void stoppingReturnsToTheStart() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.play(source);
        clock.millis += 500;
        device.stop(source);

        device.play(source);
        clock.millis += 900;

        assertEquals(SourceState.PLAYING, device.sourceState(source));
    }

    @Test
    void rewindingLeavesASourceAsItStarted() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.play(source);
        device.rewind(source);

        assertEquals(SourceState.INITIAL, device.sourceState(source));
    }

    /** a doubled rate plays a one-second buffer out in half a second. */
    @Test
    void pitchScalesHowFastTheBufferIsConsumed() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.setPitch(source, 2.0f);
        device.play(source);

        clock.millis += 400;
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        clock.millis += 200;
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void aPitchChangeMidPlaybackDoesNotRewriteWhatAlreadyPlayed() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.play(source);

        clock.millis += 900;
        device.setPitch(source, 4.0f);
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        clock.millis += 50;
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void aPitchOfZeroIsRefused() {
        int source = device.createSource();

        assertThrows(AudioException.class, () -> device.setPitch(source, 0.0f));
    }

    @Test
    void attachingABufferThatDoesNotExistIsRefused() {
        int source = device.createSource();

        assertThrows(AudioException.class, () -> device.attachBuffer(source, 404));
    }

    @Test
    void detachingLeavesNothingToPlay() {
        int source = device.createSource();
        device.attachBuffer(source, oneSecondBuffer());
        device.attachBuffer(source, AudioDevice.NO_BUFFER);

        assertEquals(AudioDevice.NO_BUFFER, device.attachedBuffer(source));
        device.play(source);
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void vectorsComeBackAsTheyWereSet() {
        int source = device.createSource();
        device.setSourcePosition(source, 1.0f, 2.0f, 3.0f);
        device.setSourceVelocity(source, -1.0f, 0.0f, 0.5f);
        device.setListenerPosition(4.0f, 5.0f, 6.0f);

        float[] read = new float[3];
        device.sourcePosition(source, read);
        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, read);
        device.sourceVelocity(source, read);
        assertArrayEquals(new float[] {-1.0f, 0.0f, 0.5f}, read);
        device.listenerPosition(read);
        assertArrayEquals(new float[] {4.0f, 5.0f, 6.0f}, read);
    }

    @Test
    void theListenerFacesDownNegativeZUntilToldOtherwise() {
        float[] read = new float[6];
        device.listenerOrientation(read);

        assertArrayEquals(new float[] {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f}, read);
    }

    @Test
    void anOrientationOfFewerThanSixFloatsIsRefused() {
        assertThrows(AudioException.class, () -> device.setListenerOrientation(new float[3]));
    }

    @Test
    void namingSomethingDeletedIsRefused() {
        int source = device.createSource();
        device.deleteSource(source);

        assertFalse(device.isSource(source));
        assertThrows(AudioException.class, () -> device.play(source));
    }

    @Test
    void shutdownLetsEverythingGo() {
        int source = device.createSource();
        int buffer = oneSecondBuffer();

        device.setDistanceModel(DistanceModel.LINEAR);
        device.setListenerGain(0.5f);

        device.shutdown();

        assertFalse(device.isSource(source));
        assertFalse(device.isBuffer(buffer));
        assertEquals(DistanceModel.INVERSE_CLAMPED, device.distanceModel());
        assertEquals(1.0f, device.listenerGain());
    }

    /** a queue is played through in order, and its clock keeps running as buffers are taken back */
    @Test
    void playsAQueueThroughAndCountsWhatItFinished() {
        int source = device.createSource();
        int first = oneSecondBuffer();
        int second = oneSecondBuffer();
        device.queueBuffers(source, new int[]{first, second});

        assertEquals(2, device.buffersQueued(source));
        device.play(source);

        clock.millis += 1_100;
        assertEquals(1, device.buffersProcessed(source));
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        assertArrayEquals(new int[]{first}, device.unqueueBuffers(source, 1));
        assertEquals(1, device.buffersQueued(source));
        assertEquals(0, device.buffersProcessed(source));

        clock.millis += 1_000;
        assertEquals(1, device.buffersProcessed(source));
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void refusesToHandBackABufferThatIsStillPlaying() {
        int source = device.createSource();
        device.queueBuffers(source, new int[]{oneSecondBuffer()});
        device.play(source);

        assertThrows(AudioException.class, () -> device.unqueueBuffers(source, 1));
    }

    @Test
    void willNotHoldAQueueAndASingleBufferAtOnce() {
        int streaming = device.createSource();
        int buffer = oneSecondBuffer();
        device.queueBuffers(streaming, new int[]{buffer});
        device.play(streaming);
        assertThrows(AudioException.class, () -> device.attachBuffer(streaming, buffer));

        int single = device.createSource();
        device.attachBuffer(single, buffer);
        assertThrows(AudioException.class, () -> device.queueBuffers(single, new int[]{buffer}));
    }

    @Test
    void stoppingAQueueMarksEverythingOnItPlayed() {
        int source = device.createSource();
        device.queueBuffers(source, new int[]{oneSecondBuffer(), oneSecondBuffer()});
        device.play(source);

        device.stop(source);

        assertEquals(2, device.buffersProcessed(source));
        assertEquals(2, device.unqueueBuffers(source, 2).length);
    }
}
