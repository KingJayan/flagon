package org.lwjgl.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * riff wave pcm, read into what {@code alBufferData} takes.
 *
 * <p>only uncompressed pcm is read, which is all openal ever took. a compressed wave is
 * refused by name rather than played as noise.
 */
public final class WaveData {

    public final ByteBuffer data;
    public final int format;
    public final int samplerate;

    private WaveData(ByteBuffer data, int format, int samplerate) {
        this.data = data;
        this.format = format;
        this.samplerate = samplerate;
    }

    /** frees nothing a garbage collector would not, and is here because callers call it. */
    public void dispose() {
        data.clear();
    }

    public static WaveData create(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return read(bytes);
    }

    public static WaveData create(InputStream stream) {
        if (stream == null) {
            return null;
        }
        try {
            return read(stream.readAllBytes());
        } catch (IOException unreadable) {
            return null;
        }
    }

    public static WaveData create(URL location) {
        if (location == null) {
            return null;
        }
        try (InputStream stream = location.openStream()) {
            return create(stream);
        } catch (IOException unreachable) {
            return null;
        }
    }

    /** a path on the classpath, the way a game bundles its sounds. */
    public static WaveData create(String path) {
        if (path == null) {
            return null;
        }
        return create(WaveData.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path));
    }

    public static WaveData create(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return read(bytes);
    }

    private static WaveData read(byte[] wave) {
        if (wave.length < 12 || !tag(wave, 0, "RIFF") || !tag(wave, 8, "WAVE")) {
            return null;
        }
        int channels = 0;
        int bits = 0;
        int rate = 0;
        int at = 12;
        while (at + 8 <= wave.length) {
            int size = int32(wave, at + 4);
            int body = at + 8;
            if (size < 0 || body + size > wave.length) {
                return null;
            }
            if (tag(wave, at, "fmt ")) {
                if (size < 16 || int16(wave, body) != 1) {
                    return null;
                }
                channels = int16(wave, body + 2);
                rate = int32(wave, body + 4);
                bits = int16(wave, body + 14);
            } else if (tag(wave, at, "data")) {
                int name = name(channels, bits);
                if (name == 0 || rate <= 0) {
                    return null;
                }
                ByteBuffer pcm = BufferUtils.createByteBuffer(size);
                pcm.put(wave, body, size);
                pcm.flip();
                return new WaveData(pcm, name, rate);
            }
            // chunks are word aligned, and an odd one carries a pad byte the length does not count
            at = body + size + (size & 1);
        }
        return null;
    }

    private static int name(int channels, int bits) {
        if (channels == 1) {
            return bits == 8 ? AL10.AL_FORMAT_MONO8 : bits == 16 ? AL10.AL_FORMAT_MONO16 : 0;
        }
        if (channels == 2) {
            return bits == 8 ? AL10.AL_FORMAT_STEREO8 : bits == 16 ? AL10.AL_FORMAT_STEREO16 : 0;
        }
        return 0;
    }

    private static boolean tag(byte[] wave, int at, String name) {
        for (int letter = 0; letter < 4; letter++) {
            if (at + letter >= wave.length || wave[at + letter] != (byte) name.charAt(letter)) {
                return false;
            }
        }
        return true;
    }

    private static int int16(byte[] wave, int at) {
        return (wave[at] & 0xFF) | ((wave[at + 1] & 0xFF) << 8);
    }

    private static int int32(byte[] wave, int at) {
        return (wave[at] & 0xFF) | ((wave[at + 1] & 0xFF) << 8)
                | ((wave[at + 2] & 0xFF) << 16) | ((wave[at + 3] & 0xFF) << 24);
    }
}
