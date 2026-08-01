package dev.flagon.platform.browser.audio;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.audio.Audio;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.AudioException;
import dev.flagon.platform.audio.AudioFormat;
import dev.flagon.platform.audio.SourceState;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;
import org.teavm.jso.webaudio.PannerNode;

import java.util.HashMap;
import java.util.Map;

/**
 * an {@link AudioDevice} on web audio: one node graph per source, built when it plays
 */
public final class WebAudioDevice implements AudioDevice {

    private static final class Buffer {
        AudioBuffer audio;
        AudioFormat format = AudioFormat.MONO_16;
        int sampleRate;
        int sizeInBytes;
    }

    private final class Source {
        final GainNode gain = context.createGain();
        final PannerNode panner = context.createPanner();
        AudioBufferSourceNode node;
        int buffer = NO_BUFFER;
        SourceState state = SourceState.INITIAL;
        boolean looping;
        boolean relative;
        float pitch = 1.0f;
        final float[] position = new float[3];
        final float[] velocity = new float[3];

        int generation;
        double offset;
        double startedAt;

        Source() {
            panner.setDistanceModel(PannerNode.DISTANCE_MODEL_INVERSE);
            panner.setPanningModel(PannerNode.MODEL_EQUALPOWER);
            gain.connect(panner);
            panner.connect(master);
            place();
        }

        void place() {
            float x = position[0];
            float y = position[1];
            float z = position[2];
            if (relative) {
                x += listenerPosition[0];
                y += listenerPosition[1];
                z += listenerPosition[2];
            }
            panner.setPosition(x, y, z);
        }

        double elapsed() {
            if (state != SourceState.PLAYING) {
                return offset;
            }
            return offset + (context.getCurrentTime() - startedAt) * pitch;
        }

        void release() {
            if (node != null) {
                generation++;
                node.stop();
                node.disconnect();
                node = null;
            }
        }

        void dispose() {
            release();
            gain.disconnect();
            panner.disconnect();
        }
    }

    private final AudioContext context;
    private final GainNode master;
    private final Map<Integer, Buffer> buffers = new HashMap<>();
    private final Map<Integer, Source> sources = new HashMap<>();
    private final float[] listenerPosition = new float[3];
    private final float[] listenerVelocity = new float[3];
    private final float[] listenerOrientation = {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f};
    private int nextName = 1;

    private WebAudioDevice(AudioContext context) {
        this.context = context;
        this.master = context.createGain();
        master.connect(context.getDestination());
        applyListenerOrientation();
    }

    static WebAudioDevice on(AudioContext context) {
        return new WebAudioDevice(context);
    }

    /**
     * @return the device, or null where the page has no web audio at all
     */
    public static WebAudioDevice createIfAvailable(ConsoleSink console) {
        if (!WebAudio.isAvailable()) {
            return null;
        }
        WebAudioDevice device = new WebAudioDevice(new AudioContext());
        device.awaitGesture(console);
        return device;
    }

    /**
     * a context built without a user gesture starts suspended
     */
    private void awaitGesture(ConsoleSink console) {
        context.resume();
        if (isOutputLive()) {
            return;
        }
        console.warn("audio is suspended until the page gets a click or a key press");
        HTMLDocument document = Window.current().getDocument();
        document.addEventListener("pointerdown", event -> context.resume());
        document.addEventListener("keydown", event -> context.resume());
    }

    @Override
    public String name() {
        return "webaudio";
    }

    @Override
    public boolean isOutputLive() {
        return AudioContext.STATE_RUNNING.equals(context.getState());
    }

    @Override
    public int createBuffer() {
        int name = nextName++;
        buffers.put(name, new Buffer());
        return name;
    }

    @Override
    public void bufferData(int buffer, AudioFormat format, byte[] data, int length, int sampleRate) {
        Audio.requireWholeFrames(format, length, sampleRate);
        Buffer held = buffer(buffer);
        int frames = Audio.frames(format, length);
        AudioBuffer audio = context.createBuffer(format.channels(), Math.max(frames, 1), sampleRate);
        for (int channel = 0; channel < format.channels(); channel++) {
            audio.copyToChannel(Audio.channelSamples(format, data, length, channel), channel);
        }
        held.audio = audio;
        held.format = format;
        held.sampleRate = sampleRate;
        held.sizeInBytes = length;
    }

