package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;

/**
 unmodified openal against the browser's mixer, from raw pcm to a source running out
 */
public final class AudioDemoProgram implements Program {

    private static final int SAMPLE_RATE = 22_050;
    private static final int TONE_MILLIS = 250;
    private static final float TONE_HZ = 440.0f;

    @Override
    public String id() {
        return "audio";
    }

    @Override
    public String description() {
        return "plays pcm through OpenAL";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        AL.create();
        try {
            console.info("audio device: " + AL10.alGetString(AL10.AL_RENDERER));
            console.info("output live: " + Platforms.get().audio().isOutputLive());

            int buffer = AL10.alGenBuffers();
            ByteBuffer pcm = tone();
            AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
            console.info("buffered " + AL10.alGetBufferi(buffer, AL10.AL_SIZE) + " bytes at "
                    + AL10.alGetBufferi(buffer, AL10.AL_FREQUENCY) + " Hz");

            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, 0.2f);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            AL10.alListener3f(AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            check(console, "setup");

            play(console, source);
            latch(console);

            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(buffer);
            console.info("released: " + !AL10.alIsSource(source) + " " + !AL10.alIsBuffer(buffer));
            check(console, "teardown");
            console.info("audio: done");
        } finally {
            AL.destroy();
        }
    }

    /** a source is only proved to have played by taking as long to finish as its buffer is long. */
    private void play(ConsoleSink console, int source) throws Exception {
        AL10.alSourcePlay(source);
        console.info("source state: " + name(AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE)));

        long started = Platforms.get().clock().currentTimeMillis();
        long waited = 0;
        while (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
                && waited < 5_000) {
            Thread.sleep(20);
            waited = Platforms.get().clock().currentTimeMillis() - started;
        }
        console.info("source state: " + name(AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE))
                + " after " + waited + " ms");
        if (waited < TONE_MILLIS / 2) {
            throw new IllegalStateException(
                    "a " + TONE_MILLIS + " ms tone ended after " + waited + " ms; nothing played");
        }
    }

    /** openal reports a bad name through alGetError rather than by throwing, and so does this */
    private void latch(ConsoleSink console) {
        AL10.alSourcePlay(9_999);
        console.info("missing source reports "
                + (AL10.alGetError() == AL10.AL_INVALID_NAME ? "AL_INVALID_NAME" : "nothing"));
        console.info("error cleared: " + (AL10.alGetError() == AL10.AL_NO_ERROR));
    }

    private ByteBuffer tone() {
        int frames = SAMPLE_RATE * TONE_MILLIS / 1000;
        ByteBuffer pcm = BufferUtils.createByteBuffer(frames * 2);
        for (int frame = 0; frame < frames; frame++) {
            double angle = 2.0 * Math.PI * TONE_HZ * frame / SAMPLE_RATE;
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE);
            pcm.put((byte) (sample & 0xFF));
            pcm.put((byte) ((sample >> 8) & 0xFF));
        }
        pcm.flip();
        return pcm;
    }

    private void check(ConsoleSink console, String phase) {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            throw new IllegalStateException(phase + " left error 0x" + Integer.toHexString(error));
        }
        console.info(phase + ": no error");
    }

    private static String name(int state) {
        if (state == AL10.AL_PLAYING) {
            return "playing";
        }
        if (state == AL10.AL_PAUSED) {
            return "paused";
        }
        if (state == AL10.AL_STOPPED) {
            return "stopped";
        }
        return "initial";
    }
}
