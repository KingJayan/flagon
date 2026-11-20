package org.w3c.dom;

public class DOMException extends RuntimeException {

    public static final short NOT_FOUND_ERR = 8;

    public short code;

    public DOMException(short code, String message) {
        super(message);
        this.code = code;
    }
}
