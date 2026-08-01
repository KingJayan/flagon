package dev.flagon.lwjgl;

import dev.flagon.platform.Platforms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTest {

    private FakeDisplayPlatform platform;

    @BeforeEach
    void setUp() {
        Platforms.reset();
        platform = new FakeDisplayPlatform();
        Platforms.install(platform);
    }

    @AfterEach
    void tearDown() {
        Display.destroy();
        Display.sync(0);
        Platforms.reset();
    }

    /** a 144 Hz host asked for 60 spends most of its frames waiting rather than drawing */
    @Test
    void holdsFramesUntilTheAskedForRateHasPassed() throws LWJGLException {
        Display.create();

        for (int frame = 0; frame < 10; frame++) {
            Display.update();
            Display.sync(60);
        }

        long wanted = 10 * 1_000_000_000L / 60;
        assertTrue(Math.abs(elapsed() - wanted) <= 2 * platform.frameNanos,
                "ten frames at 60 took " + elapsed() + "ns");
        assertTrue(platform.presented() >= 20, "presented " + platform.presented());
    }

    @Test
    void leavesALoopSlowerThanTheRateAlone() throws LWJGLException {
        platform.frameNanos = 1_000_000_000L / 20;
        Display.create();

        for (int frame = 0; frame < 10; frame++) {
            Display.update();
            Display.sync(60);
        }

        assertEquals(10, platform.presented());
    }

    @Test
    void doesNotMakeUpForTimeItFellBehind() throws LWJGLException {
        Display.create();
        Display.update();
        Display.sync(60);

        // one long frame, as a garbage collection or a tab in the background would give
        platform.frameNanos = 1_000_000_000L;
        Display.update();
        platform.frameNanos = 1_000_000_000L / 144;
        Display.sync(60);

        int after = platform.presented();
        Display.update();
        Display.sync(60);

        assertEquals(after + 1, platform.presented());
    }

    @Test
    void paysNoAttentionToTheRateWhereThereIsNone() throws LWJGLException {
        Display.create();

        Display.update();
        Display.sync(0);

        assertEquals(1, platform.presented());
        assertEquals(0, Display.getSyncRate());
    }

    @Test
    void reportsTheRateItWasAskedFor() throws LWJGLException {
        Display.create();

        Display.sync(75);

        assertEquals(75, Display.getSyncRate());
    }

    @Test
    void opensAnotherDisplayAfterOneIsDestroyed() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(640, 360));
        Display.create();
        Display.destroy();

        assertFalse(Display.isCreated());

        Display.create();

        assertTrue(Display.isCreated());
        assertFalse(Display.isCloseRequested());
        assertEquals(2, platform.opened());
    }

    @Test
    void followsTheSurfaceInAndOutOfFocus() throws LWJGLException {
        Display.create();

        assertTrue(Display.isActive());

        platform.focus(false);

        assertFalse(Display.isActive());
        assertTrue(Display.isVisible(), "losing focus does not hide a window");
    }

    @Test
    void followsTheSurfaceOutOfSight() throws LWJGLException {
        Display.create();

        platform.show(false);

        assertFalse(Display.isVisible());
    }

    /** an app polls these before it has a display and after it has given one up */
    @Test
    void isNeitherActiveNorVisibleWithoutADisplay() throws LWJGLException {
        assertFalse(Display.isActive());
        assertFalse(Display.isVisible());

        Display.create();
        Display.destroy();

        assertFalse(Display.isActive());
        assertFalse(Display.isVisible());
    }

    private long elapsed() {
        return Platforms.get().clock().nanoTime();
    }
}
