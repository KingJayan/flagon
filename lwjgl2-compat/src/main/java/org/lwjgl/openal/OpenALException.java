package org.lwjgl.openal;

import org.lwjgl.LWJGLException;

/** thrown for an openal error a caller cannot recover from, as lwjgl's own bindings do. */
public class OpenALException extends RuntimeException {

    public OpenALException(String message) {
        super(message);
    }

    public OpenALException(LWJGLException cause) {
        super(cause);
    }
}
