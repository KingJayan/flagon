package javax.sound.midi;

public abstract class MidiMessage {

    protected byte[] data;

    protected MidiMessage(byte[] data) {
        this.data = data;
    }

    public byte[] getMessage() {
        return data == null ? new byte[0] : data.clone();
    }

    public int getStatus() {
        return data == null || data.length == 0 ? 0 : data[0] & 0xff;
    }

    public int getLength() {
        return data == null ? 0 : data.length;
    }
}
