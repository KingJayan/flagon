package dev.flagon.types.fixture;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSExportClasses;

@JSExportClasses(Counter.class)
public final class FixtureMain {

    private FixtureMain() {
    }

    public static void main(String[] args) {
    }

    @JSExport
    public static int add(int a, int b) {
        return a + b;
    }

    @JSExport
    public static String greet(String in, Point at) {
        return in;
    }

    @JSExport
    public static long sum(int... values) {
        return 0;
    }

    @JSExport
    @JSByRef
    public static float[] samples(@JSByRef byte[] data, String[] labels) {
        return null;
    }

    @JSExport
    public static void each(Visitor visitor) {
    }

    @JSExport
    public static void delete() {
    }

    public static void hidden() {
    }
}
