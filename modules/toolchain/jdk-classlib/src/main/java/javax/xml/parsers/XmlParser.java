package javax.xml.parsers;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** a non-validating parser: elements, attributes, text, cdata and comments; dtds are skipped */
final class XmlParser {

    private final CharSequence text;

    private int at;

    XmlParser(CharSequence text) {
        this.text = text;
        if (text.length() > 0 && text.charAt(0) == '﻿') {
            at = 1;
        }
    }

    Document document() throws SAXException {
        Dom.DocumentNode document = new Dom.DocumentNode();
        while (true) {
            skipSpace();
            if (at >= text.length()) {
                break;
            }
            if (startsWith("<?")) {
                skipPast("?>");
            } else if (startsWith("<!--")) {
                document.add(comment());
            } else if (startsWith("<!DOCTYPE")) {
                skipDoctype();
            } else if (peek() == '<') {
                if (document.getDocumentElement() != null) {
                    throw fail("a document has one root element");
                }
                document.add(element());
            } else {
                throw fail("text outside the root element");
            }
        }
        if (document.getDocumentElement() == null) {
            throw fail("no root element");
        }
        return document;
    }

    private Dom.ElementNode element() throws SAXException {
        expect('<');
        Dom.ElementNode element = new Dom.ElementNode(name());
        while (true) {
            skipSpace();
            if (startsWith("/>")) {
                at += 2;
                return element;
            }
            if (peek() == '>') {
                at++;
                break;
            }
            String attribute = name();
            skipSpace();
            expect('=');
            skipSpace();
            char quote = peek();
            if (quote != '"' && quote != '\'') {
                throw fail("an attribute value needs quotes");
            }
            at++;
            int end = indexOf(String.valueOf(quote));
            element.attributes.list.add(new Dom.AttrNode(attribute, decode(at, end)));
            at = end + 1;
        }
        StringBuilder pending = new StringBuilder();
        while (true) {
            if (at >= text.length()) {
                throw fail("<" + element.name + "> is not closed");
            }
            if (startsWith("</")) {
                flush(element, pending);
                at += 2;
                String closing = name();
                if (!closing.equals(element.name)) {
                    throw fail("</" + closing + "> closes <" + element.name + ">");
                }
                skipSpace();
                expect('>');
                return element;
            }
            if (startsWith("<!--")) {
                flush(element, pending);
                element.add(comment());
            } else if (startsWith("<![CDATA[")) {
                at += "<![CDATA[".length();
                int end = indexOf("]]>");
                pending.append(text, at, end);
                at = end + 3;
            } else if (startsWith("<?")) {
                skipPast("?>");
            } else if (peek() == '<') {
                flush(element, pending);
                element.add(element());
            } else {
                int end = at;
                while (end < text.length() && text.charAt(end) != '<') {
                    end++;
                }
                pending.append(decode(at, end));
                at = end;
            }
        }
    }

    private static void flush(Dom.ElementNode element, StringBuilder pending) {
        if (pending.length() > 0) {
            element.add(new Dom.TextNode(pending.toString()));
            pending.setLength(0);
        }
    }

    private Dom.CommentNode comment() throws SAXException {
        at += "<!--".length();
        int end = indexOf("-->");
        Dom.CommentNode comment = new Dom.CommentNode(text.subSequence(at, end).toString());
        at = end + 3;
        return comment;
    }

    private void skipDoctype() throws SAXException {
        int depth = 0;
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (c == '>' && depth == 0) {
                return;
            }
        }
        throw fail("<!DOCTYPE is not closed");
    }

    private String name() throws SAXException {
        int start = at;
        while (at < text.length()) {
            char c = text.charAt(at);
            if (Character.isWhitespace(c) || c == '>' || c == '/' || c == '=' || c == '<') {
                break;
            }
            at++;
        }
        if (start == at) {
            throw fail("a name is missing");
        }
        return text.subSequence(start, at).toString();
    }

    private String decode(int from, int to) throws SAXException {
        StringBuilder out = new StringBuilder(to - from);
        for (int index = from; index < to; index++) {
            char c = text.charAt(index);
            if (c != '&') {
                out.append(c);
                continue;
            }
            int end = index + 1;
            while (end < to && text.charAt(end) != ';') {
                end++;
            }
            if (end >= to) {
                throw fail("an entity has no ';'");
            }
            String entity = text.subSequence(index + 1, end).toString();
            switch (entity) {
                case "lt" -> out.append('<');
                case "gt" -> out.append('>');
                case "amp" -> out.append('&');
                case "quot" -> out.append('"');
                case "apos" -> out.append('\'');
                default -> {
                    if (!entity.startsWith("#")) {
                        throw fail("&" + entity + "; is not an entity this parser knows");
                    }
                    boolean hex = entity.startsWith("#x");
                    String digits = entity.substring(hex ? 2 : 1);
                    out.appendCodePoint(Integer.parseInt(digits, hex ? 16 : 10));
                }
            }
            index = end;
        }
        return out.toString();
    }

    private void skipSpace() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
            at++;
        }
    }

    private void skipPast(String end) throws SAXException {
        at = indexOf(end) + end.length();
    }

    private int indexOf(String wanted) throws SAXException {
        for (int index = at; index + wanted.length() <= text.length(); index++) {
            if (regionMatches(index, wanted)) {
                return index;
            }
        }
        throw fail("'" + wanted + "' is missing");
    }

    private boolean startsWith(String wanted) {
        return at + wanted.length() <= text.length() && regionMatches(at, wanted);
    }

    private boolean regionMatches(int from, String wanted) {
        for (int index = 0; index < wanted.length(); index++) {
            if (text.charAt(from + index) != wanted.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private char peek() throws SAXException {
        if (at >= text.length()) {
            throw fail("the document ends early");
        }
        return text.charAt(at);
    }

    private void expect(char wanted) throws SAXException {
        if (peek() != wanted) {
            throw fail("'" + wanted + "' was expected");
        }
        at++;
    }

    private SAXParseException fail(String problem) {
        int line = 1;
        int column = 1;
        for (int index = 0; index < Math.min(at, text.length()); index++) {
            if (text.charAt(index) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new SAXParseException(problem, line, column);
    }
}
