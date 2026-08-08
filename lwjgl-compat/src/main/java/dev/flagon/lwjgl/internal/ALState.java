package dev.flagon.lwjgl.internal;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.DistanceModel;

/**
 * the context state openal keeps internally, and the error it hands back one call later.
 *
 * <p>a singleton because openal's api is one; {@link #attach} is called by {@code AL.create()}
 * and {@link #detach} by {@code AL.destroy()}.
 */
public final class ALState {

    public static final int NO_ERROR = 0;
    public static final int INVALID_NAME = 0xA001;
    public static final int INVALID_ENUM = 0xA002;
    public static final int INVALID_VALUE = 0xA003;
    public static final int INVALID_OPERATION = 0xA004;

    public static final int DISTANCE_NONE = 0;
    public static final int INVERSE_DISTANCE = 0xD001;
    public static final int INVERSE_DISTANCE_CLAMPED = 0xD002;

    private static ALState current;

    private final AudioDevice device;
    private final ConsoleSink console;
    private int error = NO_ERROR;
    private boolean warnedAboutDoppler;

    private ALState(AudioDevice device, ConsoleSink console) {
        this.device = device;
        this.console = console;
    }

    public static void attach(AudioDevice device, ConsoleSink console) {
        if (device == null) {
            throw new IllegalArgumentException("device must not be null");
        }
        current = new ALState(device, console);
    }

    public static void detach() {
        if (current != null) {
            current.device.shutdown();
            current = null;
        }
    }

    public static boolean isAttached() {
        return current != null;
    }

    /** @throws IllegalStateException if {@code AL.create()} has not been called */
    public static ALState get() {
        if (current == null) {
            throw new IllegalStateException(
                    "no openal context; call AL.create() before any al* function");
        }
        return current;
    }

    public AudioDevice device() {
        return device;
    }

    /**
     * latches an error the way openal does: the first one is kept until it is read, because a
     * caller that checks once after a run of calls expects to hear about the first failure.
     */
    public void fail(int code) {
        if (error == NO_ERROR) {
            error = code;
        }
    }

    public int takeError() {
        int taken = error;
        error = NO_ERROR;
        return taken;
    }

    public int distanceModel() {
        return switch (device.distanceModel()) {
            case NONE -> DISTANCE_NONE;
            case INVERSE -> INVERSE_DISTANCE;
            case INVERSE_CLAMPED -> INVERSE_DISTANCE_CLAMPED;
        };
    }

    /** @throws UnsupportedOperationException for the models web audio's panner cannot express */
    public void setDistanceModel(int model) {
        if (model != DISTANCE_NONE && model != INVERSE_DISTANCE
                && model != INVERSE_DISTANCE_CLAMPED) {
            throw new UnsupportedOperationException(
                    "alDistanceModel(" + Integer.toHexString(model) + ") is not emulated; web "
                    + "audio's panner attenuates inversely, so AL_NONE, AL_INVERSE_DISTANCE and "
                    + "AL_INVERSE_DISTANCE_CLAMPED are the models available");
        }
        device.setDistanceModel(switch (model) {
            case DISTANCE_NONE -> DistanceModel.NONE;
            case INVERSE_DISTANCE -> DistanceModel.INVERSE;
            default -> DistanceModel.INVERSE_CLAMPED;
        });
    }

    public void setReferenceDistance(int source, float value) {
        device.setSourceAttenuation(source, value, device.rolloffFactor(source),
                device.maxDistance(source));
    }

    public void setRolloffFactor(int source, float value) {
        device.setSourceAttenuation(source, device.referenceDistance(source), value,
                device.maxDistance(source));
    }

    public void setMaxDistance(int source, float value) {
        device.setSourceAttenuation(source, device.referenceDistance(source),
                device.rolloffFactor(source), value);
    }

    public float referenceDistance(int source) {
        return device.referenceDistance(source);
    }

    public float rolloffFactor(int source) {
        return device.rolloffFactor(source);
    }

    public float maxDistance(int source) {
        return device.maxDistance(source);
    }

    /**
     * doppler shift left openal's successor: web audio removed velocity from its panner and
     * listener. velocities are still recorded, so a caller reading one back sees what it set.
     */
    public void warnAboutDoppler() {
        if (warnedAboutDoppler || console == null) {
            return;
        }
        warnedAboutDoppler = true;
        console.warn("velocities are recorded but produce no doppler shift; web audio dropped it");
    }

}
