package org.lwjgl.openal;

import dev.flagon.lwjgl.internal.ALState;
import dev.flagon.lwjgl.internal.Buffers;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.AudioException;
import dev.flagon.platform.audio.AudioFormat;
import dev.flagon.platform.audio.SourceState;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * openal 1.0 over the platform mixer.
 *
 * <p>errors are latched rather than thrown, which is what openal does and what code polling
 * {@code alGetError()} expects. a call that names something that does not exist leaves
 * {@code AL_INVALID_NAME} behind and changes nothing.
 *
 * <p>parameters with no counterpart in the browser's mixer throw
 * {@link UnsupportedOperationException} naming the parameter, rather than being dropped: a
 * cone angle that silently does nothing is a sound that plays at the wrong volume forever.
 */
public final class AL10 {

    public static final int AL_NONE = 0;
    public static final int AL_FALSE = 0;
    public static final int AL_TRUE = 1;

    public static final int AL_SOURCE_RELATIVE = 0x202;
    public static final int AL_CONE_INNER_ANGLE = 0x1001;
    public static final int AL_CONE_OUTER_ANGLE = 0x1002;
    public static final int AL_PITCH = 0x1003;
    public static final int AL_POSITION = 0x1004;
    public static final int AL_DIRECTION = 0x1005;
    public static final int AL_VELOCITY = 0x1006;
    public static final int AL_LOOPING = 0x1007;
    public static final int AL_BUFFER = 0x1009;
    public static final int AL_GAIN = 0x100A;
    public static final int AL_MIN_GAIN = 0x100D;
    public static final int AL_MAX_GAIN = 0x100E;
    public static final int AL_ORIENTATION = 0x100F;

    public static final int AL_SOURCE_STATE = 0x1010;
    public static final int AL_INITIAL = 0x1011;
    public static final int AL_PLAYING = 0x1012;
    public static final int AL_PAUSED = 0x1013;
    public static final int AL_STOPPED = 0x1014;

    public static final int AL_BUFFERS_QUEUED = 0x1015;
    public static final int AL_BUFFERS_PROCESSED = 0x1016;

    public static final int AL_REFERENCE_DISTANCE = 0x1020;
    public static final int AL_ROLLOFF_FACTOR = 0x1021;
    public static final int AL_CONE_OUTER_GAIN = 0x1022;
    public static final int AL_MAX_DISTANCE = 0x1023;

    public static final int AL_FORMAT_MONO8 = 0x1100;
    public static final int AL_FORMAT_MONO16 = 0x1101;
    public static final int AL_FORMAT_STEREO8 = 0x1102;
    public static final int AL_FORMAT_STEREO16 = 0x1103;

    public static final int AL_FREQUENCY = 0x2001;
    public static final int AL_BITS = 0x2002;
    public static final int AL_CHANNELS = 0x2003;
    public static final int AL_SIZE = 0x2004;

    public static final int AL_NO_ERROR = ALState.NO_ERROR;
    public static final int AL_INVALID_NAME = ALState.INVALID_NAME;
    public static final int AL_INVALID_ENUM = ALState.INVALID_ENUM;
    public static final int AL_INVALID_VALUE = ALState.INVALID_VALUE;
    public static final int AL_INVALID_OPERATION = ALState.INVALID_OPERATION;
    public static final int AL_OUT_OF_MEMORY = 0xA005;

    public static final int AL_VENDOR = 0xB001;
    public static final int AL_VERSION = 0xB002;
    public static final int AL_RENDERER = 0xB003;
    public static final int AL_EXTENSIONS = 0xB004;

    public static final int AL_DOPPLER_FACTOR = 0xC000;
    public static final int AL_DOPPLER_VELOCITY = 0xC001;

    public static final int AL_DISTANCE_MODEL = 0xD000;
    public static final int AL_INVERSE_DISTANCE = ALState.INVERSE_DISTANCE;
    public static final int AL_INVERSE_DISTANCE_CLAMPED = ALState.INVERSE_DISTANCE_CLAMPED;

    private AL10() {
    }

    private static ALState state() {
        return ALState.get();
    }

    private static AudioDevice device() {
        return ALState.get().device();
    }

    public static int alGetError() {
        return state().takeError();
    }

    public static String alGetString(int parameter) {
        switch (parameter) {
            case AL_VENDOR:
                return "Flagon";
            case AL_VERSION:
                return "1.1 ALSOFT-compatible";
            case AL_RENDERER:
                return device().name();
            case AL_EXTENSIONS:
                return "";
            default:
                state().fail(AL_INVALID_ENUM);
                return null;
        }
    }

