package java.awt.datatransfer;

/** a piece of text on its way to or from the clipboard */
public class StringSelection implements Transferable, ClipboardOwner {

    private static final DataFlavor[] FLAVORS = {DataFlavor.stringFlavor};

    private final String text;

    public StringSelection(String text) {
        this.text = text;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.stringFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return text;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
