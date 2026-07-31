package dev.flagon.platform.audio;

/** the pcm layouts openal buffers carry, and the only ones a host has to decode. */
public enum AudioFormat {

    MONO_8(1, 8),
    MONO_16(1, 16),
    STEREO_8(2, 8),
    STEREO_16(2, 16);

    private final int channels;
    private final int bitsPerSample;

    AudioFormat(int channels, int bitsPerSample) {
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
    }

    public int channels() {
        return channels;
    }

    public int bitsPerSample() {
        return bitsPerSample;
    }

    /** @return the size of one sample across every channel */
    public int frameSizeInBytes() {
        return channels * bitsPerSample / 8;
    }
}
