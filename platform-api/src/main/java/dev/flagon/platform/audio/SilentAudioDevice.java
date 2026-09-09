package dev.flagon.platform.audio;

import dev.flagon.platform.Clock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * an {@link AudioDevice} that keeps every book but makes no sound, for hosts without output.
 *
 * <p>playback is still timed off the clock, so a source that has played its buffer out reports
 * itself stopped. code that waits for that is the reason this is not a set of empty methods.
 */
public final class SilentAudioDevice implements AudioDevice {

    private static final class Buffer {
        AudioFormat format = AudioFormat.MONO_16;
        int sampleRate = 44_100;
        int sizeInBytes;
    }

    private static final class Source {
        int buffer = NO_BUFFER;
        final List<Integer> queue = new ArrayList<>();
        int processed;
        SourceState state = SourceState.INITIAL;
        boolean looping;
        boolean relative;
        float gain = 1.0f;
        float pitch = 1.0f;
        float referenceDistance = 1.0f;
        float rolloffFactor = 1.0f;
        float maxDistance = Float.MAX_VALUE;
        final float[] position = new float[3];
        final float[] velocity = new float[3];
        /** seconds into the buffer at the moment playback last started, or where it was paused. */
        double offset;
        long startedAt;
    }

    private final Clock clock;
    private final Map<Integer, Buffer> buffers = new HashMap<>();
    private final Map<Integer, Source> sources = new HashMap<>();
    private final float[] listenerPosition = new float[3];
    private final float[] listenerVelocity = new float[3];
    private final float[] listenerOrientation = {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f};
    private DistanceModel distanceModel = DistanceModel.INVERSE_CLAMPED;
    private float dopplerFactor = Doppler.DEFAULT_FACTOR;
    private float speedOfSound = Doppler.DEFAULT_SPEED_OF_SOUND;
    private float listenerGain = 1.0f;
    private int nextName = 1;

    public SilentAudioDevice(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
    }

    @Override
    public String name() {
        return "silent";
    }

    @Override
    public boolean isOutputLive() {
        return false;
    }

    @Override
    public int createBuffer() {
        int name = nextName++;
        buffers.put(name, new Buffer());
        return name;
    }

