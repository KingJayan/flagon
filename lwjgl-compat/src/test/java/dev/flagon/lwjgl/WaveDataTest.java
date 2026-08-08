package dev.flagon.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WaveDataTest {

    @Test
    void readsMono16PcmAsOpenalTakesIt() {
        byte[] wave = wave(1, 16, 22_050, new byte[]{1, 2, 3, 4});

        WaveData read = WaveData.create(wave);

        assertEquals(AL10.AL_FORMAT_MONO16, read.format);
        assertEquals(22_050, read.samplerate);
        assertEquals(4, read.data.remaining());
        assertEquals(1, read.data.get(0));
        assertEquals(4, read.data.get(3));
    }

    @Test
    void readsStereo8FromAStream() {
        byte[] wave = wave(2, 8, 8_000, new byte[]{10, 20});

        WaveData read = WaveData.create(new ByteArrayInputStream(wave));

        assertEquals(AL10.AL_FORMAT_STEREO8, read.format);
        assertEquals(8_000, read.samplerate);
        assertEquals(2, read.data.remaining());
    }

    /** a chunk it does not know sits between the header and the pcm, and is stepped over */
    @Test
    void walksPastAChunkItDoesNotKnow() {
        byte[] wave = wave(1, 16, 44_100, new byte[]{7, 8}, true);

        WaveData read = WaveData.create(wave);

        assertEquals(44_100, read.samplerate);
        assertEquals(2, read.data.remaining());
    }

    @Test
    void refusesWhatIsNotUncompressedWave() {
        assertNull(WaveData.create(new byte[]{'O', 'g', 'g', 'S'}));
        assertNull(WaveData.create(compressed()));
        assertNull(WaveData.create((byte[]) null));
    }

    private static byte[] wave(int channels, int bits, int rate, byte[] pcm) {
        return wave(channels, bits, rate, pcm, false);
    }

    private static byte[] wave(int channels, int bits, int rate, byte[] pcm, boolean stranger) {
        return riff(1, channels, bits, rate, pcm, stranger);
    }

    private static byte[] compressed() {
        return riff(2, 1, 16, 8_000, new byte[]{1, 2}, false);
    }

    private static byte[] riff(int encoding, int channels, int bits, int rate, byte[] pcm,
                               boolean stranger) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tag(out, "RIFF");
        int32(out, 0);
        tag(out, "WAVE");

        tag(out, "fmt ");
        int32(out, 16);
        int16(out, encoding);
        int16(out, channels);
        int32(out, rate);
        int32(out, rate * channels * bits / 8);
        int16(out, channels * bits / 8);
        int16(out, bits);

        if (stranger) {
            tag(out, "LIST");
            int32(out, 3);
            out.write('a');
            out.write('b');
            out.write('c');
            out.write(0);
        }

        tag(out, "data");
        int32(out, pcm.length);
        out.write(pcm, 0, pcm.length);
        return out.toByteArray();
    }

    private static void tag(ByteArrayOutputStream out, String name) {
        for (int at = 0; at < 4; at++) {
            out.write(name.charAt(at));
        }
    }

    private static void int16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void int32(ByteArrayOutputStream out, int value) {
        int16(out, value & 0xFFFF);
        int16(out, (value >>> 16) & 0xFFFF);
    }
}
