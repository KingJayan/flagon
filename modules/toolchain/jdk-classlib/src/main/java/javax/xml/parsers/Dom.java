package javax.xml.parsers;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

final class Dom {

    private Dom() {
    }

    abstract static class Base implements Node {

        Base parent;

        final Nodes children = new Nodes();

        @Override
        public String getNodeValue() {
            return null;
        }

        @Override
        public String getTextContent() {
            StringBuilder text = new StringBuilder();
            for (Node child : children.list) {
                if (child.getNodeType() != COMMENT_NODE) {
                    text.append(child.getTextContent());
                }
            }
            return text.toString();
        }

        @Override
        public Node getParentNode() {
            return parent;
        }

        @Override
        public NodeList getChildNodes() {
            return children;
        }

        @Override
        public Node getFirstChild() {
            return children.list.isEmpty() ? null : children.list.get(0);
        }

        @Override
        public NamedNodeMap getAttributes() {
            return null;
        }

        @Override
        public boolean hasChildNodes() {
            return !children.list.isEmpty();
        }

        void add(Base child) {
            child.parent = this;
            children.list.add(child);
        }

        void collect(String name, Nodes into) {
            for (Node child : children.list) {
                if (child instanceof ElementNode element) {
                    if ("*".equals(name) || element.name.equals(name)) {
                        into.list.add(element);
                    }
                    element.collect(name, into);
                }
            }
        }
    }

    static final class DocumentNode extends Base implements Document {

        @Override
        public short getNodeType() {
            return DOCUMENT_NODE;
        }

        @Override
        public String getNodeName() {
            return "#document";
        }

        @Override
        public Element getDocumentElement() {
            for (Node child : children.list) {
                if (child instanceof Element element) {
                    return element;
                }
            }
            return null;
        }

        @Override
        public NodeList getElementsByTagName(String name) {
            Nodes found = new Nodes();
            collect(name, found);
            return found;
        }
    }

    static final class ElementNode extends Base implements Element {

        final String name;

        final Attributes attributes = new Attributes();

        ElementNode(String name) {
            this.name = name;
        }

        @Override
        public short getNodeType() {
            return ELEMENT_NODE;
        }

        @Override
        public String getNodeName() {
            return name;
        }

        @Override
        public String getTagName() {
            return name;
        }

        @Override
        public NamedNodeMap getAttributes() {
            return attributes;
        }

        @Override
        public String getAttribute(String attribute) {
            Node found = attributes.getNamedItem(attribute);
            return found == null ? "" : found.getNodeValue();
        }

        @Override
        public boolean hasAttribute(String attribute) {
            return attributes.getNamedItem(attribute) != null;
        }

        @Override
        public NodeList getElementsByTagName(String tag) {
            Nodes found = new Nodes();
            collect(tag, found);
            return found;
        }
    }

    static final class AttrNode extends Base implements Attr {

        final String name;

        final String value;

        AttrNode(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public short getNodeType() {
            return ATTRIBUTE_NODE;
        }

        @Override
        public String getNodeName() {
            return name;
        }

        @Override
        public String getNodeValue() {
            return value;
        }

        @Override
        public String getTextContent() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    static class TextNode extends Base implements Text {

        final String data;

        TextNode(String data) {
            this.data = data;
        }

        @Override
        public short getNodeType() {
            return TEXT_NODE;
        }

        @Override
        public String getNodeName() {
            return "#text";
        }

        @Override
        public String getNodeValue() {
            return data;
        }

        @Override
        public String getTextContent() {
            return data;
        }

        @Override
        public String getData() {
            return data;
        }
    }

    static final class CommentNode extends Base implements Comment {

        final String data;

        CommentNode(String data) {
            this.data = data;
        }

        @Override
        public short getNodeType() {
            return COMMENT_NODE;
        }

        @Override
        public String getNodeName() {
            return "#comment";
        }

        @Override
        public String getNodeValue() {
            return data;
        }

        @Override
        public String getTextContent() {
            return data;
        }

        @Override
        public String getData() {
            return data;
        }
    }

    static final class Nodes implements NodeList {

        final List<Node> list = new ArrayList<>();

        @Override
        public Node item(int index) {
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }

        @Override
        public int getLength() {
            return list.size();
        }
    }

    static final class Attributes implements NamedNodeMap {

        final List<AttrNode> list = new ArrayList<>();

        @Override
        public Node getNamedItem(String name) {
            for (AttrNode attribute : list) {
                if (attribute.name.equals(name)) {
                    return attribute;
                }
            }
            return null;
        }

        @Override
        public Node removeNamedItem(String name) {
            for (int at = 0; at < list.size(); at++) {
                if (list.get(at).name.equals(name)) {
                    return list.remove(at);
                }
            }
            throw new DOMException(DOMException.NOT_FOUND_ERR, name + " is not an attribute here");
        }

        @Override
        public Node item(int index) {
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }

        @Override
        public int getLength() {
            return list.size();
        }
    }
}
