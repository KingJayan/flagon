package dev.flagon.platform.audio;

/**
 * the pitch shift openal derives from velocities, computed here because web audio dropped its own
 */
public final class Doppler {

    public static final float DEFAULT_FACTOR = 1.0f;
    public static final float DEFAULT_SPEED_OF_SOUND = 343.3f;

    private Doppler() {
    }

    /**
     * openal 1.1 measures both velocities along the line from the source to the listener and
     * clamps each to the speed sound travels, so a source outrunning its own sound does not
     * invert the shift.
     *
     * @return the factor a source's pitch is multiplied by, 1 when there is nothing to shift
     */
    public static float shift(float[] sourcePosition, float[] sourceVelocity,
                              float[] listenerPosition, float[] listenerVelocity,
                              float factor, float speedOfSound) {
        if (factor <= 0.0f || speedOfSound <= 0.0f) {
            return 1.0f;
        }
        float x = listenerPosition[0] - sourcePosition[0];
        float y = listenerPosition[1] - sourcePosition[1];
        float z = listenerPosition[2] - sourcePosition[2];
        float distance = (float) Math.sqrt(x * x + y * y + z * z);
        // a source sitting on the listener has no line to measure along
        if (distance == 0.0f) {
            return 1.0f;
        }
        float limit = speedOfSound / factor;
        float towards = Math.min(project(listenerVelocity, x, y, z, distance), limit);
        float away = Math.min(project(sourceVelocity, x, y, z, distance), limit);

        float numerator = speedOfSound - factor * towards;
        float denominator = speedOfSound - factor * away;
        if (denominator <= 0.0f) {
            return 1.0f;
        }
        return Math.max(numerator / denominator, 0.0f);
    }

    private static float project(float[] velocity, float x, float y, float z, float distance) {
        return (velocity[0] * x + velocity[1] * y + velocity[2] * z) / distance;
    }
}