    @Override
    public void deleteBuffer(int buffer) {
        buffers.remove(buffer);
    }

    @Override
    public boolean isBuffer(int buffer) {
        return buffers.containsKey(buffer);
    }

    @Override
    public AudioFormat bufferFormat(int buffer) {
        return buffer(buffer).format;
    }

    @Override
    public int bufferSampleRate(int buffer) {
        return buffer(buffer).sampleRate;
    }

    @Override
    public int bufferSizeInBytes(int buffer) {
        return buffer(buffer).sizeInBytes;
    }

    @Override
    public int createSource() {
        int name = nextName++;
        sources.put(name, new Source());
        return name;
    }

    @Override
    public void deleteSource(int source) {
        Source held = sources.remove(source);
        if (held != null) {
            held.dispose();
        }
    }

    @Override
    public boolean isSource(int source) {
        return sources.containsKey(source);
    }

    @Override
    public void attachBuffer(int source, int buffer) {
        if (buffer != NO_BUFFER && !isBuffer(buffer)) {
            throw new AudioException("not a buffer: " + buffer);
        }
        Source held = source(source);
        held.release();
        held.buffer = buffer;
        held.offset = 0.0;
        held.state = SourceState.INITIAL;
    }

    @Override
    public int attachedBuffer(int source) {
        return source(source).buffer;
    }

    @Override
    public void play(int source) {
        Source held = source(source);
        Buffer buffer = buffers.get(held.buffer);
        if (buffer == null || buffer.audio == null) {
            throw new AudioException("source " + source + " has no buffer to play");
        }
        double from = held.state == SourceState.PAUSED ? held.offset : 0.0;
        if (from >= buffer.audio.getDuration()) {
            from = 0.0;
        }
        held.release();

        AudioBufferSourceNode node = context.createBufferSource();
        node.setBuffer(buffer.audio);
        node.setLoop(held.looping);
        node.getPlaybackRate().setValue(held.pitch);
        node.connect(held.gain);

        int generation = held.generation;
        node.setOnEnded(event -> {
            if (held.generation == generation && held.state == SourceState.PLAYING) {
                held.state = SourceState.STOPPED;
                held.offset = 0.0;
                held.node = null;
            }
        });

        held.node = node;
        held.offset = from;
        held.startedAt = context.getCurrentTime();
        held.state = SourceState.PLAYING;
        node.start(0.0, from);
    }

    @Override
    public void pause(int source) {
        Source held = source(source);
        if (held.state != SourceState.PLAYING) {
            return;
        }
        double at = held.elapsed();
        held.release();
        held.offset = at;
        held.state = SourceState.PAUSED;
    }

    @Override
    public void stop(int source) {
        Source held = source(source);
        held.release();
        held.offset = 0.0;
        held.state = SourceState.STOPPED;
    }

    @Override
    public void rewind(int source) {
        Source held = source(source);
        held.release();
        held.offset = 0.0;
        held.state = SourceState.INITIAL;
    }

    @Override
    public SourceState sourceState(int source) {
        return source(source).state;
    }

    @Override
    public void setLooping(int source, boolean looping) {
        Source held = source(source);
        held.looping = looping;
        if (held.node != null) {
            held.node.setLoop(looping);
        }
    }

    @Override
    public boolean isLooping(int source) {
        return source(source).looping;
    }

    @Override
    public void setGain(int source, float gain) {
        source(source).gain.getGain().setValue(gain);
    }

    @Override
    public float gain(int source) {
        return source(source).gain.getGain().getValue();
    }

    @Override
    public void setPitch(int source, float pitch) {
        if (pitch <= 0.0f) {
            throw new AudioException("pitch must be positive: " + pitch);
        }
        Source held = source(source);
        // what has played so far played at the old rate, so bank it before the rate changes
        if (held.state == SourceState.PLAYING) {
            held.offset = held.elapsed();
            held.startedAt = context.getCurrentTime();
        }
        held.pitch = pitch;
        if (held.node != null) {
            held.node.getPlaybackRate().setValue(pitch);
        }
    }

