package java.awt.datatransfer;

/** told when something else takes the clipboard over */
public interface ClipboardOwner {

    void lostOwnership(Clipboard clipboard, Transferable contents);
}
