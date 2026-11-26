package java.awt.datatransfer;

/**
 * something a clipboard can hold, and the kinds it can be read as
 */
public interface Transferable {

    DataFlavor[] getTransferDataFlavors();

    boolean isDataFlavorSupported(DataFlavor flavor);

    Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException;
}
