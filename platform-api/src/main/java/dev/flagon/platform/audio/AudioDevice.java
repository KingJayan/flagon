package dev.flagon.platform.audio;

/**
 a host-agnostic mixer: pcm buffers played by positioned sources, addressed by int names
 */
public interface AudioDevice {

    int NO_BUFFER = 0;
    int NO_SOURCE = 0;

    /** @return a name for diagnostics, e.g. the api this is backed by */
    String name();

    /**
     * @return whether output is live right now. a browser holds a context created without a user
     *         gesture suspended, so a source can be playing and inaudible
     */
    boolean isOutputLive();

    int createBuffer();

    /**
     * replaces a buffer's contents with {@code length} bytes of pcm.
     *
     * @throws AudioException if the data is not a whole number of frames
     */
    void bufferData(int buffer, AudioFormat format, byte[] data, int length, int sampleRate);

    void deleteBuffer(int buffer);

    boolean isBuffer(int buffer);

    AudioFormat bufferFormat(int buffer);

    int bufferSampleRate(int buffer);

    int bufferSizeInBytes(int buffer);

    int createSource();

    void deleteSource(int source);

    boolean isSource(int source);

    /** attaches the buffer a source plays, or {@link #NO_BUFFER} to detach. */
    void attachBuffer(int source, int buffer);

    int attachedBuffer(int source);

    /**
     * appends buffers to a source's queue, which it plays through in order
    
     * @throws AudioException if any name is not a buffer, or the source holds a single buffer
     */
    void queueBuffers(int source, int[] buffers);

    /**
     * takes the oldest {@code count} buffers a source has finished with off its queue
     * @return their names, oldest first
     * @throws AudioException if the source has not finished with that many
     */
    int[] unqueueBuffers(int source, int count);

    /** how many buffers are on the queue, played or not. */
    int buffersQueued(int source);

    /** how many of them have played through and are waiting to be taken back. */
    int buffersProcessed(int source);

    void play(int source);

    void pause(int source);

    void stop(int source);

    /** stops the source and returns its playback position to the start. */
    void rewind(int source);

    SourceState sourceState(int source);

    void setLooping(int source, boolean looping);

    boolean isLooping(int source);

    void setGain(int source, float gain);

    float gain(int source);

    void setPitch(int source, float pitch);

    float pitch(int source);

    /** true when a source's position is relative to the listener rather than to the world. */
    void setSourceRelative(int source, boolean relative);

    boolean isSourceRelative(int source);

    void setSourcePosition(int source, float x, float y, float z);

    void setSourceVelocity(int source, float x, float y, float z);

    /** writes x, y and z into the first three elements of {@code into}. */
    void sourcePosition(int source, float[] into);

    void sourceVelocity(int source, float[] into);

    /** the model every source's attenuation is read through; sources keep what they were told. */
    void setDistanceModel(DistanceModel model);

    /** scales the doppler shift; 0 switches it off, as OpenAL's AL_DOPPLER_FACTOR does. */
    void setDopplerFactor(float factor);

    float dopplerFactor();

    /** the speed the shift is measured against, in the same units as the positions. */
    void setSpeedOfSound(float speed);

    float speedOfSound();

    /**
      * @return what the source's pitch is multiplied by for the velocities it and the listener
      *         currently have, 1 when nothing is moving along the line between them
      */
    float dopplerShift(int source);

    DistanceModel distanceModel();

    /** how gain falls off with distance; the same three numbers openal names. */
    void setSourceAttenuation(int source, float referenceDistance, float rolloffFactor,
                              float maxDistance);

    float referenceDistance(int source);

    float rolloffFactor(int source);

    float maxDistance(int source);

    void setListenerGain(float gain);

    float listenerGain();

    void setListenerPosition(float x, float y, float z);

    void setListenerVelocity(float x, float y, float z);

    void listenerPosition(float[] into);

    void listenerVelocity(float[] into);

    /** at-vector then up-vector, six floats, as {@code alListener(AL_ORIENTATION, ...)} passes them. */
    void setListenerOrientation(float[] atThenUp);

    void listenerOrientation(float[] into);

    /** stops everything and releases every buffer and source. the device is unusable afterwards. */
    void shutdown();
}
