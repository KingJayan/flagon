package org.w3c.dom;

public interface NamedNodeMap {

    Node getNamedItem(String name);

    Node removeNamedItem(String name) throws DOMException;

    Node item(int index);

    int getLength();
}
