package java.awt.datatransfer;

public class Clipboard {

    private final String name;

    private Transferable contents;
    private ClipboardOwner owner;

    public Clipboard(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public synchronized void setContents(Transferable put, ClipboardOwner newOwner) {
        ClipboardOwner previous = owner;
        Transferable had = contents;
        contents = put;
        owner = newOwner;
        if (previous != null && previous != newOwner) {
            previous.lostOwnership(this, had);
        }
    }

    public synchronized Transferable getContents(Object requester) {
        return contents;
    }

    public synchronized boolean isDataFlavorAvailable(DataFlavor flavor) {
        return contents != null && contents.isDataFlavorSupported(flavor);
    }

    public synchronized DataFlavor[] getAvailableDataFlavors() {
        return contents == null ? new DataFlavor[0] : contents.getTransferDataFlavors();
    }
}
