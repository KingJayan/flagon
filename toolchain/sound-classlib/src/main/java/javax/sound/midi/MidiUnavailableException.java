package javax.sound.midi;

public class MidiUnavailableException extends Exception {

    public MidiUnavailableException() {
        super("a page has no midi device: nothing here plays a sequence. see "
                + "flagon-nu.vercel.app/docs");
    }

    public MidiUnavailableException(String message) {
        super(message);
    }
}
