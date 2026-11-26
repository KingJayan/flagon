package org.xml.sax;

import java.io.InputStream;
import java.io.Reader;

public class InputSource {

    private InputStream bytes;

    private Reader characters;

    private String systemId;

    public InputSource() {
    }

    public InputSource(InputStream bytes) {
        this.bytes = bytes;
    }

    public InputSource(Reader characters) {
        this.characters = characters;
    }

    public InputSource(String systemId) {
        this.systemId = systemId;
    }

    public InputStream getByteStream() {
        return bytes;
    }

    public void setByteStream(InputStream bytes) {
        this.bytes = bytes;
    }

    public Reader getCharacterStream() {
        return characters;
    }

    public void setCharacterStream(Reader characters) {
        this.characters = characters;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }
}
