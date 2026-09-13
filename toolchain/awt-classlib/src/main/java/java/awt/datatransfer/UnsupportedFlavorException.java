package java.awt.datatransfer;

/** thrown when a clipboard is asked for a kind it is not holding */
public class UnsupportedFlavorException extends Exception {

    public UnsupportedFlavorException(DataFlavor flavor) {
        super("the clipboard is not holding " + flavor);
    }
}