    @Override
    public void bufferData(int buffer, AudioFormat format, byte[] data, int length, int sampleRate) {
        Buffer held = buffer(buffer);
        Audio.requireWholeFrames(format, length, sampleRate);
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
        sources.remove(source);
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
        if (!held.queue.isEmpty()) {
            throw new AudioException("source " + source + " is streaming a queue");
        }
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
            if (!isBuffer(buffer)) {
                throw new AudioException("not a buffer: " + buffer);
            }
        }
        for (int buffer : buffers) {
            held.queue.add(buffer);
        }
    }

    @Override
    public int[] unqueueBuffers(int source, int count) {
        Source held = source(source);
        settle(held);
        if (count < 0 || count > held.processed) {
            throw new AudioException("source " + source + " has processed " + held.processed
                    + " buffers, not " + count);
        }
        int[] taken = new int[count];
        double played = 0.0;
        for (int at = 0; at < count; at++) {
            taken[at] = held.queue.remove(0);
            played += durationOf(taken[at]);
        }
        held.offset = Math.max(0.0, elapsed(held) - played);
        held.startedAt = clock.currentTimeMillis();
        held.processed -= count;
        return taken;
    }

    @Override
    public int buffersQueued(int source) {
        return source(source).queue.size();
    }

    @Override
    public int buffersProcessed(int source) {
        Source held = source(source);
        settle(held);
        return held.processed;
    }

    @Override
    public void play(int source) {
        Source held = source(source);
        if (held.buffer == NO_BUFFER && held.queue.isEmpty()) {
            throw new AudioException("source " + source + " has no buffer to play");
        }
        if (held.state != SourceState.PAUSED) {
            held.offset = 0.0;
        }
        held.startedAt = clock.currentTimeMillis();
        held.state = SourceState.PLAYING;
    }

    @Override
    public void pause(int source) {
        Source held = source(source);
        if (settle(held) != SourceState.PLAYING) {
            return;
        }
        held.offset = elapsed(held);
        held.state = SourceState.PAUSED;
    }

    @Override
    public void stop(int source) {
        Source held = source(source);
        held.offset = 0.0;
        held.processed = held.queue.size();
        held.state = SourceState.STOPPED;
    }

    @Override
    public void rewind(int source) {
        Source held = source(source);
        held.offset = 0.0;
        held.processed = 0;
        held.state = SourceState.INITIAL;
    }

    @Override
    public SourceState sourceState(int source) {
        return settle(source(source));
    }

    @Override
    public void setLooping(int source, boolean looping) {
        source(source).looping = looping;
    }

    @Override
    public boolean isLooping(int source) {
        return source(source).looping;
    }

    @Override
    public void setGain(int source, float gain) {
        source(source).gain = gain;
    }

    @Override
    public float gain(int source) {
        return source(source).gain;
    }

    @Override
    public void setPitch(int source, float pitch) {
        if (pitch <= 0.0f) {
            throw new AudioException("pitch must be positive: " + pitch);
        }
        Source held = source(source);
        // the elapsed time so far was played at the old rate, so bank it before the rate changes
        if (settle(held) == SourceState.PLAYING) {
            held.offset = elapsed(held);
            held.startedAt = clock.currentTimeMillis();
        }
        held.pitch = pitch;
    }

    @Override
    public float pitch(int source) {
        return source(source).pitch;
    }

    @Override
    public void setSourceRelative(int source, boolean relative) {
        source(source).relative = relative;
    }

    @Override
    public boolean isSourceRelative(int source) {
        return source(source).relative;
    }

    @Override
    public void setSourcePosition(int source, float x, float y, float z) {
        set(source(source).position, x, y, z);
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
    public void setDistanceModel(DistanceModel model) {
        distanceModel = model;
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
    }

    @Override
    public float speedOfSound() {
        return speedOfSound;
    }

    @Override
    public float dopplerShift(int source) {
        Source held = source(source);
        return Doppler.shift(held.position, held.velocity, listenerPosition, listenerVelocity,
                dopplerFactor, speedOfSound);
    }

    @Override
    public void setSourceAttenuation(int source, float referenceDistance, float rolloffFactor,
                                     float maxDistance) {
        Source held = source(source);
        held.referenceDistance = referenceDistance;
        held.rolloffFactor = rolloffFactor;
        held.maxDistance = maxDistance;
    }

    @Override
    public float referenceDistance(int source) {
        return source(source).referenceDistance;
    }

    @Override
    public float rolloffFactor(int source) {
        return source(source).rolloffFactor;
    }

    @Override
    public float maxDistance(int source) {
        return source(source).maxDistance;
    }

    @Override
    public void setListenerGain(float gain) {
        listenerGain = gain;
    }

    @Override
    public float listenerGain() {
        return listenerGain;
    }

    @Override
    public void setListenerPosition(float x, float y, float z) {
        set(listenerPosition, x, y, z);
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
        buffers.clear();
        sources.clear();
    }

    /** @return the state after however much of the buffer has played by now */
    private SourceState settle(Source source) {
        if (source.state != SourceState.PLAYING) {
            return source.state;
        }
        double played = elapsed(source);
        if (source.queue.isEmpty()) {
            if (!source.looping && played >= duration(source)) {
                source.state = SourceState.STOPPED;
                source.offset = 0.0;
            }
            return source.state;
        }
        int done = 0;
        double taken = 0.0;
        for (int name : source.queue) {
            double length = durationOf(name);
            if (played < taken + length) {
                break;
            }
            taken += length;
            done++;
        }
        source.processed = done;
        if (done == source.queue.size() && !source.looping) {
            source.state = SourceState.STOPPED;
        }
        return source.state;
    }

    private double elapsed(Source source) {
        double seconds = (clock.currentTimeMillis() - source.startedAt) / 1000.0;
        return source.offset + seconds * source.pitch;
    }

    private double duration(Source source) {
        return durationOf(source.buffer);
    }

    private double durationOf(int name) {
        Buffer buffer = buffers.get(name);
        if (buffer == null) {
            return 0.0;
        }
        return Audio.durationInSeconds(buffer.format, buffer.sizeInBytes, buffer.sampleRate);
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
