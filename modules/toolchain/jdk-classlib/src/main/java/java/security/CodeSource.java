package java.security;

import java.net.URL;

public class CodeSource {

    private final URL location;

    public CodeSource(URL location) {
        this.location = location;
    }

    public final URL getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "(" + location + ")";
    }
}
