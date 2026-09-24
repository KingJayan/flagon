package dev.flagon.platform.audio;

/**
 host-agnostic mixer where pcm buffers are played by positioned sources and addressed by int names
 */
public interface AudioDevice {

    int NO_BUFFER = 0;
    int NO_SOURCE = 0;

    String name();

    boolean isOutputLive();

    int createBuffer();

    void bufferData(int buffer, AudioFormat format, byte[] data, int length, int sampleRate);

    void deleteBuffer(int buffer);

    boolean isBuffer(int buffer);

    AudioFormat bufferFormat(int buffer);

    int bufferSampleRate(int buffer);

    int bufferSizeInBytes(int buffer);

    int createSource();

    void deleteSource(int source);

    boolean isSource(int source);

    void attachBuffer(int source, int buffer);

    int attachedBuffer(int source);

    void queueBuffers(int source, int[] buffers);

    int[] unqueueBuffers(int source, int count);

    int buffersQueued(int source);

    int buffersProcessed(int source);

    void play(int source);

    void pause(int source);

    void stop(int source);

    void rewind(int source);

    SourceState sourceState(int source);

    void setLooping(int source, boolean looping);

    boolean isLooping(int source);

    void setGain(int source, float gain);

    float gain(int source);

    void setPitch(int source, float pitch);

    float pitch(int source);

    void setSourceRelative(int source, boolean relative);

    boolean isSourceRelative(int source);

    void setSourcePosition(int source, float x, float y, float z);

    void setSourceVelocity(int source, float x, float y, float z);

    void sourcePosition(int source, float[] into);

    void sourceVelocity(int source, float[] into);

    void setDistanceModel(DistanceModel model);

    void setDopplerFactor(float factor);

    float dopplerFactor();

    void setSpeedOfSound(float speed);

    float speedOfSound();

    float dopplerShift(int source);

    DistanceModel distanceModel();

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

    void setListenerOrientation(float[] atThenUp);

    void listenerOrientation(float[] into);

    void shutdown();
}
