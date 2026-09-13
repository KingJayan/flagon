package javax.sound.midi;

public class MidiUnavailableException extends Exception {

    public MidiUnavailableException() {
        super("a page has no midi device: nothing here plays a sequence. see "
                + "docs/COMPATIBILITY.md");
    }

    public MidiUnavailableException(String message) {
        super(message);
    }
}