    @Override
    public float pitch(int source) {
        return source(source).pitch;
    }

    @Override
    public void setSourceRelative(int source, boolean relative) {
        Source held = source(source);
        held.relative = relative;
        held.place();
    }

    @Override
    public boolean isSourceRelative(int source) {
        return source(source).relative;
    }

    @Override
    public void setSourcePosition(int source, float x, float y, float z) {
        Source held = source(source);
        set(held.position, x, y, z);
        held.place();
    }

    @Override
    public void setSourceVelocity(int source, float x, float y, float z) {
        set(source(source).velocity, x, y, z);
    }

    @Override
    public void sourcePosition(int source, float[] into) {
        copy(source(source).position, into);
    }

    @Override
    public void sourceVelocity(int source, float[] into) {
        copy(source(source).velocity, into);
    }

    @Override
    public void setSourceAttenuation(int source, float referenceDistance, float rolloffFactor,
                                     float maxDistance) {
        PannerNode panner = source(source).panner;
        panner.setRefDistance(referenceDistance);
        panner.setRolloffFactor(rolloffFactor);
        panner.setMaxDistance(maxDistance);
    }

    @Override
    public float referenceDistance(int source) {
        return source(source).panner.getRefDistance();
    }

    @Override
    public float rolloffFactor(int source) {
        return source(source).panner.getRolloffFactor();
    }

    @Override
    public float maxDistance(int source) {
        return source(source).panner.getMaxDistance();
    }

    @Override
    public void setListenerGain(float gain) {
        master.getGain().setValue(gain);
    }

    @Override
    public float listenerGain() {
        return master.getGain().getValue();
    }

    @Override
    public void setListenerPosition(float x, float y, float z) {
        set(listenerPosition, x, y, z);
        context.getListener().setPosition(x, y, z);
        for (Source source : sources.values()) {
            if (source.relative) {
                source.place();
            }
        }
    }

    @Override
    public void setListenerVelocity(float x, float y, float z) {
        set(listenerVelocity, x, y, z);
    }

    @Override
    public void listenerPosition(float[] into) {
        copy(listenerPosition, into);
    }

    @Override
    public void listenerVelocity(float[] into) {
        copy(listenerVelocity, into);
    }

    @Override
    public void setListenerOrientation(float[] atThenUp) {
        if (atThenUp == null || atThenUp.length < 6) {
            throw new AudioException("orientation needs six floats: at then up");
        }
        System.arraycopy(atThenUp, 0, listenerOrientation, 0, 6);
        applyListenerOrientation();
    }

    @Override
    public void listenerOrientation(float[] into) {
        if (into == null || into.length < 6) {
            throw new AudioException("orientation needs six floats: at then up");
        }
        System.arraycopy(listenerOrientation, 0, into, 0, 6);
    }

    @Override
    public void shutdown() {
        for (Source source : sources.values()) {
            source.dispose();
        }
        sources.clear();
        buffers.clear();
        master.disconnect();
        context.close();
    }

    private void applyListenerOrientation() {
        float[] at = listenerOrientation;
        context.getListener().setOrientation(at[0], at[1], at[2], at[3], at[4], at[5]);
    }

    private Buffer buffer(int name) {
        Buffer buffer = buffers.get(name);
        if (buffer == null) {
            throw new AudioException("not a buffer: " + name);
        }
        return buffer;
    }

    private Source source(int name) {
        Source source = sources.get(name);
        if (source == null) {
            throw new AudioException("not a source: " + name);
        }
        return source;
    }

    private static void set(float[] vector, float x, float y, float z) {
        vector[0] = x;
        vector[1] = y;
        vector[2] = z;
    }

    private static void copy(float[] vector, float[] into) {
        if (into == null || into.length < 3) {
            throw new AudioException("a vector needs three floats");
        }
        System.arraycopy(vector, 0, into, 0, 3);
    }
}
