package javax.xml.validation;

import java.io.IOException;
import javax.xml.transform.Source;
import org.xml.sax.SAXException;

public abstract class Validator {

    protected Validator() {
    }

    public abstract void validate(Source source) throws SAXException, IOException;
}
