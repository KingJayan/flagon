package dev.flagon.platform.audio;

/** the pcm arithmetic every {@link AudioDevice} needs, in one place because none of it is host-specific. */
public final class Audio {

    private Audio() {
    }

    /** @throws AudioException if the data is not a whole number of frames, or the rate is not positive */
    public static void requireWholeFrames(AudioFormat format, int length, int sampleRate) {
        if (sampleRate <= 0) {
            throw new AudioException("sample rate must be positive: " + sampleRate);
        }
        requireWholeFrames(format, length);
    }

    /** @throws AudioException if the data is not a whole number of frames */
    public static void requireWholeFrames(AudioFormat format, int length) {
        if (format == null) {
            throw new AudioException("format must not be null");
        }
        if (length < 0 || length % format.frameSizeInBytes() != 0) {
            throw new AudioException("pcm length " + length + " is not a whole number of "
                    + format.frameSizeInBytes() + "-byte " + format + " frames");
        }
    }

    public static int frames(AudioFormat format, int lengthInBytes) {
        return lengthInBytes / format.frameSizeInBytes();
    }

    public static double durationInSeconds(AudioFormat format, int lengthInBytes, int sampleRate) {
        return (double) frames(format, lengthInBytes) / sampleRate;
    }

    /**
     * pulls one channel out of interleaved pcm as samples in [-1, 1].
     *
     * <p>16-bit samples are little-endian and signed, 8-bit are unsigned with 128 for silence,
     * which is what openal buffers hold and not what java's own byte reads assume.
     */
    public static float[] channelSamples(AudioFormat format, byte[] data, int length, int channel) {
        requireWholeFrames(format, length);
        if (channel < 0 || channel >= format.channels()) {
            throw new AudioException("no channel " + channel + " in " + format);
        }
        int frames = frames(format, length);
        float[] samples = new float[frames];
        int bytesPerSample = format.bitsPerSample() / 8;
        int stride = format.frameSizeInBytes();
        int at = channel * bytesPerSample;
        for (int frame = 0; frame < frames; frame++, at += stride) {
            if (bytesPerSample == 1) {
                samples[frame] = ((data[at] & 0xFF) - 128) / 128.0f;
            } else {
                int value = (data[at] & 0xFF) | (data[at + 1] << 8);
                samples[frame] = value / 32768.0f;
            }
        }
        return samples;
    }
}
