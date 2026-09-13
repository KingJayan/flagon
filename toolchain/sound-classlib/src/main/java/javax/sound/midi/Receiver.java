package javax.sound.midi;

public interface Receiver extends java.io.Closeable {

    void send(MidiMessage message, long timestamp);

    @Override
    void close();
}
