package javax.sound.midi;

public interface MidiDevice extends java.io.Closeable {

    Info getDeviceInfo();

    void open() throws MidiUnavailableException;

    boolean isOpen();

    Receiver getReceiver() throws MidiUnavailableException;

    Transmitter getTransmitter() throws MidiUnavailableException;

    @Override
    void close();

    class Info {

        private final String name;

        protected Info(String name, String vendor, String description, String version) {
            this.name = name;
        }

        public final String getName() {
            return name;
        }

        @Override
        public final String toString() {
            return name;
        }
    }
}
