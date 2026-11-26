package dev.flagon.platform.browser.audio;

import dev.flagon.platform.browser.JsFake;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.AudioDestinationNode;
import org.teavm.jso.webaudio.AudioListener;
import org.teavm.jso.webaudio.GainNode;
import org.teavm.jso.webaudio.PannerNode;

/**
 * web audio's one class among its interfaces, so this is written out where the rest are faked
 */
final class FakeAudioContext extends AudioContext {

    private final JsFake js;
    private final AudioListener listener;
    private final AudioDestinationNode destination;

    private final java.util.List<AudioBufferSourceNode> played = new java.util.ArrayList<>();
    private String state = AudioContext.STATE_RUNNING;
    private double now;

    FakeAudioContext(JsFake js) {
        this.js = js;
        this.listener = js.of(AudioListener.class);
        this.destination = js.of(AudioDestinationNode.class);
    }

    void state(String state) {
        this.state = state;
    }

    void advance(double seconds) {
        now += seconds;
    }

    @Override
    public AudioDestinationNode getDestination() {
        js.record("getDestination()");
        return destination;
    }

    @Override
    public AudioListener getListener() {
        return listener;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public double getCurrentTime() {
        return now;
    }

    @Override
    public void resume() {
        js.record("resume()");
    }

    @Override
    public void close() {
        js.record("close()");
    }

    @Override
    public GainNode createGain() {
        js.record("createGain()");
        return js.of(GainNode.class);
    }

    @Override
    public PannerNode createPanner() {
        js.record("createPanner()");
        return js.of(PannerNode.class);
    }

    @Override
    public AudioBufferSourceNode createBufferSource() {
        js.record("createBufferSource()");
        AudioBufferSourceNode node = js.of(AudioBufferSourceNode.class);
        played.add(node);
        return node;
    }

    /** the nodes handed out, so a test can end one the way the graph would */
    java.util.List<AudioBufferSourceNode> played() {
        return played;
    }

    @Override
    public AudioBuffer createBuffer(int channels, int frames, float sampleRate) {
        js.record("createBuffer(" + channels + ", " + frames + ", " + sampleRate + ")");
        return js.answer("getDuration", frames / (double) sampleRate).of(AudioBuffer.class);
    }
}
