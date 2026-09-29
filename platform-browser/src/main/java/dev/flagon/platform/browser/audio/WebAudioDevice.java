package dev.flagon.platform.browser.audio;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Names;
import dev.flagon.platform.audio.Audio;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.DistanceModel;
import dev.flagon.platform.audio.Doppler;
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

import java.util.ArrayList;
import java.util.List;

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
        final List<Integer> queue = new ArrayList<>();
        final List<AudioBufferSourceNode> playing = new ArrayList<>();
        int processed;
        double tail;
        int buffer = NO_BUFFER;
        SourceState state = SourceState.INITIAL;
        boolean looping;
        boolean relative;
        float pitch = 1.0f;
        float rate = 1.0f;
        float rolloff = 1.0f;
        final float[] position = new float[3];
        final float[] velocity = new float[3];
        private final float[] world = new float[3];

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
            world(world);
            panner.setPosition(world[0], world[1], world[2]);
            rate();
        }

        void world(float[] into) {
            into[0] = position[0];
            into[1] = position[1];
            into[2] = position[2];
            if (relative) {
                into[0] += listenerPosition[0];
                into[1] += listenerPosition[1];
                into[2] += listenerPosition[2];
            }
        }

        float shift() {
            world(world);
            return Doppler.shift(world, velocity, listenerPosition, listenerVelocity,
                    dopplerFactor, speedOfSound);
        }

        /** the rate the graph runs at: the source pitch, shifted by whatever is moving. */
        void rate() {
            float wanted = pitch * shift();
            if (wanted == rate) {
                return;
            }
            // what has played so far played at the old rate, so bank it before the rate changes
            if (state == SourceState.PLAYING) {
                offset = elapsed();
                startedAt = context.getCurrentTime();
            }
            rate = wanted;
            if (node != null) {
                node.getPlaybackRate().setValue(rate);
            }
            for (AudioBufferSourceNode scheduled : playing) {
                scheduled.getPlaybackRate().setValue(rate);
            }
        }

        double elapsed() {
            if (state != SourceState.PLAYING) {
                return offset;
            }
            return offset + (context.getCurrentTime() - startedAt) * rate;
        }

        void release() {
            generation++;
            if (node != null) {
                node.stop();
                node.disconnect();
                node = null;
            }
            for (AudioBufferSourceNode scheduled : playing) {
                scheduled.stop();
                scheduled.disconnect();
            }
            playing.clear();
        }

        void dispose() {
            release();
            gain.disconnect();
            panner.disconnect();
        }
    }

    private final AudioContext context;
    private final GainNode master;
    // al names buffers and sources from one pool, so one table holds both
    private final Names<Object> objects = new Names<>();
    private DistanceModel distanceModel = DistanceModel.INVERSE_CLAMPED;
    private float dopplerFactor = Doppler.DEFAULT_FACTOR;
    private float speedOfSound = Doppler.DEFAULT_SPEED_OF_SOUND;
    private final float[] listenerPosition = new float[3];
    private final float[] listenerVelocity = new float[3];
    private final float[] listenerOrientation = {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f};

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
        return objects.add(new Buffer());
    }

    @Override
    public void bufferData(int buffer, AudioFormat format, byte[] data, int length,
            int sampleRate) {
        Audio.requireWholeFrames(format, length, sampleRate);
        Buffer held = buffer(buffer);
        int frames = Audio.frames(format, length);
        AudioBuffer audio = context.createBuffer(format.channels(), Math.max(frames, 1),
                sampleRate);
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
        if (isBuffer(buffer)) {
            objects.remove(buffer);
        }
    }

    @Override
    public boolean isBuffer(int buffer) {
        return objects.get(buffer) instanceof Buffer;
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
        return objects.add(new Source());
    }

    @Override
    public void deleteSource(int source) {
        if (objects.get(source) instanceof Source held) {
            objects.remove(source);
            held.dispose();
        }
    }

    @Override
    public boolean isSource(int source) {
        return objects.get(source) instanceof Source;
    }

    @Override
    public void attachBuffer(int source, int buffer) {
        if (buffer != NO_BUFFER && !isBuffer(buffer)) {
            throw new AudioException("not a buffer: " + buffer);
        }
        Source held = source(source);
        if (!held.queue.isEmpty()) {
            throw new AudioException("source " + source + " is streaming a queue");
        }
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
    public void queueBuffers(int source, int[] buffers) {
        Source held = source(source);
        if (held.buffer != NO_BUFFER) {
            throw new AudioException("source " + source + " holds a single buffer");
        }
        for (int buffer : buffers) {
            audioOf(buffer);
        }
        for (int buffer : buffers) {
            held.queue.add(buffer);
            if (held.state == SourceState.PLAYING) {
                schedule(held, buffer);
            }
        }
    }

    @Override
    public int[] unqueueBuffers(int source, int count) {
        Source held = source(source);
        if (count < 0 || count > held.processed) {
            throw new AudioException("source " + source + " has processed " + held.processed
                    + " buffers, not " + count);
        }
        int[] taken = new int[count];
        for (int at = 0; at < count; at++) {
            taken[at] = held.queue.remove(0);
            if (!held.playing.isEmpty()) {
                held.playing.remove(0);
            }
        }
        held.processed -= count;
        return taken;
    }

    @Override
    public int buffersQueued(int source) {
        return source(source).queue.size();
    }

    @Override
    public int buffersProcessed(int source) {
        return source(source).processed;
    }

    // every queued buffer is scheduled back to back up front, the graph keeping the time rather
    // than a callback arriving late and leaving a gap
    private void schedule(Source held, int buffer) {
        AudioBuffer audio = audioOf(buffer);
        AudioBufferSourceNode node = context.createBufferSource();
        node.setBuffer(audio);
        node.getPlaybackRate().setValue(held.rate);
        node.connect(held.gain);

        int generation = held.generation;
        node.setOnEnded(event -> {
            if (held.generation != generation) {
                return;
            }
            held.processed++;
            if (held.processed >= held.queue.size()) {
                held.state = SourceState.STOPPED;
            }
        });

        node.start(held.tail);
        held.playing.add(node);
        held.tail += audio.getDuration() / held.rate;
    }

    private void playQueue(Source held) {
        int from = held.state == SourceState.PAUSED ? held.processed : 0;
        held.release();
        held.processed = from;
        held.tail = context.getCurrentTime();
        held.state = SourceState.PLAYING;
        for (int at = from; at < held.queue.size(); at++) {
            schedule(held, held.queue.get(at));
        }
    }

    @Override
    public void play(int source) {
        Source held = source(source);
        if (!held.queue.isEmpty()) {
            playQueue(held);
            return;
        }
        if (!(objects.get(held.buffer) instanceof Buffer buffer) || buffer.audio == null) {
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
        node.getPlaybackRate().setValue(held.rate);
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
        held.processed = held.queue.size();
        held.state = SourceState.STOPPED;
    }

    @Override
    public void rewind(int source) {
        Source held = source(source);
        held.release();
        held.offset = 0.0;
        held.processed = 0;
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
        held.rate();
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
        Source held = source(source);
        set(held.velocity, x, y, z);
        held.rate();
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
    public void setDistanceModel(DistanceModel model) {
        distanceModel = model;
        for (int name = 1; name < objects.size(); name++) {
            if (objects.get(name) instanceof Source) {
                applyAttenuation(name);
            }
        }
    }

    @Override
    public DistanceModel distanceModel() {
        return distanceModel;
    }

    @Override
    public void setDopplerFactor(float factor) {
        if (factor < 0.0f) {
            throw new AudioException("doppler factor must not be negative: " + factor);
        }
        dopplerFactor = factor;
        for (Object held : objects) {
            if (held instanceof Source source) {
                source.rate();
            }
        }
    }

    @Override
    public float dopplerFactor() {
        return dopplerFactor;
    }

    @Override
    public void setSpeedOfSound(float speed) {
        if (speed <= 0.0f) {
            throw new AudioException("speed of sound must be positive: " + speed);
        }
        speedOfSound = speed;
        for (Object held : objects) {
            if (held instanceof Source source) {
                source.rate();
            }
        }
    }

    @Override
    public float speedOfSound() {
        return speedOfSound;
    }

    @Override
    public float dopplerShift(int source) {
        return source(source).shift();
    }

    @Override
    public void setSourceAttenuation(int source, float referenceDistance, float rolloffFactor,
                                     float maxDistance) {
        Source held = source(source);
        held.rolloff = rolloffFactor;
        held.panner.setRefDistance(referenceDistance);
        held.panner.setMaxDistance(maxDistance);
        applyAttenuation(source);
    }

    // the panner has the three curves but no way to switch off; not at all is a rolloff of zero
    private void applyAttenuation(int source) {
        Source held = source(source);
        held.panner.setDistanceModel(curveOf(distanceModel));
        held.panner.setRolloffFactor(
                distanceModel == DistanceModel.NONE ? 0.0f : held.rolloff);
    }

    private static String curveOf(DistanceModel model) {
        return switch (model) {
            case LINEAR, LINEAR_CLAMPED -> PannerNode.DISTANCE_MODEL_LINEAR;
            case EXPONENT, EXPONENT_CLAMPED -> PannerNode.DISTANCE_MODEL_EXPONENTIAL;
            default -> PannerNode.DISTANCE_MODEL_INVERSE;
        };
    }

    @Override
    public float referenceDistance(int source) {
        return source(source).panner.getRefDistance();
    }

    @Override
    public float rolloffFactor(int source) {
        return source(source).rolloff;
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
        // every source moved relative to the listener, which is what the shift is measured along
        for (Object held : objects) {
            if (held instanceof Source source) {
                if (source.relative) {
                    source.place();
                } else {
                    source.rate();
                }
            }
        }
    }

    @Override
    public void setListenerVelocity(float x, float y, float z) {
        set(listenerVelocity, x, y, z);
        for (Object held : objects) {
            if (held instanceof Source source) {
                source.rate();
            }
        }
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
        for (Object held : objects) {
            if (held instanceof Source source) {
                source.dispose();
            }
        }
        objects.clear();
        master.disconnect();
        context.close();
    }

    private void applyListenerOrientation() {
        float[] at = listenerOrientation;
        context.getListener().setOrientation(at[0], at[1], at[2], at[3], at[4], at[5]);
    }

    private AudioBuffer audioOf(int name) {
        Buffer held = buffer(name);
        if (held.audio == null) {
            throw new AudioException("buffer " + name + " holds no pcm");
        }
        return held.audio;
    }

    private Buffer buffer(int name) {
        if (objects.get(name) instanceof Buffer buffer) {
            return buffer;
        }
        throw new AudioException("not a buffer: " + name);
    }

    private Source source(int name) {
        if (objects.get(name) instanceof Source source) {
            return source;
        }
        throw new AudioException("not a source: " + name);
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
