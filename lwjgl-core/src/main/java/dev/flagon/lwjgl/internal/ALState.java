package dev.flagon.lwjgl.internal;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.DistanceModel;

/**
 openal context
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
    public static final int LINEAR_DISTANCE = 0xD003;
    public static final int LINEAR_DISTANCE_CLAMPED = 0xD004;
    public static final int EXPONENT_DISTANCE = 0xD005;
    public static final int EXPONENT_DISTANCE_CLAMPED = 0xD006;

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
            case LINEAR -> LINEAR_DISTANCE;
            case LINEAR_CLAMPED -> LINEAR_DISTANCE_CLAMPED;
            case EXPONENT -> EXPONENT_DISTANCE;
            case EXPONENT_CLAMPED -> EXPONENT_DISTANCE_CLAMPED;
        };
    }

    public boolean setDistanceModel(int model) {
        DistanceModel wanted = switch (model) {
            case DISTANCE_NONE -> DistanceModel.NONE;
            case INVERSE_DISTANCE -> DistanceModel.INVERSE;
            case INVERSE_DISTANCE_CLAMPED -> DistanceModel.INVERSE_CLAMPED;
            case LINEAR_DISTANCE -> DistanceModel.LINEAR;
            case LINEAR_DISTANCE_CLAMPED -> DistanceModel.LINEAR_CLAMPED;
            case EXPONENT_DISTANCE -> DistanceModel.EXPONENT;
            case EXPONENT_DISTANCE_CLAMPED -> DistanceModel.EXPONENT_CLAMPED;
            default -> null;
        };
        if (wanted == null) {
            return false;
        }
        device.setDistanceModel(wanted);
        return true;
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

    public void warnAboutDoppler() {
        if (warnedAboutDoppler || console == null) {
            return;
        }
        warnedAboutDoppler = true;
        console.warn("velocities are recorded but produce no doppler shift; web audio dropped it");
    }

}
