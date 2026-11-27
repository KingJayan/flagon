package dev.flagon.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSObjects;

/** a web api refused: the DOMException or Error it rejected with */
public class WebException extends RuntimeException {

    private final String name;

    public WebException(String name, String message) {
        super(message.isEmpty() ? name : name + ": " + message);
        this.name = name;
    }

    /** @return the error's name, such as NotFoundError or SecurityError */
    public String name() {
        return name;
    }

    /** @return the exception for what a promise or request rejected with */
    public static RuntimeException of(Object error) {
        if (error instanceof RuntimeException thrown) {
            return thrown;
        }
        if (error instanceof Throwable thrown) {
            return new WebException("Error", String.valueOf(thrown.getMessage()));
        }
        JSObject value = (JSObject) error;
        if (value == null || JSObjects.isUndefined(value)) {
            return new WebException("Error", "");
        }
        return new WebException(hasName(value) ? nameOf(value) : "Error",
                hasMessage(value) ? messageOf(value) : textOf(value));
    }

    @JSBody(params = "error", script = "return typeof error.name === 'string';")
    private static native boolean hasName(JSObject error);

    @JSBody(params = "error", script = "return error.name;")
    private static native String nameOf(JSObject error);

    @JSBody(params = "error", script = "return typeof error.message === 'string';")
    private static native boolean hasMessage(JSObject error);

    @JSBody(params = "error", script = "return error.message;")
    private static native String messageOf(JSObject error);

    @JSBody(params = "error", script = "return String(error);")
    private static native String textOf(JSObject error);
}
