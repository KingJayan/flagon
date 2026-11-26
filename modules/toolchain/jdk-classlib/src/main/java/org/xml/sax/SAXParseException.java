package org.xml.sax;

public class SAXParseException extends SAXException {

    private final int line;

    private final int column;

    public SAXParseException(String message, int line, int column) {
        super(message + " at line " + line + ", column " + column);
        this.line = line;
        this.column = column;
    }

    public int getLineNumber() {
        return line;
    }

    public int getColumnNumber() {
        return column;
    }
}
