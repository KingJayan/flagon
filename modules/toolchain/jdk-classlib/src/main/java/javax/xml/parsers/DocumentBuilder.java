package javax.xml.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentBuilder {

    protected DocumentBuilder() {
    }

    public Document parse(InputStream input) throws SAXException, IOException {
        return parse(new InputSource(input));
    }

    public Document parse(InputSource source) throws SAXException, IOException {
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            InputStream bytes = source.getByteStream();
            if (bytes == null) {
                throw new SAXException("the source has no stream to read");
            }
            reader = new InputStreamReader(bytes, StandardCharsets.UTF_8);
        }
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[8192];
        for (int read; (read = reader.read(buffer)) > 0; ) {
            text.append(buffer, 0, read);
        }
        return new XmlParser(text).document();
    }

    public boolean isNamespaceAware() {
        return false;
    }

    public boolean isValidating() {
        return false;
    }
}
