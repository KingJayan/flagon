package dev.flagon.types.fixture;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Point extends Shape {

    @JSProperty
    int getX();

    @JSProperty
    void setX(int x);

    @JSProperty
    boolean isVisible();

    @JSMethod("toString")
    String text();

    String label(String prefix, JSObject extra);

    default int twice() {
        return getX() * 2;
    }
}
