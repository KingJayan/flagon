package javax.sound.midi;

public interface Sequencer extends MidiDevice {

    void setSequence(Sequence sequence) throws InvalidMidiDataException;

    void start();

    void stop();

    boolean isRunning();

    void setMicrosecondPosition(long position);

    long getMicrosecondPosition();

    boolean addMetaEventListener(MetaEventListener listener);

    void removeMetaEventListener(MetaEventListener listener);
}
