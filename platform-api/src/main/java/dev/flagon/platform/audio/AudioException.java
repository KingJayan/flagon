package dev.flagon.platform.audio;

/** a request the audio device could not carry out. */
public class AudioException extends RuntimeException {

    public AudioException(String message) {
        super(message);
    }

    public AudioException(String message, Throwable cause) {
        super(message, cause);
    }
}
