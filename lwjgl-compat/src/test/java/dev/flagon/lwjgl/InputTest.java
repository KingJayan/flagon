package dev.flagon.lwjgl;

import dev.flagon.lwjgl.internal.InputState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputTest {

    private FakeInputSource source;

    @BeforeEach
    void setUp() {
        source = new FakeInputSource();
        InputState.attach(source);
    }

    @AfterEach
    void tearDown() {
        InputState.detach();
        Keyboard.destroy();
        Mouse.destroy();
    }

    @Test
    void keyEventsAreDrainedOneAtATime() throws LWJGLException {
        Keyboard.create();
        source.pressKey(Keyboard.KEY_W, 'w');
        source.releaseKey(Keyboard.KEY_W);

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_W, Keyboard.getEventKey());
        assertEquals('w', Keyboard.getEventCharacter());
        assertTrue(Keyboard.getEventKeyState());

        assertTrue(Keyboard.next());
        assertEquals(Keyboard.KEY_W, Keyboard.getEventKey());
        assertFalse(Keyboard.getEventKeyState());

        assertFalse(Keyboard.next());
    }

    @Test
    void heldKeysAreReadableWithoutDrainingEvents() {
        source.pressKey(Keyboard.KEY_A, 'a');
        assertTrue(Keyboard.isKeyDown(Keyboard.KEY_A));
        assertFalse(Keyboard.isKeyDown(Keyboard.KEY_D));

        source.releaseKey(Keyboard.KEY_A);
        assertFalse(Keyboard.isKeyDown(Keyboard.KEY_A));
    }

    /** the getters describe the last dequeued event, so they must be inert before the first. */
    @Test
    void theGettersAreSafeBeforeAnyEvent() {
        assertEquals(Keyboard.KEY_NONE, Keyboard.getEventKey());
        assertEquals(Keyboard.CHAR_NONE, Keyboard.getEventCharacter());
        assertFalse(Keyboard.getEventKeyState());
        assertEquals(-1, Mouse.getEventButton());
        assertEquals(0, Mouse.getEventDWheel());
    }

    @Test
    void repeatEventsAreOffUntilAskedFor() {
        assertFalse(Keyboard.areRepeatEventsEnabled());
        Keyboard.enableRepeatEvents(true);
        assertTrue(Keyboard.areRepeatEventsEnabled());
    }

    @Test
    void keyNamesRoundTrip() {
        assertEquals("W", Keyboard.getKeyName(Keyboard.KEY_W));
        assertEquals("LSHIFT", Keyboard.getKeyName(Keyboard.KEY_LSHIFT));
        assertEquals(Keyboard.KEY_W, Keyboard.getKeyIndex("W"));
        assertEquals(Keyboard.KEY_ESCAPE, Keyboard.getKeyIndex("ESCAPE"));
        assertEquals("NONE", Keyboard.getKeyName(Keyboard.KEY_NONE));
        assertEquals(Keyboard.KEY_NONE, Keyboard.getKeyIndex("NOT_A_KEY"));
    }

    @Test
    void mouseButtonsAndPositionAreReported() throws LWJGLException {
        Mouse.create();
        source.moveTo(120, 80);
        source.clickButton(1);

        assertEquals(120, Mouse.getX());
        assertEquals(80, Mouse.getY());
        assertTrue(Mouse.isButtonDown(1));
        assertFalse(Mouse.isButtonDown(0));

        assertTrue(Mouse.next());
        assertEquals(-1, Mouse.getEventButton());
        assertEquals(120, Mouse.getEventDX());

        assertTrue(Mouse.next());
        assertEquals(1, Mouse.getEventButton());
        assertTrue(Mouse.getEventButtonState());
    }

    /** movement is consumed by reading it, which is how mouselook avoids applying it twice. */
    @Test
    void movementIsResetByReadingIt() {
        source.moveTo(10, 10);
        source.moveTo(30, 25);

        assertEquals(30, Mouse.getDX());
        assertEquals(25, Mouse.getDY());
        assertEquals(0, Mouse.getDX());
        assertEquals(0, Mouse.getDY());
    }

    @Test
    void wheelIsResetByReadingIt() {
        source.turnWheel(120);
        source.turnWheel(120);

        assertEquals(240, Mouse.getDWheel());
        assertEquals(0, Mouse.getDWheel());
    }

    @Test
    void grabbingIsReportedRatherThanAssumed() {
        Mouse.setGrabbed(true);
        assertTrue(Mouse.isGrabbed());

        Mouse.setGrabbed(false);
        assertFalse(Mouse.isGrabbed());

        source.grantGrab = false;
        Mouse.setGrabbed(true);
        assertFalse(Mouse.isGrabbed(), "a refused grab must not be reported as granted");
    }

    @Test
    void aGrabbedPointerCountsAsInsideTheWindow() {
        assertFalse(Mouse.isInsideWindow());
        Mouse.setGrabbed(true);
        assertTrue(Mouse.isInsideWindow());
    }

    @Test
    void inputBeforeDisplayCreateFailsWithAClearMessage() {
        InputState.detach();

        LWJGLException failure = assertThrows(LWJGLException.class, Keyboard::create);
        assertTrue(failure.getMessage().contains("Display.create()"), failure.getMessage());
        assertThrows(LWJGLException.class, Mouse::create);
        assertFalse(Keyboard.isCreated());
        assertFalse(Mouse.isCreated());

        IllegalStateException direct =
                assertThrows(IllegalStateException.class, () -> Keyboard.isKeyDown(Keyboard.KEY_A));
        assertTrue(direct.getMessage().contains("Display.create()"), direct.getMessage());
    }
}
