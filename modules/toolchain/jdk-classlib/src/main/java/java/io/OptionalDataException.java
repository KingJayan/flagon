package java.io;

public class OptionalDataException extends ObjectStreamException {

    public int length;
    public boolean eof;

    OptionalDataException(int length) {
        this.length = length;
    }

    OptionalDataException(boolean eof) {
        this.eof = eof;
    }
}
