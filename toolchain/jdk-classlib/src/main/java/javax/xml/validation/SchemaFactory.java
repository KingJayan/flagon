package javax.xml.validation;

import javax.xml.transform.Source;
import org.xml.sax.SAXException;

public class SchemaFactory {

    protected SchemaFactory() {
    }

    public static SchemaFactory newInstance(String language) {
        return new SchemaFactory();
    }

    public Schema newSchema(Source schema) throws SAXException {
        throw new SAXException("xml schema validation is not in a page. see docs/COMPATIBILITY.md");
    }
}
