package dev.flagon.platform.browser.audio;

import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.AudioException;
import dev.flagon.platform.audio.DistanceModel;
import dev.flagon.platform.audio.SourceState;
import dev.flagon.platform.browser.JsFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.teavm.jso.webaudio.AudioContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAudioDeviceTest {

    private JsFake audio;
    private FakeAudioContext context;
    private WebAudioDevice device;

    @BeforeEach
    void setUp() {
        audio = new JsFake();
        context = new FakeAudioContext(audio);
        device = WebAudioDevice.on(context);
    }

    /** every source hangs off one gain node, so a listener volume is one value and not a sweep */
    @Test
    void buildsAMasterGainAndConnectsIt() {
        assertEquals(1, audio.count("createGain"));
        assertEquals(1, audio.count("getDestination"));
    }

    @Test
    void aDistanceModelOfNoneFlattensThePannerWithoutForgettingTheRolloff() {
        int source = device.createSource();
        device.setSourceAttenuation(source, 4.0f, 2.0f, 100.0f);
        assertEquals("setRolloffFactor(2.0)", audio.lastCall("setRolloffFactor"));

        device.setDistanceModel(DistanceModel.NONE);
        assertEquals("setRolloffFactor(0.0)", audio.lastCall("setRolloffFactor"));
        assertEquals(2.0f, device.rolloffFactor(source));

        device.setDistanceModel(DistanceModel.INVERSE_CLAMPED);
        assertEquals("setRolloffFactor(2.0)", audio.lastCall("setRolloffFactor"));
    }

    @Test
    void reportsOutputLiveOnlyWhileTheContextIsRunning() {
        context.state(AudioContext.STATE_SUSPENDED);
        assertFalse(device.isOutputLive());

        context.state(AudioContext.STATE_RUNNING);
        assertTrue(device.isOutputLive());
    }

    /** a paused source resumes where it stopped, which is what the context clock is read for */
    @Test
    void resumesAPausedSourceWhereItLeftOff() {
        int source = sourceWithSound();
        device.play(source);

        context.advance(0.25);
        device.pause(source);
        device.play(source);

        assertEquals("start(0.0, 0.25)", audio.lastCall("start"));
    }

    @Test
    void countsATimeStretchedSourceAtTheRateItPlayedAt() {
        int source = sourceWithSound();
        device.setPitch(source, 2.0f);
        device.play(source);

        context.advance(0.5);
        device.pause(source);
        device.play(source);

        assertEquals("start(0.0, 1.0)", audio.lastCall("start"));
    }

    @Test
    void neverHandsOutTheSameNameTwice() {
        int buffer = device.createBuffer();
        int source = device.createSource();
        int another = device.createSource();

        assertNotEquals(buffer, source);
        assertNotEquals(source, another);
        assertNotEquals(AudioDevice.NO_BUFFER, buffer);
    }

    @Test
    void forgetsWhatItWasToldToDelete() {
        int source = device.createSource();
        assertTrue(device.isSource(source));

        device.deleteSource(source);

        assertFalse(device.isSource(source));
        assertThrows(AudioException.class, () -> device.play(source));
    }

    @Test
    void refusesABufferThatWasNeverCreated() {
        int source = device.createSource();

        assertThrows(AudioException.class, () -> device.attachBuffer(source, 404));
    }

    @Test
    void detachesABufferWithoutComplaining() {
        int source = device.createSource();

        device.attachBuffer(source, AudioDevice.NO_BUFFER);

        assertEquals(AudioDevice.NO_BUFFER, device.attachedBuffer(source));
    }

    @Test
    void stopsASourceWithNothingInItWhenToldToPlay() {
        int source = device.createSource();

        device.play(source);

        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void givesUpAStoppedQueueForASingleBuffer() {
        int queued = device.createBuffer();
        int single = device.createBuffer();
        soundIn(queued);
        soundIn(single);
        int source = device.createSource();
        device.queueBuffers(source, new int[] {queued});
        device.play(source);

        assertThrows(AudioException.class, () -> device.attachBuffer(source, single));
        device.stop(source);
        device.attachBuffer(source, single);

        assertEquals(0, device.buffersQueued(source));
        assertEquals(single, device.attachedBuffer(source));
    }

    // the state machine

    @Test
    void movesThroughTheStatesAnApplicationExpects() {
        int source = sourceWithSound();
        assertEquals(SourceState.INITIAL, device.sourceState(source));

        device.play(source);
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        device.pause(source);
        assertEquals(SourceState.PAUSED, device.sourceState(source));

        device.play(source);
        device.stop(source);
        assertEquals(SourceState.STOPPED, device.sourceState(source));

        device.rewind(source);
        assertEquals(SourceState.INITIAL, device.sourceState(source));
    }

    @Test
    void ignoresAPauseOfSomethingThatIsNotPlaying() {
        int source = sourceWithSound();

        device.pause(source);

        assertEquals(SourceState.INITIAL, device.sourceState(source));
    }

    /** web audio nodes are one-shot, so playing again is a new node every time */
    @Test
    void buildsANewNodeForEveryPlay() {
        int source = sourceWithSound();

        device.play(source);
        device.play(source);

        assertEquals(2, audio.count("createBufferSource"));
        assertEquals(1, audio.count("stop"), "the node it replaced was not stopped");
    }

    @Test
    void carriesLoopingOntoTheNodeItPlaysOn() {
        int source = sourceWithSound();

        device.setLooping(source, true);
        device.play(source);

        assertTrue(device.isLooping(source));
        assertTrue(audio.called("setLoop(true)"), audio.calls().toString());
    }

    @Test
    void refusesAPitchThatWouldStopTimeOrRunItBackwards() {
        int source = device.createSource();

        assertThrows(AudioException.class, () -> device.setPitch(source, 0.0f));
        assertThrows(AudioException.class, () -> device.setPitch(source, -1.0f));
        assertEquals(1.0f, device.pitch(source));
    }

    @Test
    void keepsThePitchItWasGiven() {
        int source = device.createSource();

        device.setPitch(source, 2.5f);

        assertEquals(2.5f, device.pitch(source));
    }

    @Test
    void putsTheGainItWasGivenOnTheSourcesOwnNode() {
        int source = device.createSource();

        device.setGain(source, 0.25f);

        assertEquals(0.25f, device.gain(source));
    }

    // position

    @Test
    void keepsASourceWhereItWasPut() {
        int source = device.createSource();
        float[] into = new float[3];

        device.setSourcePosition(source, 1.0f, 2.0f, 3.0f);
        device.sourcePosition(source, into);

        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, into);
        assertTrue(audio.called("setPosition(1.0, 2.0, 3.0)"), audio.calls().toString());
    }

    /** a relative source is placed against the listener, and moves when the listener does */
    @Test
    void movesARelativeSourceWithTheListener() {
        int source = device.createSource();
        device.setSourceRelative(source, true);
        device.setSourcePosition(source, 1.0f, 0.0f, 0.0f);

        device.setListenerPosition(10.0f, 0.0f, 0.0f);

        assertTrue(audio.called("setPosition(11.0, 0.0, 0.0)"), audio.calls().toString());
        float[] into = new float[3];
        device.sourcePosition(source, into);
        assertArrayEquals(new float[] {1.0f, 0.0f, 0.0f}, into, "the source itself did not move");
    }

    @Test
    void leavesAnAbsoluteSourceWhereItIsWhenTheListenerMoves() {
        int source = device.createSource();
        device.setSourcePosition(source, 1.0f, 0.0f, 0.0f);
        long placed = placements();

        device.setListenerPosition(10.0f, 0.0f, 0.0f);

        assertEquals(placed, placements(), "an absolute source was replaced");
    }

    @Test
    void startsFacingForwardWithUpWhereItBelongs() {
        float[] into = new float[6];

        device.listenerOrientation(into);

        assertArrayEquals(new float[] {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f}, into);
        assertTrue(audio.called("setOrientation(0.0, 0.0, -1.0, 0.0, 1.0, 0.0)"),
                audio.calls().toString());
    }

    @Test
    void refusesAnOrientationThatIsNotSixFloats() {
        assertThrows(AudioException.class, () -> device.setListenerOrientation(new float[3]));
        assertThrows(AudioException.class, () -> device.setListenerOrientation(null));
        assertThrows(AudioException.class, () -> device.listenerOrientation(new float[5]));
    }

    @Test
    void refusesAVectorTooSmallToHoldTheAnswer() {
        int source = device.createSource();

        assertThrows(AudioException.class, () -> device.sourcePosition(source, new float[2]));
    }

    /** the platform hands the same device to the next context, so it has to keep playing */
    @Test
    void letsEverythingGoOnShutdownAndPlaysAgain() {
        int source = sourceWithSound();
        device.setListenerGain(0.5f);
        device.setDopplerFactor(2.0f);
        device.play(source);

        device.shutdown();

        assertFalse(device.isSource(source));
        assertEquals(0, audio.count("close"));
        assertEquals(1.0f, device.listenerGain());
        assertEquals(1.0f, device.dopplerFactor());
        int next = sourceWithSound();
        device.play(next);
        assertEquals(SourceState.PLAYING, device.sourceState(next));
    }

    private long placements() {
        return audio.calls().stream().filter(call -> call.equals("setPosition(1.0, 0.0, 0.0)"))
                .count();
    }

    /** a stream is buffers queued up, played through in order, and taken back to be refilled */
    @Test
    void schedulesEveryQueuedBufferAndTakesBackWhatHasPlayed() {
        int source = device.createSource();
        int first = device.createBuffer();
        int second = device.createBuffer();
        soundIn(first);
        soundIn(second);

        device.queueBuffers(source, new int[]{first, second});
        assertEquals(2, device.buffersQueued(source));
        assertEquals(0, device.buffersProcessed(source));

        device.play(source);
        assertEquals(2, audio.count("createBufferSource"));
        assertEquals("start(2.0)", audio.lastCall("start"), audio.calls().toString());

        end(0);
        assertEquals(1, device.buffersProcessed(source));
        assertArrayEquals(new int[]{first}, device.unqueueBuffers(source, 1));
        assertEquals(1, device.buffersQueued(source));
        assertEquals(SourceState.PLAYING, device.sourceState(source));

        end(1);
        assertEquals(SourceState.STOPPED, device.sourceState(source));
    }

    @Test
    void refusesToTakeBackMoreThanHasPlayed() {
        int source = device.createSource();
        int buffer = device.createBuffer();
        soundIn(buffer);
        device.queueBuffers(source, new int[]{buffer});

        assertThrows(AudioException.class, () -> device.unqueueBuffers(source, 1));
    }

    @Test
    void keepsAQueueAndASingleBufferApart() {
        int streaming = device.createSource();
        int buffer = device.createBuffer();
        soundIn(buffer);
        device.queueBuffers(streaming, new int[]{buffer});
        device.play(streaming);

        assertThrows(AudioException.class, () -> device.attachBuffer(streaming, buffer));
        assertThrows(AudioException.class,
                () -> device.queueBuffers(sourceWithSound(), new int[]{buffer}));
    }

    @Test
    void putsEachDistanceModelsOwnCurveOnThePanner() {
        int source = device.createSource();
        device.setSourceAttenuation(source, 1.0f, 1.0f, 100.0f);

        device.setDistanceModel(DistanceModel.LINEAR);
        assertEquals("setDistanceModel(linear)", audio.lastCall("setDistanceModel"));

        device.setDistanceModel(DistanceModel.EXPONENT_CLAMPED);
        assertEquals("setDistanceModel(exponential)", audio.lastCall("setDistanceModel"));

        device.setDistanceModel(DistanceModel.INVERSE);
        assertEquals("setDistanceModel(inverse)", audio.lastCall("setDistanceModel"));
    }

    /** the graph reporting a scheduled buffer done, which is what advances a queue */
    private void end(int node) {
        context.played().get(node).getOnEnded().handleEvent(null);
    }

    /** a source with a buffer behind it, which is what play() insists on */
    private int sourceWithSound() {
        int buffer = device.createBuffer();
        int source = device.createSource();
        soundIn(buffer);
        device.attachBuffer(source, buffer);
        return source;
    }

    /** two seconds of it, so a play that resumes part way through has somewhere to resume to */
    private void soundIn(int buffer) {
        device.bufferData(buffer, dev.flagon.platform.audio.AudioFormat.MONO_8,
                new byte[16000], 16000, 8000);
    }
}
