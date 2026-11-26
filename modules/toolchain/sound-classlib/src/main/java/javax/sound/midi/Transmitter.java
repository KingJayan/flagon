package javax.sound.midi;

public interface Transmitter extends java.io.Closeable {

    void setReceiver(Receiver receiver);

    Receiver getReceiver();

    @Override
    void close();
}
