package dev.flagon.platform.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioTest {

    @Test
    void aFrameSpansEveryChannel() {
        assertEquals(1, AudioFormat.MONO_8.frameSizeInBytes());
        assertEquals(2, AudioFormat.MONO_16.frameSizeInBytes());
        assertEquals(2, AudioFormat.STEREO_8.frameSizeInBytes());
        assertEquals(4, AudioFormat.STEREO_16.frameSizeInBytes());
    }

    @Test
    void durationCountsFramesRatherThanBytes() {
        assertEquals(0.5, Audio.durationInSeconds(AudioFormat.STEREO_16, 4 * 22_050, 44_100), 1e-9);
    }

    @Test
    void halfAFrameIsRefused() {
        AudioException thrown = assertThrows(AudioException.class,
                () -> Audio.requireWholeFrames(AudioFormat.STEREO_16, 6, 44_100));

        assertTrue(thrown.getMessage().contains("STEREO_16"), thrown.getMessage());
    }

    @Test
    void aSampleRateOfZeroIsRefused() {
        assertThrows(AudioException.class,
                () -> Audio.requireWholeFrames(AudioFormat.MONO_16, 4, 0));
    }

    /** 8-bit pcm is unsigned with 128 for silence, which a plain byte read gets wrong. */
    @Test
    void eightBitSamplesAreUnsigned() {
        byte[] pcm = {(byte) 128, (byte) 255, 0};

        float[] samples = Audio.channelSamples(AudioFormat.MONO_8, pcm, pcm.length, 0);

        assertArrayEquals(new float[] {0.0f, 127 / 128.0f, -1.0f}, samples, 1e-6f);
    }

    /** 16-bit pcm is little-endian and signed, which java's own byte order is not. */
    @Test
    void sixteenBitSamplesAreLittleEndianAndSigned() {
        byte[] pcm = {0x00, 0x00, (byte) 0xFF, 0x7F, 0x00, (byte) 0x80};

        float[] samples = Audio.channelSamples(AudioFormat.MONO_16, pcm, pcm.length, 0);

        assertArrayEquals(new float[] {0.0f, 32_767 / 32_768.0f, -1.0f}, samples, 1e-6f);
    }

    @Test
    void channelsAreDeinterleaved() {
        byte[] pcm = {1, 2, 3, 4};

        assertArrayEquals(new float[] {(1 - 128) / 128.0f, (3 - 128) / 128.0f},
                Audio.channelSamples(AudioFormat.STEREO_8, pcm, pcm.length, 0), 1e-6f);
        assertArrayEquals(new float[] {(2 - 128) / 128.0f, (4 - 128) / 128.0f},
                Audio.channelSamples(AudioFormat.STEREO_8, pcm, pcm.length, 1), 1e-6f);
    }

    @Test
    void aChannelThatIsNotThereIsRefused() {
        assertThrows(AudioException.class,
                () -> Audio.channelSamples(AudioFormat.MONO_8, new byte[2], 2, 1));
    }
}
