package dev.flagon.types.fixture;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
public interface Visitor extends JSObject {

    boolean visit(Point point, double weight);
}
