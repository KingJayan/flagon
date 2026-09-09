package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.*;

import dev.flagon.platform.Platforms;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlfwTest {

    private FakeDisplayPlatform platform;
    private long window;

    @BeforeEach
    void setUp() {
        Platforms.reset();
        platform = new FakeDisplayPlatform();
        Platforms.install(platform);
        glfwInit();
        window = glfwCreateWindow(854, 480, "flagon", NULL, NULL);
    }

    @AfterEach
    void tearDown() {
        glfwTerminate();
        Platforms.reset();
    }

    @Test
    void opensAWindowOverTheSurface() {
        assertNotEquals(NULL, window);
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);

        assertEquals(640, width[0]);
        assertEquals(360, height[0]);
    }

    @Test
    void refusesASecondWindow() {
        assertEquals(NULL, glfwCreateWindow(320, 240, "second", NULL, NULL));
    }

    @Test
    void presentsAFrameWhenBuffersAreSwapped() {
        glfwSwapBuffers(window);

        assertEquals(1, platform.presented());
    }

    @Test
    void closesWhenAskedTo() {
        assertFalse(glfwWindowShouldClose(window));

        glfwSetWindowShouldClose(window, true);

        assertTrue(glfwWindowShouldClose(window));
    }

    @Test
    void reportsKeysInGlfwNumbering() {
        List<Integer> seen = new ArrayList<>();
        glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                seen.add(key);
            }
        });
        platform.input().pressKey(dev.flagon.platform.input.Scancode.W, 'w');

        glfwPollEvents();

        assertEquals(List.of(GLFW_KEY_W), seen);
        assertEquals(GLFW_PRESS, glfwGetKey(window, GLFW_KEY_W));
    }

    @Test
    void reportsTheTextAKeyProduced() {
        StringBuilder typed = new StringBuilder();
        glfwSetCharCallback(window, (handle, codepoint) -> typed.append((char) codepoint));
        platform.input().pressKey(dev.flagon.platform.input.Scancode.W, 'w');

        glfwPollEvents();

        assertEquals("w", typed.toString());
    }

    @Test
    void carriesTheModifiersHeldAtTheTime() {
        List<Integer> mods = new ArrayList<>();
        platform.input().pressKey(dev.flagon.platform.input.Scancode.LEFT_SHIFT, '\0');
        glfwPollEvents();
        glfwSetKeyCallback(window, (handle, key, scancode, action, held) -> mods.add(held));
        platform.input().pressKey(dev.flagon.platform.input.Scancode.A, 'A');

        glfwPollEvents();

        assertEquals(List.of(GLFW_MOD_SHIFT), mods);
    }

    @Test
    void reportsMouseButtonsAndTheWheel() {
        List<Integer> buttons = new ArrayList<>();
        List<Double> scrolled = new ArrayList<>();
        glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
            if (action == GLFW_PRESS) {
                buttons.add(button);
            }
        });
        glfwSetScrollCallback(window, (handle, x, y) -> scrolled.add(y));
        platform.input().clickButton(GLFW_MOUSE_BUTTON_RIGHT);
        platform.input().turnWheel(120);

        glfwPollEvents();

        assertEquals(List.of(GLFW_MOUSE_BUTTON_RIGHT), buttons);
        assertEquals(List.of(1.0), scrolled);
    }

    @Test
    void countsThePointerDownFromTheTop() {
        platform.input().moveTo(10, 359);
        glfwPollEvents();
        double[] x = new double[1];
        double[] y = new double[1];

        glfwGetCursorPos(window, x, y);

        assertEquals(10.0, x[0]);
        assertEquals(0.0, y[0]);
    }

    @Test
    void grabsThePointerWhenTheCursorIsDisabled() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        assertEquals(GLFW_CURSOR_DISABLED, glfwGetInputMode(window, GLFW_CURSOR));
        assertTrue(platform.input().isGrabbed());
    }

    @Test
    void tellsTheCallerWhenAGrabWasRefused() {
        platform.input().grantGrab = false;

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        assertEquals(GLFW_CURSOR_NORMAL, glfwGetInputMode(window, GLFW_CURSOR));
    }

    @Test
    void reportsAMonitorTheSizeOfTheSurface() {
        assertEquals(640, glfwGetVideoMode(glfwGetPrimaryMonitor()).width());
    }

    @Test
    void movesTimeForwardFromInit() {
        assertTrue(glfwGetTime() >= 0.0);

        glfwSetTime(5.0);

        assertEquals(5.0, glfwGetTime(), 0.5);
    }

    @Test
    void destroysTheSurfaceWithTheWindow() {
        glfwDestroyWindow(window);

        assertTrue(platform.destroyed());
    }
}
