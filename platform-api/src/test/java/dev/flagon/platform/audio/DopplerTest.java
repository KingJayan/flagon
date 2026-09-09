package dev.flagon.platform.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DopplerTest {

    private static final float[] ORIGIN = {0.0f, 0.0f, 0.0f};
    private static final float[] STILL = {0.0f, 0.0f, 0.0f};
    private static final float[] LISTENER = {0.0f, 0.0f, 10.0f};

    private static float shift(float[] sourceVelocity, float[] listenerVelocity) {
        return Doppler.shift(ORIGIN, sourceVelocity, LISTENER, listenerVelocity,
                Doppler.DEFAULT_FACTOR, Doppler.DEFAULT_SPEED_OF_SOUND);
    }

    @Test
    void nothingMovingDoesNotShift() {
        assertEquals(1.0f, shift(STILL, STILL), 1.0e-6f);
    }

    @Test
    void aSourceApproachingRaisesThePitch() {
        assertTrue(shift(new float[] {0.0f, 0.0f, 30.0f}, STILL) > 1.0f);
    }

    @Test
    void aSourceRecedingLowersThePitch() {
        assertTrue(shift(new float[] {0.0f, 0.0f, -30.0f}, STILL) < 1.0f);
    }

    @Test
    void aListenerApproachingRaisesThePitch() {
        assertTrue(shift(STILL, new float[] {0.0f, 0.0f, -30.0f}) > 1.0f);
    }

    @Test
    void motionAcrossTheLineDoesNotShift() {
        assertEquals(1.0f, shift(new float[] {30.0f, 0.0f, 0.0f}, STILL), 1.0e-6f);
    }

    @Test
    void theShiftIsTheRatioOpenAlSpecifies() {
        float speed = 340.0f;
        float closing = 34.0f;
        float shifted = Doppler.shift(ORIGIN, new float[] {0.0f, 0.0f, closing}, LISTENER, STILL,
                1.0f, speed);

        assertEquals(speed / (speed - closing), shifted, 1.0e-5f);
    }

    @Test
    void theFactorScalesTheShift() {
        float[] approaching = {0.0f, 0.0f, 30.0f};
        float once = Doppler.shift(ORIGIN, approaching, LISTENER, STILL, 1.0f,
                Doppler.DEFAULT_SPEED_OF_SOUND);
        float twice = Doppler.shift(ORIGIN, approaching, LISTENER, STILL, 2.0f,
                Doppler.DEFAULT_SPEED_OF_SOUND);

        assertTrue(twice > once);
    }

    @Test
    void aZeroFactorSwitchesTheShiftOff() {
        assertEquals(1.0f, Doppler.shift(ORIGIN, new float[] {0.0f, 0.0f, 30.0f}, LISTENER, STILL,
                0.0f, Doppler.DEFAULT_SPEED_OF_SOUND), 1.0e-6f);
    }

    /** a source outrunning its own sound must not invert the shift */
    @Test
    void aSourceFasterThanSoundIsClamped() {
        float shifted = Doppler.shift(ORIGIN, new float[] {0.0f, 0.0f, 5_000.0f}, LISTENER, STILL,
                1.0f, Doppler.DEFAULT_SPEED_OF_SOUND);

        assertTrue(shifted > 0.0f && Float.isFinite(shifted), "shift was " + shifted);
    }

    @Test
    void aSourceOnTheListenerHasNoLineToMeasureAlong() {
        assertEquals(1.0f, Doppler.shift(LISTENER, new float[] {0.0f, 0.0f, 30.0f}, LISTENER,
                STILL, 1.0f, Doppler.DEFAULT_SPEED_OF_SOUND), 1.0e-6f);
    }
}
