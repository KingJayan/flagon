package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamesTest {

    private final Names<String> names = new Names<>();

    @Test
    void handsOutNamesFromOne() {
        assertEquals(1, names.add("a"));
        assertEquals(2, names.add("b"));
        assertEquals("a", names.get(1));
    }

    @Test
    void givesAFreedNameBackBeforeMakingANewOne() {
        names.add("a");
        int second = names.add("b");
        names.add("c");

        assertEquals("b", names.remove(second));

        assertEquals(second, names.add("d"));
        assertEquals("d", names.get(second));
    }

    @Test
    void treatsAnUnknownNameAsEmpty() {
        assertNull(names.get(0));
        assertNull(names.get(-1));
        assertNull(names.get(99));
        assertFalse(names.has(99));
        assertNull(names.remove(99));
    }

    @Test
    void putsAnObjectUnderAChosenName() {
        names.put(4, "d");

        assertEquals("d", names.get(4));
        assertTrue(names.has(4));
        assertEquals(5, names.size());
    }

    @Test
    void fillsTheGapBeforeAChosenNameWithFreeNames() {
        names.put(3, "c");

        assertEquals(1, names.add("a"));
        assertEquals(2, names.add("b"));
        assertEquals(4, names.add("d"));
        assertEquals("c", names.get(3));
    }

    @Test
    void refusesANameBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> names.put(0, "a"));
    }

    @Test
    void walksOnlyTheNamesInUse() {
        names.add("a");
        int second = names.add("b");
        names.add("c");
        names.remove(second);

        List<String> walked = new ArrayList<>();
        for (String held : names) {
            walked.add(held);
        }

        assertEquals(List.of("a", "c"), walked);
    }

    @Test
    void startsOverAfterAClear() {
        names.add("a");
        names.add("b");

        names.clear();

        assertEquals(1, names.size());
        assertFalse(names.iterator().hasNext());
        assertEquals(1, names.add("c"));
    }

    @Test
    void keepsHandingOutNamesPastTheFreeListSize() {
        for (int at = 1; at <= 40; at++) {
            assertEquals(at, names.add("x" + at));
        }
        for (int at = 1; at <= 40; at++) {
            names.remove(at);
        }
        for (int at = 1; at <= 40; at++) {
            assertTrue(names.add("y" + at) <= 40);
        }
    }

    @Test
    void neverHandsOutZero() {
        assertNotEquals(0, names.add("a"));
    }

    @Test
    void forgetsWhatWasRemoved() {
        int name = names.add("a");

        names.remove(name);

        assertNull(names.get(name));
        assertFalse(names.has(name));
    }

    @Test
    void removingTwiceFreesTheNameOnce() {
        int first = names.add("a");
        names.remove(first);
        names.remove(first);

        assertNotEquals(names.add("b"), names.add("c"));
    }

    @Test
    void doesNotHandOutAFreedNameThatWasPutAgain() {
        int first = names.add("a");
        names.remove(first);
        names.put(first, "b");

        assertNotEquals(first, names.add("c"));
    }

    /** the leak this table exists to not have: a session that churns objects at a steady count */
    @Test
    void doesNotGrowWhileTheLiveCountHolds() {
        for (int at = 0; at < 8; at++) {
            names.add("held " + at);
        }
        int settled = names.size();

        for (int round = 0; round < 1000; round++) {
            names.remove(names.add("churned"));
        }

        assertEquals(settled + 1, names.size());
    }
}
