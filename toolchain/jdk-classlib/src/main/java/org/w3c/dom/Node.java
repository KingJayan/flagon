package org.w3c.dom;

public interface Node {

    short ELEMENT_NODE = 1;
    short ATTRIBUTE_NODE = 2;
    short TEXT_NODE = 3;
    short CDATA_SECTION_NODE = 4;
    short COMMENT_NODE = 8;
    short DOCUMENT_NODE = 9;

    short getNodeType();

    String getNodeName();

    String getNodeValue();

    String getTextContent();

    Node getParentNode();

    NodeList getChildNodes();

    Node getFirstChild();

    NamedNodeMap getAttributes();

    boolean hasChildNodes();
}
