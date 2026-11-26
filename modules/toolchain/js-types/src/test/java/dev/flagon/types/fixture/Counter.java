package dev.flagon.types.fixture;

import org.teavm.jso.JSExport;
import org.teavm.jso.JSProperty;

public class Counter {

    private int value;

    @JSExport
    public Counter(int start) {
        value = start;
    }

    @JSExport
    public static Counter zero() {
        return new Counter(0);
    }

    @JSExport
    public int increment() {
        return ++value;
    }

    @JSExport
    @JSProperty
    public int getValue() {
        return value;
    }

    public void reset() {
        value = 0;
    }
}
