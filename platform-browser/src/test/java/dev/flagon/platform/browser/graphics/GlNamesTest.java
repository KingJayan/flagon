package dev.flagon.platform.browser.graphics;

import org.junit.jupiter.api.Test;
import org.teavm.jso.JSObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlNamesTest {

    private record Thing(String what) implements JSObject {
    }

    private final GlNames names = new GlNames();

    @Test
    void neverHandsOutZero() {
        assertNotEquals(0, names.add(new Thing("a")));
    }

    @Test
    void answersWithWhatWasStored() {
        Thing thing = new Thing("a");

        assertSame(thing, names.get(names.add(thing)));
    }

    @Test
    void knowsNothingOfZeroOrANameNeverGivenOut() {
        assertNull(names.get(0));
        assertNull(names.get(-1));
        assertNull(names.get(7));
    }

    @Test
    void forgetsWhatWasRemoved() {
        int name = names.add(new Thing("a"));

        names.remove(name);

        assertNull(names.get(name));
    }

    @Test
    void removingTwiceFreesTheNameOnce() {
        int first = names.add(new Thing("a"));
        names.remove(first);
        names.remove(first);

        assertNotEquals(names.add(new Thing("b")), names.add(new Thing("c")));
    }

    @Test
    void handsOutAFreedNameAgain() {
        int first = names.add(new Thing("a"));
        names.remove(first);

        assertEquals(first, names.add(new Thing("b")));
    }

    @Test
    void givesAnObjectToANameItNeverHandedOut() {
        Thing thing = new Thing("a");

        names.put(7, thing);

        assertSame(thing, names.get(7));
    }

    @Test
    void handsOutTheNamesBelowAPutName() {
        names.put(3, new Thing("a"));

        assertEquals(1, names.add(new Thing("b")));
        assertEquals(2, names.add(new Thing("c")));
        assertEquals(4, names.add(new Thing("d")));
    }

    @Test
    void doesNotHandOutAFreedNameThatWasPutAgain() {
        int first = names.add(new Thing("a"));
        names.remove(first);
        names.put(first, new Thing("b"));

        assertNotEquals(first, names.add(new Thing("c")));
    }

    /** the leak this table exists to not have: a session that churns objects at a steady count */
    @Test
    void doesNotGrowWhileTheLiveCountHolds() {
        for (int at = 0; at < 8; at++) {
            names.add(new Thing("held " + at));
        }
        int settled = names.size();

        for (int round = 0; round < 1000; round++) {
            int name = names.add(new Thing("churned"));
            names.remove(name);
        }

        assertEquals(settled + 1, names.size());
    }
}
