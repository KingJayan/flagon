package dev.flagon.platform.browser.display;

import dev.flagon.platform.Clock;
import dev.flagon.platform.input.KeyEvent;
import dev.flagon.platform.input.MouseEvent;
import dev.flagon.platform.input.Scancode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserInputTest {

    private static final int HEIGHT = 360;

    private TestInput input;

    /** a canvas of a known height whose pointer lock the test decides */
    private static final class TestInput extends BrowserInput {

        boolean locked;

        TestInput(Clock clock) {
            super(null, clock);
        }

        @Override
        int height() {
            return HEIGHT;
        }

        @Override
        boolean pointerLocked() {
            return locked;
        }
    }

    @BeforeEach
    void setUp() {
        input = new TestInput(new Clock() {
            @Override
            public long currentTimeMillis() {
                return 0L;
            }

            @Override
            public long nanoTime() {
                return 42L;
            }
        });
    }

    // keys

    @Test
    void reportsAKeyItKnowsAsHandled() {
        assertTrue(input.onKey("KeyW", "w", true, false));
        assertTrue(input.isKeyDown(Scancode.W));
    }

    /** an unhandled key has to reach the page, or the browser loses its own shortcuts */
    @Test
    void leavesAKeyItDoesNotKnowToThePage() {
        assertFalse(input.onKey("BrowserSearch", "", true, false));
        assertNull(input.nextKeyEvent());
    }

    @Test
    void queuesAKeyPressAndItsRelease() {
        input.onKey("KeyW", "w", true, false);
        input.onKey("KeyW", "w", false, false);

        KeyEvent pressed = input.nextKeyEvent();
        KeyEvent released = input.nextKeyEvent();

        assertEquals(Scancode.W, pressed.key());
        assertTrue(pressed.isPressed());
        assertEquals('w', pressed.character());
        assertFalse(released.isPressed());
        assertFalse(input.isKeyDown(Scancode.W));
        assertNull(input.nextKeyEvent());
    }

    @Test
    void swallowsARepeatUntilAnApplicationAsksForOne() {
        input.onKey("KeyW", "w", true, false);
        input.nextKeyEvent();

        assertTrue(input.onKey("KeyW", "w", true, true), "a repeat still belongs to the canvas");
        assertNull(input.nextKeyEvent());

        input.setRepeatEvents(true);
        input.onKey("KeyW", "w", true, true);

        assertTrue(input.nextKeyEvent().isRepeat());
    }

    /** the release of a key held when focus went elsewhere never arrives */
    @Test
    void lettingGoOfTheKeyboardLetsGoOfEveryKey() {
        input.onKey("KeyW", "w", true, false);
        input.onKey("ShiftLeft", "Shift", true, false);

        input.onFocus(false);

        assertFalse(input.isKeyDown(Scancode.W));
        assertFalse(input.isKeyDown(Scancode.LEFT_SHIFT));
    }

    @Test
    void answersForAScancodeThatIsNotOne() {
        assertFalse(input.isKeyDown(Scancode.NONE));
        assertFalse(input.isKeyDown(-1));
        assertFalse(input.isKeyDown(Scancode.COUNT + 10));
    }

    // buttons and the pointer

    @Test
    void putsTheMiddleAndRightButtonsWhereGlEraCodeExpectsThem() {
        input.onButton(1, true, 0, 0);
        input.onButton(2, true, 0, 0);

        assertTrue(input.isButtonDown(2), "the dom's middle button is gl's button 2");
        assertTrue(input.isButtonDown(1), "the dom's right button is gl's button 1");
        assertFalse(input.isButtonDown(0));
    }

    @Test
    void countsFromTheBottomOfTheCanvasTheWayGlDoes() {
        input.onButton(0, true, 100, 0);

        assertEquals(100, input.mouseX());
        assertEquals(HEIGHT - 1, input.mouseY());
    }

    @Test
    void leavesThePointerWhereItWasWhileItIsLocked() {
        input.onMove(10, 10, 0, 0, false);
        int x = input.mouseX();
        int y = input.mouseY();
        input.locked = true;

        input.onButton(0, true, 500, 500);

        assertEquals(x, input.mouseX());
        assertEquals(y, input.mouseY());
    }

    @Test
    void movesByWhatTheMouseMovedWhileItIsLocked() {
        input.locked = true;

        input.onMove(0, 0, 4, 3, true);

        assertEquals(4, input.takeDeltaX());
        assertEquals(-3, input.takeDeltaY(), "the y axis is the other way up from the dom's");
    }

    @Test
    void handsOutADeltaOnceAndThenStartsAgain() {
        input.onMove(10, 10, 0, 0, false);

        assertEquals(10, input.takeDeltaX());
        assertEquals(HEIGHT - 11, input.takeDeltaY());
        assertEquals(0, input.takeDeltaX());
        assertEquals(0, input.takeDeltaY());
    }

    @Test
    void saysNothingAboutAMoveThatMovedNothing() {
        input.onMove(10, 10, 0, 0, false);
        while (input.nextMouseEvent() != null) {
            continue;
        }

        input.onMove(10, 10, 0, 0, false);

        assertNull(input.nextMouseEvent());
    }

    @Test
    void countsTheGrabbedPointerAsInsideWhereverItIs() {
        input.onPresence(false);
        assertFalse(input.isPointerInside());

        input.locked = true;

        assertTrue(input.isPointerInside());
    }

    // the wheel

    @Test
    void turnsAWheelNotchIntoTheHundredAndTwentyGlExpects() {
        input.onWheel(-100.0, 0);

        assertEquals(120, input.takeWheel());
        assertEquals(0, input.takeWheel());
    }

    /** a trackpad sends pixels a few at a time, and a notch is only worth reporting whole */
    @Test
    void keepsThePixelsOfAPartialNotchUntilThereAreEnough() {
        for (int nudge = 0; nudge < 4; nudge++) {
            input.onWheel(-25.0, 0);
        }

        assertEquals(120, input.takeWheel());

        input.onWheel(-25.0, 0);
        assertEquals(0, input.takeWheel());
    }

    @Test
    void readsALineAndAPageOfScrollingAsWell() {
        input.onWheel(-2.5, 1);
        assertEquals(120, input.takeWheel());

        input.onWheel(-0.25, 2);
        assertEquals(120, input.takeWheel());
    }

    @Test
    void turnsTheWheelBothWays() {
        input.onWheel(-100.0, 0);
        assertEquals(120, input.takeWheel());

        input.onWheel(100.0, 0);
        assertEquals(-120, input.takeWheel());
    }

    // the queues

    @Test
    void dropsTheOldestEventRatherThanGrowingForever() {
        for (int event = 0; event < 1000; event++) {
            input.onKey("KeyW", "w", event % 2 == 0, false);
        }

        int queued = 0;
        while (input.nextKeyEvent() != null) {
            queued++;
        }

        assertEquals(256, queued);
    }

    @Test
    void forgetsEverythingItWasHoldingOnReset() {
        input.onKey("KeyW", "w", true, false);
        input.onButton(0, true, 5, 5);
        input.onWheel(-100.0, 0);

        input.reset();

        assertNull(input.nextKeyEvent());
        assertNull(input.nextMouseEvent());
        assertFalse(input.isKeyDown(Scancode.W));
        assertFalse(input.isButtonDown(0));
        assertEquals(0, input.takeWheel());
        assertEquals(0, input.takeDeltaX());
    }

    @Test
    void stampsEventsWithTheHostsClock() {
        input.onKey("KeyW", "w", true, false);
        input.onButton(0, true, 1, 1);

        KeyEvent key = input.nextKeyEvent();
        MouseEvent mouse = input.nextMouseEvent();

        assertNotNull(key);
        assertEquals(42L, key.nanos());
        assertEquals(42L, mouse.nanos());
    }
}
