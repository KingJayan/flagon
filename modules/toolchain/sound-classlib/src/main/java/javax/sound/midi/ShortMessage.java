package javax.sound.midi;

public class ShortMessage extends MidiMessage {

    public static final int NOTE_OFF = 0x80;
    public static final int NOTE_ON = 0x90;
    public static final int CONTROL_CHANGE = 0xb0;
    public static final int PROGRAM_CHANGE = 0xc0;

    public ShortMessage() {
        super(new byte[3]);
    }

    public void setMessage(int command, int channel, int first, int second)
            throws InvalidMidiDataException {
        data = new byte[] {
            (byte) ((command & 0xf0) | (channel & 0x0f)), (byte) first, (byte) second,
        };
    }

    public int getCommand() {
        return getStatus() & 0xf0;
    }

    public int getChannel() {
        return getStatus() & 0x0f;
    }

    public int getData1() {
        return getLength() > 1 ? data[1] & 0xff : 0;
    }

    public int getData2() {
        return getLength() > 2 ? data[2] & 0xff : 0;
    }
}