    // buffers

    public static int alGenBuffers() {
        return device().createBuffer();
    }

    public static void alGenBuffers(IntBuffer buffers) {
        AudioDevice device = device();
        for (int at = buffers.position(); at < buffers.limit(); at++) {
            buffers.put(at, device.createBuffer());
        }
    }

    public static void alDeleteBuffers(int buffer) {
        if (!device().isBuffer(buffer)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        device().deleteBuffer(buffer);
    }

    public static void alDeleteBuffers(IntBuffer buffers) {
        for (int at = buffers.position(); at < buffers.limit(); at++) {
            alDeleteBuffers(buffers.get(at));
        }
    }

    public static boolean alIsBuffer(int buffer) {
        return device().isBuffer(buffer);
    }

    /** the data is raw pcm; openal never took anything else, and neither does this. */
    public static void alBufferData(int buffer, int format, ByteBuffer data, int frequency) {
        if (!device().isBuffer(buffer)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        AudioFormat pcm = format(format);
        if (pcm == null) {
            state().fail(AL_INVALID_ENUM);
            return;
        }
        byte[] bytes = Buffers.readRemaining(data);
        try {
            device().bufferData(buffer, pcm, bytes, bytes.length, frequency);
        } catch (AudioException failed) {
            state().fail(AL_INVALID_VALUE);
        }
    }

    public static int alGetBufferi(int buffer, int parameter) {
        AudioDevice device = device();
        if (!device.isBuffer(buffer)) {
            state().fail(AL_INVALID_NAME);
            return 0;
        }
        switch (parameter) {
            case AL_FREQUENCY:
                return device.bufferSampleRate(buffer);
            case AL_BITS:
                return device.bufferFormat(buffer).bitsPerSample();
            case AL_CHANNELS:
                return device.bufferFormat(buffer).channels();
            case AL_SIZE:
                return device.bufferSizeInBytes(buffer);
            default:
                state().fail(AL_INVALID_ENUM);
                return 0;
        }
    }

    // sources

    public static int alGenSources() {
        return device().createSource();
    }

    public static void alGenSources(IntBuffer sources) {
        AudioDevice device = device();
        for (int at = sources.position(); at < sources.limit(); at++) {
            sources.put(at, device.createSource());
        }
    }

    public static void alDeleteSources(int source) {
        if (!device().isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        device().deleteSource(source);
        state().forgetSource(source);
    }

    public static void alDeleteSources(IntBuffer sources) {
        for (int at = sources.position(); at < sources.limit(); at++) {
            alDeleteSources(sources.get(at));
        }
    }

    public static boolean alIsSource(int source) {
        return device().isSource(source);
    }

    public static void alSourcei(int source, int parameter, int value) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        switch (parameter) {
            case AL_BUFFER:
                if (value != AudioDevice.NO_BUFFER && !device.isBuffer(value)) {
                    state().fail(AL_INVALID_NAME);
                    return;
                }
                device.attachBuffer(source, value);
                return;
            case AL_LOOPING:
                device.setLooping(source, value != AL_FALSE);
                return;
            case AL_SOURCE_RELATIVE:
                device.setSourceRelative(source, value != AL_FALSE);
                return;
            default:
                unsupportedSourceParameter("alSourcei", parameter);
        }
    }

    public static void alSourcef(int source, int parameter, float value) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        switch (parameter) {
            case AL_PITCH:
                if (value <= 0.0f) {
                    state().fail(AL_INVALID_VALUE);
                    return;
                }
                device.setPitch(source, value);
                return;
            case AL_GAIN:
                if (value < 0.0f) {
                    state().fail(AL_INVALID_VALUE);
                    return;
                }
                device.setGain(source, value);
                return;
            case AL_REFERENCE_DISTANCE:
                state().setReferenceDistance(source, value);
                return;
            case AL_ROLLOFF_FACTOR:
                state().setRolloffFactor(source, value);
                return;
            case AL_MAX_DISTANCE:
                state().setMaxDistance(source, value);
                return;
            default:
                unsupportedSourceParameter("alSourcef", parameter);
        }
    }

    public static void alSource3f(int source, int parameter, float x, float y, float z) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        switch (parameter) {
            case AL_POSITION:
                device.setSourcePosition(source, x, y, z);
                return;
            case AL_VELOCITY:
                device.setSourceVelocity(source, x, y, z);
                state().warnAboutDoppler();
                return;
            default:
                unsupportedSourceParameter("alSource3f", parameter);
        }
    }

