package javax.sound.midi;

import java.net.URL;

public final class MidiSystem {

    private MidiSystem() {
    }

    public static MidiDevice.Info[] getMidiDeviceInfo() {
        return new MidiDevice.Info[0];
    }

    public static MidiDevice getMidiDevice(MidiDevice.Info info) throws MidiUnavailableException {
        throw new MidiUnavailableException();
    }

    public static Receiver getReceiver() throws MidiUnavailableException {
        throw new MidiUnavailableException();
    }

    public static Sequencer getSequencer() throws MidiUnavailableException {
        throw new MidiUnavailableException();
    }

    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        throw new MidiUnavailableException();
    }

    public static Sequence getSequence(URL url) throws InvalidMidiDataException {
        throw new InvalidMidiDataException("a page has no midi device: nothing here reads a "
                + "sequence. see docs/COMPATIBILITY.md");
    }
}
