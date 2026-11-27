package dev.flagon.lwjgl;

import dev.flagon.platform.Platforms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ALC10Test {

    @BeforeEach
    void setUp() {
        Platforms.install(new FakeDisplayPlatform());
    }

    @AfterEach
    void tearDown() {
        ALC10.alcMakeContextCurrent(null);
        Platforms.reset();
    }

    /** what a game does before its first al* call, and what has to work for the rest to */
    @Test
    void opensADeviceAndMakesAContextOnItCurrent() {
        ALCdevice device = ALC10.alcOpenDevice((String) null);
        assertNotNull(device);

        ALCcontext context = ALC10.alcCreateContext(device, null);
        assertEquals(ALC10.ALC_TRUE, ALC10.alcMakeContextCurrent(context));

        assertSame(context, ALC10.alcGetCurrentContext());
        assertSame(device, ALC10.alcGetContextsDevice(context));
        assertEquals("silent", AL10.alGetString(AL10.AL_RENDERER));
        assertEquals(ALC10.ALC_NO_ERROR, ALC10.alcGetError(device));
    }

    @Test
    void destroyingTheCurrentContextTakesTheMixerWithIt() {
        ALCdevice device = ALC10.alcOpenDevice("");
        ALCcontext context = ALC10.alcCreateContext(device, null);
        ALC10.alcMakeContextCurrent(context);

        ALC10.alcDestroyContext(context);

        assertNull(ALC10.alcGetCurrentContext());
        assertFalse(context.isValid());
        assertTrue(ALC10.alcCloseDevice(device));
        assertFalse(device.isValid());
    }

    @Test
    void namesTheOutputItOpened() {
        ALCdevice device = ALC10.alcOpenDevice("Flagon Mixer");

        assertEquals("Flagon Mixer", ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER));
        assertEquals(1, ALC10.alcGetInteger(device, ALC10.ALC_MAJOR_VERSION));
        assertEquals(ALC10.ALC_NO_ERROR, ALC10.alcGetError(device));
    }

    @Test
    void latchesAnErrorForSomethingThatWasNeverOpened() {
        assertNull(ALC10.alcCreateContext(null, null));

        assertEquals(ALC10.ALC_INVALID_DEVICE, ALC10.alcGetError(null));
        assertEquals(ALC10.ALC_NO_ERROR, ALC10.alcGetError(null));
    }

    @Test
    void refusesAContextThatHasBeenDestroyed() {
        ALCdevice device = ALC10.alcOpenDevice((String) null);
        ALCcontext context = ALC10.alcCreateContext(device, null);
        ALC10.alcDestroyContext(context);

        assertEquals(ALC10.ALC_FALSE, ALC10.alcMakeContextCurrent(context));
        assertEquals(ALC10.ALC_INVALID_CONTEXT, ALC10.alcGetError(device));
    }
}
