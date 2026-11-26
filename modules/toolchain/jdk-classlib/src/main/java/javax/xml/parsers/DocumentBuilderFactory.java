package javax.xml.parsers;

public class DocumentBuilderFactory {

    protected DocumentBuilderFactory() {
    }

    public static DocumentBuilderFactory newInstance() {
        return new DocumentBuilderFactory();
    }

    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return new DocumentBuilder();
    }

    public void setNamespaceAware(boolean aware) {
    }

    public void setValidating(boolean validating) {
    }

    public void setIgnoringComments(boolean ignoring) {
    }

    public void setXIncludeAware(boolean aware) {
    }

    public void setExpandEntityReferences(boolean expand) {
    }

    public void setFeature(String name, boolean value) throws ParserConfigurationException {
    }
}
