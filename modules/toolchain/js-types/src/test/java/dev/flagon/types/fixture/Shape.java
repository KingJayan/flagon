package dev.flagon.types.fixture;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Shape extends JSObject {

    @JSProperty("kind")
    String shapeKind();
}