    public static int alGetSourcei(int source, int parameter) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return 0;
        }
        switch (parameter) {
            case AL_SOURCE_STATE:
                return sourceState(device.sourceState(source));
            case AL_BUFFER:
                return device.attachedBuffer(source);
            case AL_LOOPING:
                return device.isLooping(source) ? AL_TRUE : AL_FALSE;
            case AL_SOURCE_RELATIVE:
                return device.isSourceRelative(source) ? AL_TRUE : AL_FALSE;
            case AL_BUFFERS_QUEUED:
            case AL_BUFFERS_PROCESSED:
                throw streamingUnsupported("alGetSourcei");
            default:
                unsupportedSourceParameter("alGetSourcei", parameter);
                return 0;
        }
    }

    public static float alGetSourcef(int source, int parameter) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return 0.0f;
        }
        switch (parameter) {
            case AL_PITCH:
                return device.pitch(source);
            case AL_GAIN:
                return device.gain(source);
            case AL_REFERENCE_DISTANCE:
                return state().referenceDistance(source);
            case AL_ROLLOFF_FACTOR:
                return state().rolloffFactor(source);
            case AL_MAX_DISTANCE:
                return state().maxDistance(source);
            default:
                unsupportedSourceParameter("alGetSourcef", parameter);
                return 0.0f;
        }
    }

    public static void alGetSource(int source, int parameter, FloatBuffer values) {
        AudioDevice device = device();
        if (!device.isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        float[] vector = new float[3];
        switch (parameter) {
            case AL_POSITION:
                device.sourcePosition(source, vector);
                break;
            case AL_VELOCITY:
                device.sourceVelocity(source, vector);
                break;
            default:
                unsupportedSourceParameter("alGetSource", parameter);
                return;
        }
        write(values, vector);
    }

    public static void alSourcePlay(int source) {
        if (!device().isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        try {
            device().play(source);
        } catch (AudioException noBuffer) {
            state().fail(AL_INVALID_OPERATION);
        }
    }

    public static void alSourcePause(int source) {
        if (!device().isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        device().pause(source);
    }

    public static void alSourceStop(int source) {
        if (!device().isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        device().stop(source);
    }

    public static void alSourceRewind(int source) {
        if (!device().isSource(source)) {
            state().fail(AL_INVALID_NAME);
            return;
        }
        device().rewind(source);
    }

    public static void alSourcePlay(IntBuffer sources) {
        each(sources, AL10::alSourcePlay);
    }

    public static void alSourcePause(IntBuffer sources) {
        each(sources, AL10::alSourcePause);
    }

    public static void alSourceStop(IntBuffer sources) {
        each(sources, AL10::alSourceStop);
    }

    public static void alSourceRewind(IntBuffer sources) {
        each(sources, AL10::alSourceRewind);
    }

    public static void alSourceQueueBuffers(int source, IntBuffer buffers) {
        throw streamingUnsupported("alSourceQueueBuffers");
    }

    public static void alSourceQueueBuffers(int source, int buffer) {
        throw streamingUnsupported("alSourceQueueBuffers");
    }

    public static void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        throw streamingUnsupported("alSourceUnqueueBuffers");
    }

    public static int alSourceUnqueueBuffers(int source) {
        throw streamingUnsupported("alSourceUnqueueBuffers");
    }

    // listener

    public static void alListenerf(int parameter, float value) {
        if (parameter == AL_GAIN) {
            device().setListenerGain(value);
            return;
        }
        state().fail(AL_INVALID_ENUM);
    }

    public static void alListener3f(int parameter, float x, float y, float z) {
        switch (parameter) {
            case AL_POSITION:
                device().setListenerPosition(x, y, z);
                return;
            case AL_VELOCITY:
                device().setListenerVelocity(x, y, z);
                state().warnAboutDoppler();
                return;
            default:
                state().fail(AL_INVALID_ENUM);
        }
    }

    public static void alListener(int parameter, FloatBuffer values) {
        switch (parameter) {
            case AL_ORIENTATION: {
                float[] orientation = read(values, 6);
                if (orientation == null) {
                    return;
                }
                device().setListenerOrientation(orientation);
                return;
            }
            case AL_POSITION: {
                float[] position = read(values, 3);
                if (position == null) {
                    return;
                }
                device().setListenerPosition(position[0], position[1], position[2]);
                return;
            }
            case AL_VELOCITY: {
                float[] velocity = read(values, 3);
                if (velocity == null) {
                    return;
                }
                device().setListenerVelocity(velocity[0], velocity[1], velocity[2]);
                state().warnAboutDoppler();
                return;
            }
            default:
                state().fail(AL_INVALID_ENUM);
        }
    }

    public static float alGetListenerf(int parameter) {
        if (parameter == AL_GAIN) {
            return device().listenerGain();
        }
        state().fail(AL_INVALID_ENUM);
        return 0.0f;
    }

    public static void alGetListener(int parameter, FloatBuffer values) {
        switch (parameter) {
            case AL_POSITION: {
                float[] position = new float[3];
                device().listenerPosition(position);
                write(values, position);
                return;
            }
            case AL_VELOCITY: {
                float[] velocity = new float[3];
                device().listenerVelocity(velocity);
                write(values, velocity);
                return;
            }
            case AL_ORIENTATION: {
                float[] orientation = new float[6];
                device().listenerOrientation(orientation);
                write(values, orientation);
                return;
            }
            default:
                state().fail(AL_INVALID_ENUM);
        }
    }

    // global state

    public static void alDistanceModel(int model) {
        state().setDistanceModel(model);
    }

    public static int alGetInteger(int parameter) {
        if (parameter == AL_DISTANCE_MODEL) {
            return state().distanceModel();
        }
        state().fail(AL_INVALID_ENUM);
        return 0;
    }

    /**
     * doppler shift is not emulated, so the only factor that means what it says is zero — which
     * is what a caller switching it off is asking for.
     */
    public static void alDopplerFactor(float factor) {
        if (factor != 0.0f) {
            state().warnAboutDoppler();
        }
    }

    public static void alDopplerVelocity(float velocity) {
        state().warnAboutDoppler();
    }

    private static int sourceState(SourceState state) {
        switch (state) {
            case PLAYING:
                return AL_PLAYING;
            case PAUSED:
                return AL_PAUSED;
            case STOPPED:
                return AL_STOPPED;
            default:
                return AL_INITIAL;
        }
    }

    private static AudioFormat format(int format) {
        switch (format) {
            case AL_FORMAT_MONO8:
                return AudioFormat.MONO_8;
            case AL_FORMAT_MONO16:
                return AudioFormat.MONO_16;
            case AL_FORMAT_STEREO8:
                return AudioFormat.STEREO_8;
            case AL_FORMAT_STEREO16:
                return AudioFormat.STEREO_16;
            default:
                return null;
        }
    }

    private interface SourceCall {
        void call(int source);
    }

    private static void each(IntBuffer sources, SourceCall call) {
        for (int at = sources.position(); at < sources.limit(); at++) {
            call.call(sources.get(at));
        }
    }

    /** @return null once the error is latched, so callers stop rather than read past the end */
    private static float[] read(FloatBuffer values, int count) {
        if (values.remaining() < count) {
            state().fail(AL_INVALID_VALUE);
            return null;
        }
        float[] read = new float[count];
        for (int at = 0; at < count; at++) {
            read[at] = values.get(values.position() + at);
        }
        return read;
    }

    private static void write(FloatBuffer values, float[] vector) {
        if (values.remaining() < vector.length) {
            state().fail(AL_INVALID_VALUE);
            return;
        }
        for (int at = 0; at < vector.length; at++) {
            values.put(values.position() + at, vector[at]);
        }
    }

    private static void unsupportedSourceParameter(String call, int parameter) {
        switch (parameter) {
            case AL_MIN_GAIN:
            case AL_MAX_GAIN:
            case AL_CONE_INNER_ANGLE:
            case AL_CONE_OUTER_ANGLE:
            case AL_CONE_OUTER_GAIN:
            case AL_DIRECTION:
                throw new UnsupportedOperationException(call + " with parameter 0x"
                        + Integer.toHexString(parameter) + " is not emulated; web audio's panner "
                        + "has no equivalent, and a source at the wrong volume is worse than a "
                        + "stack trace");
            default:
                state().fail(AL_INVALID_ENUM);
        }
    }

    private static UnsupportedOperationException streamingUnsupported(String call) {
        return new UnsupportedOperationException(call + " is not emulated; streaming a queue of "
                + "buffers needs a mixer that reports what it has consumed, and web audio plays a "
                + "buffer whole. attach one buffer per source with alSourcei(AL_BUFFER)");
    }
}
