package javax.xml.transform.stream;

import java.io.InputStream;
import javax.xml.transform.Source;

public class StreamSource implements Source {

    private final InputStream input;

    private final String systemId;

    public StreamSource(InputStream input) {
        this(input, null);
    }

    public StreamSource(InputStream input, String systemId) {
        this.input = input;
        this.systemId = systemId;
    }

    public InputStream getInputStream() {
        return input;
    }

    @Override
    public String getSystemId() {
        return systemId;
    }
}
