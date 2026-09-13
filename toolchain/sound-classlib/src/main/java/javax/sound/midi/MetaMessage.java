package javax.sound.midi;

public class MetaMessage extends MidiMessage {

    public MetaMessage() {
        super(new byte[3]);
    }

    public int getType() {
        return getLength() > 1 ? data[1] & 0xff : 0;
    }
}
